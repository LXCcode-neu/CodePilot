package com.codepliot.agent.executor;

import com.codepliot.agent.context.AgentContext;
import com.codepliot.agent.tool.AgentTool;
import com.codepliot.agent.tool.ToolResult;
import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.lock.service.TaskRunLockService;
import com.codepliot.project.entity.ProjectRepo;
import com.codepliot.sse.dto.TaskEventMessage;
import com.codepliot.sse.service.SseService;
import com.codepliot.task.entity.AgentTask;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.task.service.AgentTaskService;
import com.codepliot.trace.entity.AgentStepType;
import com.codepliot.trace.service.AgentStepService;
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
 * Coordinates Agent tool execution, step persistence, and SSE progress events.
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

    public AgentExecutor(AgentTaskService agentTaskService,
                         AgentStepService agentStepService,
                         ObjectMapper objectMapper,
                         List<AgentTool> agentTools,
                         TaskRunLockService taskRunLockService,
                         SseService sseService) {
        this.agentTaskService = agentTaskService;
        this.agentStepService = agentStepService;
        this.objectMapper = objectMapper;
        this.agentTools = agentTools;
        this.taskRunLockService = taskRunLockService;
        this.sseService = sseService;
    }

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

    public void execute(AgentTask task, ProjectRepo projectRepo) {
        AgentContext context = buildContext(task, projectRepo);
        try {
            for (AgentTool agentTool : agentTools) {
                runTool(context, agentTool);
            }
            agentTaskService.updateStatus(context.taskId(), AgentTaskStatus.COMPLETED,
                    "Mock agent run completed", null);
            runCompletionStep(context);
            pushTaskEvent(context.taskId(), AgentTaskStatus.COMPLETED.name(), "COMPLETED", null, "任务执行完成。");
            sseService.complete(context.taskId());
        } catch (RuntimeException exception) {
            agentTaskService.updateStatus(context.taskId(), AgentTaskStatus.FAILED, null, errorMessage(exception));
            pushTaskEvent(context.taskId(), AgentTaskStatus.FAILED.name(), "COMPLETED", null, errorMessage(exception));
            sseService.complete(context.taskId());
            throw exception;
        }
    }

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

    private void runCompletionStep(AgentContext context) {
        Long stepId = agentStepService.startStep(
                context.taskId(),
                AgentStepType.COMPLETE_RUN,
                "Agent 流程完成",
                toJson(stepInput(context, AgentStepType.COMPLETE_RUN))
        );
        pushTaskEvent(context.taskId(), AgentTaskStatus.COMPLETED.name(), "COMPLETED",
                AgentStepType.COMPLETE_RUN.name(), "任务收尾开始");
        agentStepService.successStep(stepId, toJson(stepOutput(
                context,
                AgentStepType.COMPLETE_RUN,
                ToolResult.success("mock agent run completed", Map.of("taskId", context.taskId()))
        )));
        pushTaskEvent(context.taskId(), AgentTaskStatus.COMPLETED.name(), "COMPLETED",
                AgentStepType.COMPLETE_RUN.name(), "任务收尾完成");
    }

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

    private Map<String, Object> stepOutput(AgentContext context, AgentStepType stepType, ToolResult result) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("taskId", context.taskId());
        output.put("stepType", stepType.name());
        output.put("success", result.success());
        output.put("message", result.message());
        output.put("data", result.data());
        return output;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to serialize agent step data");
        }
    }

    private String errorMessage(Throwable throwable) {
        if (throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getMessage();
    }

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
