package com.codepliot.agent.executor;

import com.codepliot.agent.context.AgentContext;
import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.project.entity.ProjectRepo;
import com.codepliot.task.entity.AgentTask;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.task.service.AgentTaskService;
import com.codepliot.trace.entity.AgentStepType;
import com.codepliot.trace.service.AgentStepService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Agent 执行器，目前只跑固定顺序的 Mock 流程。
 */
@Component
public class AgentExecutor {

    private final AgentTaskService agentTaskService;
    private final AgentStepService agentStepService;
    private final ObjectMapper objectMapper;

    public AgentExecutor(AgentTaskService agentTaskService,
                         AgentStepService agentStepService,
                         ObjectMapper objectMapper) {
        this.agentTaskService = agentTaskService;
        this.agentStepService = agentStepService;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步执行 Mock Agent 流程，并把每一步写入执行轨迹。
     */
    public void execute(AgentTask task, ProjectRepo projectRepo) {
        AgentContext context = buildContext(task, projectRepo);
        try {
            runStep(context, AgentTaskStatus.CLONING, AgentStepType.CLONE_REPOSITORY,
                    "拉取仓库", "mock clone repository completed");
            runStep(context, AgentTaskStatus.INDEXING, AgentStepType.BUILD_CODE_INDEX,
                    "构建代码索引", "mock code index completed");
            runStep(context, AgentTaskStatus.RETRIEVING, AgentStepType.SEARCH_RELEVANT_CODE,
                    "检索相关代码", "mock relevant code search completed");
            runStep(context, AgentTaskStatus.ANALYZING, AgentStepType.ANALYZE_ISSUE,
                    "分析 Issue", "mock issue analysis completed");
            runStep(context, AgentTaskStatus.GENERATING_PATCH, AgentStepType.GENERATE_PATCH,
                    "生成 Patch", "mock patch generation completed");
            agentTaskService.updateStatus(context.taskId(), AgentTaskStatus.COMPLETED,
                    "Mock agent run completed", null);
            runCompletionStep(context);
        } catch (RuntimeException ex) {
            agentTaskService.updateStatus(context.taskId(), AgentTaskStatus.FAILED, null, errorMessage(ex));
            throw ex;
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

    private void runStep(AgentContext context,
                         AgentTaskStatus taskStatus,
                         AgentStepType stepType,
                         String stepName,
                         String mockOutput) {
        agentTaskService.updateStatus(context.taskId(), taskStatus);
        Long stepId = agentStepService.startStep(context.taskId(), stepType, stepName, toJson(stepInput(context, stepType)));
        try {
            agentStepService.successStep(stepId, toJson(stepOutput(context, stepType, mockOutput)));
        } catch (RuntimeException ex) {
            agentStepService.failStep(stepId, errorMessage(ex));
            throw ex;
        }
    }

    private void runCompletionStep(AgentContext context) {
        Long stepId = agentStepService.startStep(
                context.taskId(),
                AgentStepType.COMPLETE_RUN,
                "Mock 流程完成",
                toJson(stepInput(context, AgentStepType.COMPLETE_RUN))
        );
        agentStepService.successStep(stepId, toJson(stepOutput(
                context,
                AgentStepType.COMPLETE_RUN,
                "mock agent run completed"
        )));
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

    private Map<String, Object> stepOutput(AgentContext context, AgentStepType stepType, String mockOutput) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("taskId", context.taskId());
        output.put("stepType", stepType.name());
        output.put("message", mockOutput);
        return output;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to serialize agent step data");
        }
    }

    private String errorMessage(RuntimeException ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getMessage();
    }
}
