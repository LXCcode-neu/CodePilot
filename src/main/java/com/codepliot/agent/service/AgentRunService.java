package com.codepliot.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codepliot.agent.executor.AgentExecutor;
import com.codepliot.auth.security.SecurityUtils;
import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.lock.service.TaskRunLockService;
import com.codepliot.project.entity.ProjectRepo;
import com.codepliot.project.mapper.ProjectRepoMapper;
import com.codepliot.sse.dto.TaskEventMessage;
import com.codepliot.sse.service.SseService;
import com.codepliot.task.entity.AgentTask;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.task.mapper.AgentTaskMapper;
import com.codepliot.task.vo.AgentTaskVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Agent run entry service. It validates ownership, acquires the run lock,
 * emits the initial SSE state, and submits async execution.
 */
@Service
public class AgentRunService {

    private final AgentTaskMapper agentTaskMapper;
    private final ProjectRepoMapper projectRepoMapper;
    private final AgentExecutor agentExecutor;
    private final TaskRunLockService taskRunLockService;
    private final SseService sseService;

    public AgentRunService(AgentTaskMapper agentTaskMapper,
                           ProjectRepoMapper projectRepoMapper,
                           AgentExecutor agentExecutor,
                           TaskRunLockService taskRunLockService,
                           SseService sseService) {
        this.agentTaskMapper = agentTaskMapper;
        this.projectRepoMapper = projectRepoMapper;
        this.agentExecutor = agentExecutor;
        this.taskRunLockService = taskRunLockService;
        this.sseService = sseService;
    }

    public AgentTaskVO run(Long taskId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AgentTask task = requireOwnedTask(taskId, currentUserId);
        String lockValue = taskRunLockService.lock(taskId);
        try {
            requireRunnableStatus(task);
            ProjectRepo projectRepo = requireOwnedProject(task.getProjectId(), currentUserId);
            claimRunnableTask(taskId, currentUserId);

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
                && !AgentTaskStatus.FAILED.name().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Agent task cannot be run in current status");
        }
    }

    private void claimRunnableTask(Long taskId, Long currentUserId) {
        int updated = agentTaskMapper.update(null, new LambdaUpdateWrapper<AgentTask>()
                .eq(AgentTask::getId, taskId)
                .eq(AgentTask::getUserId, currentUserId)
                .in(AgentTask::getStatus, AgentTaskStatus.PENDING.name(), AgentTaskStatus.FAILED.name())
                .set(AgentTask::getStatus, AgentTaskStatus.CLONING.name()));
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
                "任务已开始，正在准备执行。",
                LocalDateTime.now()
        ));
    }
}
