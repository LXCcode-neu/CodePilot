package com.codepliot.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.exception.AgentTaskCancelledException;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.TaskEventMessage;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.service.sse.SseService;
import com.codepliot.utils.SecurityUtils;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 任务取消服务。
 * <p>
 * 处理 Agent 任务的取消请求，包括：验证任务是否处于可取消状态、
 * 将任务状态更新为"取消已请求"、中断正在执行的线程、
 * 提供取消检查点供执行流程调用、以及最终标记任务为已取消。
 * </p>
 */
@Service
public class AgentTaskCancellationService {

    private static final Set<String> CANCELLABLE_STATUSES = Set.of(
            AgentTaskStatus.CLONING.name(),
            AgentTaskStatus.RETRIEVING.name(),
            AgentTaskStatus.ANALYZING.name(),
            AgentTaskStatus.GENERATING_PATCH.name(),
            AgentTaskStatus.VERIFYING.name(),
            AgentTaskStatus.REPAIRING_PATCH.name()
    );

    private final AgentTaskMapper agentTaskMapper;
    private final SseService sseService;
    private final AgentTaskCancellationRegistry cancellationRegistry;

    public AgentTaskCancellationService(AgentTaskMapper agentTaskMapper,
                                        SseService sseService,
                                        AgentTaskCancellationRegistry cancellationRegistry) {
        this.agentTaskMapper = agentTaskMapper;
        this.sseService = sseService;
        this.cancellationRegistry = cancellationRegistry;
    }

    /**
     * 请求取消指定的 Agent 任务。
     * <p>
     * 如果任务已经是"取消已请求"状态，则直接中断执行线程；
     * 否则将任务状态更新为"取消已请求"并中断线程。
     * 仅允许处于可取消状态的任务被取消。
     * </p>
     *
     * @param taskId 任务 ID
     * @return 更新后的任务视图对象
     */
    @Transactional
    public AgentTaskVO requestCancel(Long taskId) {
        Long userId = SecurityUtils.getCurrentUserId();
        AgentTask task = requireOwnedTask(taskId, userId);
        if (AgentTaskStatus.CANCEL_REQUESTED.name().equals(task.getStatus())) {
            cancellationRegistry.interrupt(taskId);
            return AgentTaskVO.from(task);
        }
        int updated = agentTaskMapper.update(null, new LambdaUpdateWrapper<AgentTask>()
                .eq(AgentTask::getId, taskId)
                .eq(AgentTask::getUserId, userId)
                .in(AgentTask::getStatus, CANCELLABLE_STATUSES)
                .set(AgentTask::getStatus, AgentTaskStatus.CANCEL_REQUESTED.name())
                .set(AgentTask::getResultSummary, "Task cancellation requested")
                .set(AgentTask::getErrorMessage, null));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Agent task cannot be cancelled in current status");
        }
        pushEvent(taskId, AgentTaskStatus.CANCEL_REQUESTED, "RUNNING", "Task cancellation requested");
        cancellationRegistry.interrupt(taskId);
        return AgentTaskVO.from(agentTaskMapper.selectById(taskId));
    }

    /**
     * 检查任务是否已被请求取消，如果是则抛出 {@link AgentTaskCancelledException}。
     * <p>
     * 此方法通常在 Agent 执行流程的关键节点调用，作为取消检查点。
     * </p>
     *
     * @param taskId 任务 ID
     * @throws AgentTaskCancelledException 如果任务已被请求取消
     */
    public void throwIfCancelRequested(Long taskId) {
        AgentTask task = agentTaskMapper.selectById(taskId);
        if (task != null && AgentTaskStatus.CANCEL_REQUESTED.name().equals(task.getStatus())) {
            throw new AgentTaskCancelledException("Agent task was cancelled by user");
        }
    }

    /**
     * 将任务标记为已取消状态。
     * <p>
     * 更新任务状态为 CANCELLED，设置结果摘要，并通过 SSE 推送取消完成事件。
     * </p>
     *
     * @param taskId  任务 ID
     * @param message 取消原因消息
     */
    @Transactional
    public void markCancelled(Long taskId, String message) {
        AgentTask task = agentTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(AgentTaskStatus.CANCELLED.name());
        task.setResultSummary(message == null || message.isBlank() ? "Task cancelled" : message);
        task.setErrorMessage(null);
        agentTaskMapper.updateById(task);
        pushEvent(taskId, AgentTaskStatus.CANCELLED, "COMPLETED", task.getResultSummary());
        sseService.complete(taskId);
    }

    private AgentTask requireOwnedTask(Long taskId, Long userId) {
        AgentTask task = agentTaskMapper.selectOne(new LambdaQueryWrapper<AgentTask>()
                .eq(AgentTask::getId, taskId)
                .eq(AgentTask::getUserId, userId)
                .last("limit 1"));
        if (task == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return task;
    }

    private void pushEvent(Long taskId, AgentTaskStatus status, String phase, String message) {
        sseService.push(new TaskEventMessage(
                taskId,
                status.name(),
                phase,
                null,
                message,
                LocalDateTime.now()
        ));
    }
}
