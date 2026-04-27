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
import com.codepliot.task.entity.AgentTask;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.task.mapper.AgentTaskMapper;
import com.codepliot.task.vo.AgentTaskVO;
import org.springframework.stereotype.Service;

/**
 * Agent 运行入口服务，负责运行前校验并异步提交执行器。
 */
@Service
public class AgentRunService {

    private final AgentTaskMapper agentTaskMapper;
    private final ProjectRepoMapper projectRepoMapper;
    private final AgentExecutor agentExecutor;
    private final TaskRunLockService taskRunLockService;

    public AgentRunService(AgentTaskMapper agentTaskMapper,
                           ProjectRepoMapper projectRepoMapper,
                           AgentExecutor agentExecutor,
                           TaskRunLockService taskRunLockService) {
        this.agentTaskMapper = agentTaskMapper;
        this.projectRepoMapper = projectRepoMapper;
        this.agentExecutor = agentExecutor;
        this.taskRunLockService = taskRunLockService;
    }

    /**
     * 校验当前用户是否可以运行该任务，然后异步提交 Agent 执行。
     */
    public AgentTaskVO run(Long taskId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AgentTask task = requireOwnedTask(taskId, currentUserId);
        String lockValue = taskRunLockService.lock(taskId);
        try {
            requireRunnableStatus(task);
            ProjectRepo projectRepo = requireOwnedProject(task.getProjectId(), currentUserId);
            claimRunnableTask(taskId, currentUserId);

            task.setStatus(AgentTaskStatus.CLONING.name());
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

    /**
     * 用条件更新抢占任务运行权，降低重复点击或并发请求导致重复执行的概率。
     */
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
}
