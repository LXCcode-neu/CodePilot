package com.codepliot.agent.executor;

import com.codepliot.agent.context.AgentContext;
import com.codepliot.agent.tool.AgentTool;
import com.codepliot.agent.tool.ToolResult;
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
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Agent 执行器，负责编排工具、更新任务状态并记录执行轨迹。
 */
@Component
public class AgentExecutor {

    private final AgentTaskService agentTaskService;
    private final AgentStepService agentStepService;
    private final ObjectMapper objectMapper;
    private final List<AgentTool> agentTools;

    public AgentExecutor(AgentTaskService agentTaskService,
                         AgentStepService agentStepService,
                         ObjectMapper objectMapper,
                         List<AgentTool> agentTools) {
        this.agentTaskService = agentTaskService;
        this.agentStepService = agentStepService;
        this.objectMapper = objectMapper;
        this.agentTools = agentTools;
    }

    /**
     * 同步执行 Mock Agent 流程，并把每一步写入执行轨迹。
     */
    public void execute(AgentTask task, ProjectRepo projectRepo) {
        AgentContext context = buildContext(task, projectRepo);
        try {
            for (AgentTool agentTool : agentTools) {
                runTool(context, agentTool);
            }
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

    private void runTool(AgentContext context, AgentTool agentTool) {
        agentTaskService.updateStatus(context.taskId(), agentTool.taskStatus());
        Long stepId = agentStepService.startStep(
                context.taskId(),
                agentTool.stepType(),
                agentTool.stepName(),
                toJson(stepInput(context, agentTool.stepType()))
        );
        try {
            ToolResult result = agentTool.execute(context);
            if (!result.success()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, result.message());
            }
            agentStepService.successStep(stepId, toJson(stepOutput(context, agentTool.stepType(), result)));
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
                ToolResult.success("mock agent run completed", Map.of("taskId", context.taskId()))
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
