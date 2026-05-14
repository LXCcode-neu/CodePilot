package com.codepliot.service.agent;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.ProjectLlmConfig;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.AgentTaskCancelledException;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.AgentContext;
import com.codepliot.model.AgentExecutionDecision;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.PatchGeneratedEvent;
import com.codepliot.model.TaskEventMessage;
import com.codepliot.policy.AgentExecutionPolicy;
import com.codepliot.service.ProjectLlmConfigService;
import com.codepliot.service.sse.SseService;
import com.codepliot.service.task.AgentStepService;
import com.codepliot.service.task.AgentTaskService;
import com.codepliot.service.task.TaskRunLockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
    private final ProjectLlmConfigService projectLlmConfigService;
    private final ApplicationEventPublisher eventPublisher;
    private final AgentTaskCancellationService agentTaskCancellationService;
    private final AgentTaskCancellationRegistry cancellationRegistry;

    public AgentExecutor(AgentTaskService agentTaskService,
                         AgentStepService agentStepService,
                         ObjectMapper objectMapper,
                         List<AgentTool> agentTools,
                         TaskRunLockService taskRunLockService,
                         SseService sseService,
                         AgentExecutionPolicy agentExecutionPolicy,
                         ProjectLlmConfigService projectLlmConfigService,
                         ApplicationEventPublisher eventPublisher,
                         AgentTaskCancellationService agentTaskCancellationService,
                         AgentTaskCancellationRegistry cancellationRegistry) {
        this.agentTaskService = agentTaskService;
        this.agentStepService = agentStepService;
        this.objectMapper = objectMapper;
        this.agentTools = agentTools;
        this.taskRunLockService = taskRunLockService;
        this.sseService = sseService;
        this.agentExecutionPolicy = agentExecutionPolicy;
        this.projectLlmConfigService = projectLlmConfigService;
        this.eventPublisher = eventPublisher;
        this.agentTaskCancellationService = agentTaskCancellationService;
        this.cancellationRegistry = cancellationRegistry;
    }

    @Async("agentTaskExecutor")
    public void executeAsync(AgentTask task, ProjectRepo projectRepo, String lockValue) {
        cancellationRegistry.register(task.getId());
        try {
            pushTaskEvent(task.getId(), task.getStatus(), "RUNNING", null, "Agent task entered background execution");
            execute(task, projectRepo);
        } catch (RuntimeException exception) {
            log.error("Agent task async execution failed, taskId={}", task.getId(), exception);
            completeWithFailure(task.getId(), exception);
        } catch (Throwable throwable) {
            log.error("Agent task async execution failed with unexpected throwable, taskId={}", task.getId(), throwable);
            completeWithFailure(task.getId(), throwable);
        } finally {
            cancellationRegistry.unregister(task.getId());
            Thread.interrupted();
            taskRunLockService.unlock(task.getId(), lockValue);
        }
    }

    public void execute(AgentTask task, ProjectRepo projectRepo) {
        AgentContext context = buildContext(task, projectRepo);
        try {
            for (AgentTool agentTool : agentTools) {
                agentTaskCancellationService.throwIfCancelRequested(context.taskId());
                runTool(context, agentTool);
                agentTaskCancellationService.throwIfCancelRequested(context.taskId());
            }

            AgentExecutionDecision decision = agentExecutionPolicy.afterPatchGenerated(
                    context.patchSafetyCheckResult(),
                    context.patchVerificationResult()
            );
            agentTaskService.updateStatus(context.taskId(), decision.status(), decision.resultSummary(), null);
            publishPatchOutcome(context, decision);
            pushTaskEvent(context.taskId(), decision.status().name(), "COMPLETED", null, decision.eventMessage());
            sseService.complete(context.taskId());
        } catch (AgentTaskCancelledException exception) {
            completeWithCancelled(context.taskId(), exception.getMessage());
        } catch (RuntimeException exception) {
            completeWithFailure(context.taskId(), exception);
        }
    }

    private AgentContext buildContext(AgentTask task, ProjectRepo projectRepo) {
        ProjectLlmConfig llmConfig = projectLlmConfigService.requireProjectConfig(task.getProjectId(), task.getUserId());
        return new AgentContext(
                task.getId(),
                task.getUserId(),
                task.getProjectId(),
                projectRepo.getRepoUrl(),
                projectRepo.getRepoName(),
                projectRepo.getLocalPath(),
                projectLlmConfigService.toRuntimeConfig(llmConfig),
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
                agentTool.stepName() + " started"
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
                    agentTool.stepName() + " succeeded"
            );
        } catch (AgentTaskCancelledException exception) {
            failStepIfPossible(stepId, exception);
            pushTaskEvent(
                    context.taskId(),
                    AgentTaskStatus.CANCEL_REQUESTED.name(),
                    "RUNNING",
                    agentTool.stepType().name(),
                    agentTool.stepName() + " cancelled"
            );
            throw exception;
        } catch (RuntimeException exception) {
            try {
                agentTaskCancellationService.throwIfCancelRequested(context.taskId());
            } catch (AgentTaskCancelledException cancellationException) {
                failStepIfPossible(stepId, cancellationException);
                pushTaskEvent(
                        context.taskId(),
                        AgentTaskStatus.CANCEL_REQUESTED.name(),
                        "RUNNING",
                        agentTool.stepType().name(),
                        agentTool.stepName() + " cancelled"
                );
                throw cancellationException;
            }
            failStepIfPossible(stepId, exception);
            pushTaskEvent(
                    context.taskId(),
                    AgentTaskStatus.FAILED.name(),
                    "RUNNING",
                    agentTool.stepType().name(),
                    agentTool.stepName() + " failed: " + errorMessage(exception)
            );
            throw exception;
        }
    }

    private void publishPatchOutcome(AgentContext context, AgentExecutionDecision decision) {
        Long patchRecordId = context.patchRecordId();
        if (patchRecordId == null) {
            return;
        }
        if (AgentTaskStatus.WAITING_CONFIRM.equals(decision.status())) {
            eventPublisher.publishEvent(new PatchGeneratedEvent(context.taskId(), patchRecordId, true, null));
            return;
        }
        if (AgentTaskStatus.VERIFY_FAILED.equals(decision.status())) {
            eventPublisher.publishEvent(new PatchGeneratedEvent(
                    context.taskId(),
                    patchRecordId,
                    false,
                    decision.resultSummary()
            ));
        }
    }

    private void failStepIfPossible(Long stepId, RuntimeException originalException) {
        try {
            agentStepService.failStep(stepId, errorMessage(originalException));
        } catch (RuntimeException failStepException) {
            log.warn(
                    "Failed to mark agent step as failed, stepId={}, originalError={}, failStepError={}",
                    stepId,
                    errorMessage(originalException),
                    errorMessage(failStepException)
            );
        }
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

    private void completeWithFailure(Long taskId, Throwable throwable) {
        String message = errorMessage(throwable);
        agentTaskService.updateStatus(taskId, AgentTaskStatus.FAILED, null, message);
        pushTaskEvent(taskId, AgentTaskStatus.FAILED.name(), "COMPLETED", null, message);
        sseService.complete(taskId);
    }

    private void completeWithCancelled(Long taskId, String message) {
        agentTaskCancellationService.markCancelled(taskId, message);
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
