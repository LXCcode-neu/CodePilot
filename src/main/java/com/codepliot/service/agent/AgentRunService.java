package com.codepliot.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.TaskEventMessage;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.service.sse.SseService;
import com.codepliot.service.task.TaskRunLockService;
import com.codepliot.utils.SecurityUtils;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AgentRunService {

    private final AgentTaskMapper agentTaskMapper;
    private final ProjectRepoMapper projectRepoMapper;
    private final AgentExecutor agentExecutor;
    private final AgentRunCleanupService agentRunCleanupService;
    private final TaskRunLockService taskRunLockService;
    private final SseService sseService;

    public AgentRunService(AgentTaskMapper agentTaskMapper,
                           ProjectRepoMapper projectRepoMapper,
                           AgentExecutor agentExecutor,
                           AgentRunCleanupService agentRunCleanupService,
                           TaskRunLockService taskRunLockService,
                           SseService sseService) {
        this.agentTaskMapper = agentTaskMapper;
        this.projectRepoMapper = projectRepoMapper;
        this.agentExecutor = agentExecutor;
        this.agentRunCleanupService = agentRunCleanupService;
        this.taskRunLockService = taskRunLockService;
        this.sseService = sseService;
    }

    public AgentTaskVO run(Long taskId) {
        return run(taskId, SecurityUtils.getCurrentUserId());
    }

    public AgentTaskVO run(Long taskId, Long userId) {
        AgentTask task = requireOwnedTask(taskId, userId);
        String lockValue = taskRunLockService.lock(taskId);
        try {
            requireRunnableStatus(task);
            ProjectRepo projectRepo = requireOwnedProject(task.getProjectId(), userId);
            agentRunCleanupService.cleanupBeforeRun(taskId);
            claimRunnableTask(taskId, userId);

            task.setStatus(AgentTaskStatus.CLONING.name());
            pushStartedEvent(taskId);
            agentExecutor.executeAsync(task, projectRepo, lockValue);
            return AgentTaskVO.from(agentTaskMapper.selectById(taskId));
        } catch (RuntimeException exception) {
            taskRunLockService.unlock(taskId, lockValue);
            throw exception;
        }
    }

    private AgentTask requireOwnedTask(Long taskId, Long currentUserId) {
        AgentTask task = agentTaskMapper.selectOne(new LambdaQueryWrapper<AgentTask>()
                .eq(AgentTask::getId, taskId)
                .eq(AgentTask::getUserId, currentUserId)
                .last("limit 1"));
        if (task == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return task;
    }

    private void requireRunnableStatus(AgentTask task) {
        if (!AgentTaskStatus.PENDING.name().equals(task.getStatus())
                && !AgentTaskStatus.FAILED.name().equals(task.getStatus())
                && !AgentTaskStatus.VERIFY_FAILED.name().equals(task.getStatus())
                && !AgentTaskStatus.CANCELLED.name().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Agent task cannot be run in current status");
        }
    }

    private void claimRunnableTask(Long taskId, Long currentUserId) {
        int updated = agentTaskMapper.update(null, new LambdaUpdateWrapper<AgentTask>()
                .eq(AgentTask::getId, taskId)
                .eq(AgentTask::getUserId, currentUserId)
                .in(AgentTask::getStatus,
                        AgentTaskStatus.PENDING.name(),
                        AgentTaskStatus.FAILED.name(),
                        AgentTaskStatus.VERIFY_FAILED.name(),
                        AgentTaskStatus.CANCELLED.name())
                .set(AgentTask::getStatus, AgentTaskStatus.CLONING.name())
                .set(AgentTask::getResultSummary, null)
                .set(AgentTask::getErrorMessage, null));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Agent task cannot be run in current status");
        }
    }

    private ProjectRepo requireOwnedProject(Long projectId, Long currentUserId) {
        ProjectRepo projectRepo = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getId, projectId)
                .eq(ProjectRepo::getUserId, currentUserId)
                .last("limit 1"));
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }

    private void pushStartedEvent(Long taskId) {
        sseService.push(new TaskEventMessage(
                taskId,
                AgentTaskStatus.CLONING.name(),
                "STARTED",
                null,
                "Agent task started",
                LocalDateTime.now()
        ));
    }
}
