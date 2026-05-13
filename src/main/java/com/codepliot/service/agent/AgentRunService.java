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

/**
 * 任务运行服务。
 *
 * <p>负责校验任务归属、抢占运行资格、申请 Redis 运行锁，并把任务提交给异步执行器。
 */
@Service
public class AgentRunService {

    private final AgentTaskMapper agentTaskMapper;
    private final ProjectRepoMapper projectRepoMapper;
    private final AgentExecutor agentExecutor;
    private final TaskRunLockService taskRunLockService;
    private final SseService sseService;

    /**
     * 创建任务运行服务。
     */
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

    /**
     * 触发指定任务运行。
     *
     * <p>接口线程只负责完成运行前校验和异步提交，不在这里同步执行完整 Agent 流程。
     */
    public AgentTaskVO run(Long taskId) {
        return run(taskId, SecurityUtils.getCurrentUserId());
    }

    public AgentTaskVO run(Long taskId, Long userId) {
        AgentTask task = requireOwnedTask(taskId, userId);

        // 先加 Redis 锁，避免同一个任务被重复点击运行。
        String lockValue = taskRunLockService.lock(taskId);
        try {
            requireRunnableStatus(task);
            ProjectRepo projectRepo = requireOwnedProject(task.getProjectId(), userId);

            // 通过数据库状态抢占运行资格，避免并发线程同时进入执行阶段。
            claimRunnableTask(taskId, userId);

            // 先给前端一个“已开始”的明确反馈，再把任务提交到异步线程池。
            task.setStatus(AgentTaskStatus.CLONING.name());
            pushStartedEvent(taskId);
            agentExecutor.executeAsync(task, projectRepo, lockValue);
            return AgentTaskVO.from(agentTaskMapper.selectById(taskId));
        } catch (RuntimeException exception) {
            // 如果异步提交前就失败，需要在当前线程释放锁。
            taskRunLockService.unlock(taskId, lockValue);
            throw exception;
        }
    }

    /**
     * 查询并校验任务是否属于当前用户。
     */
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

    /**
     * 校验任务当前状态是否允许再次运行。
     */
    private void requireRunnableStatus(AgentTask task) {
        if (!AgentTaskStatus.PENDING.name().equals(task.getStatus())
                && !AgentTaskStatus.FAILED.name().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Agent task cannot be run in current status");
        }
    }

    /**
     * 抢占任务运行资格。
     *
     * <p>只有处于待运行或失败状态的任务才允许被更新为执行中。
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

    /**
     * 查询并校验项目是否属于当前用户。
     */
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

    /**
     * 推送任务开始事件。
     */
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
