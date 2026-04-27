package com.codepliot.service;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.AgentContext;
import com.codepliot.model.AgentExecutionDecision;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.TaskEventMessage;
import com.codepliot.policy.AgentExecutionPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Agent 执行器。
 *
 * <p>负责按顺序执行全部 AgentTool，并在执行过程中持续更新任务状态、步骤轨迹和 SSE 事件。
 */
@Component
public class AgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutor.class);

    private final AgentTaskService agentTaskService;
    private final AgentStepService agentStepService;
    private final ObjectMapper objectMapper;
    private final List<AgentTool> agentTools;
    private final TaskRunLockService taskRunLockService;
    private final SseService sseService;
    private final AgentExecutionPolicy agentExecutionPolicy;

    /**
     * 创建 Agent 执行器。
     */
    public AgentExecutor(AgentTaskService agentTaskService,
                         AgentStepService agentStepService,
                         ObjectMapper objectMapper,
                         List<AgentTool> agentTools,
                         TaskRunLockService taskRunLockService,
                         SseService sseService,
                         AgentExecutionPolicy agentExecutionPolicy) {
        this.agentTaskService = agentTaskService;
        this.agentStepService = agentStepService;
        this.objectMapper = objectMapper;
        this.agentTools = agentTools;
        this.taskRunLockService = taskRunLockService;
        this.sseService = sseService;
        this.agentExecutionPolicy = agentExecutionPolicy;
    }

    /**
     * 在异步线程中执行任务。
     *
     * <p>这里统一兜底异常和锁释放，确保后台任务不会因为线程异常而丢失状态。
     */
    @Async("agentTaskExecutor")
    public void executeAsync(AgentTask task, ProjectRepo projectRepo, String lockValue) {
        try {
            pushTaskEvent(task.getId(), task.getStatus(), "RUNNING", null, "任务已进入后台执行。");
            execute(task, projectRepo);
        } catch (RuntimeException exception) {
            log.error("Agent task async execution failed, taskId={}", task.getId(), exception);
        } catch (Throwable throwable) {
            log.error("Agent task async execution failed with unexpected throwable, taskId={}", task.getId(), throwable);
            agentTaskService.updateStatus(task.getId(), AgentTaskStatus.FAILED, null, errorMessage(throwable));
            pushTaskEvent(task.getId(), AgentTaskStatus.FAILED.name(), "COMPLETED", null, errorMessage(throwable));
            sseService.complete(task.getId());
        } finally {
            taskRunLockService.unlock(task.getId(), lockValue);
        }
    }

    /**
     * 按顺序执行当前任务的全部工具链。
     */
    public void execute(AgentTask task, ProjectRepo projectRepo) {
        AgentContext context = buildContext(task, projectRepo);
        try {
            for (AgentTool agentTool : agentTools) {
                runTool(context, agentTool);
            }

            AgentExecutionDecision decision = agentExecutionPolicy.afterPatchGenerated(context.patchSafetyCheckResult());
            agentTaskService.updateStatus(context.taskId(), decision.status(), decision.resultSummary(), null);
            pushTaskEvent(context.taskId(), decision.status().name(), "COMPLETED", null, decision.eventMessage());
            sseService.complete(context.taskId());
        } catch (RuntimeException exception) {
            agentTaskService.updateStatus(context.taskId(), AgentTaskStatus.FAILED, null, errorMessage(exception));
            pushTaskEvent(context.taskId(), AgentTaskStatus.FAILED.name(), "COMPLETED", null, errorMessage(exception));
            sseService.complete(context.taskId());
            throw exception;
        }
    }

    /**
     * 构建本次运行使用的上下文对象。
     */
    private AgentContext buildContext(AgentTask task, ProjectRepo projectRepo) {
        return new AgentContext(
                task.getId(),
                task.getUserId(),
                task.getProjectId(),
                projectRepo.getRepoUrl(),
                projectRepo.getRepoName(),
                projectRepo.getLocalPath(),
                task.getIssueTitle(),
                task.getIssueDescription()
        );
    }

    /**
     * 执行单个工具步骤。
     *
     * <p>步骤开始、成功、失败三个节点都会同步写入步骤表，并推送对应 SSE 事件。
     */
    private void runTool(AgentContext context, AgentTool agentTool) {
        agentTaskService.updateStatus(context.taskId(), agentTool.taskStatus());
        Long stepId = agentStepService.startStep(
                context.taskId(),
                agentTool.stepType(),
                agentTool.stepName(),
                toJson(stepInput(context, agentTool.stepType()))
        );
        pushTaskEvent(
                context.taskId(),
                agentTool.taskStatus().name(),
                "RUNNING",
                agentTool.stepType().name(),
                agentTool.stepName() + " 开始执行"
        );

        try {
            ToolResult result = agentTool.execute(context);
            if (!result.success()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, result.message());
            }

            agentStepService.successStep(stepId, toJson(stepOutput(context, agentTool.stepType(), result)));
            pushTaskEvent(
                    context.taskId(),
                    agentTool.taskStatus().name(),
                    "RUNNING",
                    agentTool.stepType().name(),
                    agentTool.stepName() + " 执行成功"
            );
        } catch (RuntimeException exception) {
            agentStepService.failStep(stepId, errorMessage(exception));
            pushTaskEvent(
                    context.taskId(),
                    AgentTaskStatus.FAILED.name(),
                    "RUNNING",
                    agentTool.stepType().name(),
                    agentTool.stepName() + " 执行失败：" + errorMessage(exception)
            );
            throw exception;
        }
    }

    /**
     * 生成步骤输入快照，便于排查每一步拿到的上下文。
     */
    private Map<String, Object> stepInput(AgentContext context, AgentStepType stepType) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("taskId", context.taskId());
        input.put("userId", context.userId());
        input.put("projectId", context.projectId());
        input.put("stepType", stepType.name());
        input.put("repoUrl", context.repoUrl());
        input.put("repoName", context.repoName());
        input.put("localPath", context.localPath());
        input.put("issueTitle", context.issueTitle());
        input.put("issueDescription", context.issueDescription());
        return input;
    }

    /**
     * 生成步骤输出快照，便于在任务详情页中直接展示结果。
     */
    private Map<String, Object> stepOutput(AgentContext context, AgentStepType stepType, ToolResult result) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("taskId", context.taskId());
        output.put("stepType", stepType.name());
        output.put("success", result.success());
        output.put("message", result.message());
        output.put("data", result.data());
        return output;
    }

    /**
     * 将步骤数据序列化为 JSON 字符串。
     */
    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to serialize agent step data");
        }
    }

    /**
     * 提取适合展示给前端和日志的错误信息。
     */
    private String errorMessage(Throwable throwable) {
        if (throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getMessage();
    }

    /**
     * 推送任务级事件。
     */
    private void pushTaskEvent(Long taskId, String taskStatus, String phase, String stepType, String message) {
        sseService.push(new TaskEventMessage(
                taskId,
                taskStatus,
                phase,
                stepType,
                message,
                LocalDateTime.now()
        ));
    }
}
