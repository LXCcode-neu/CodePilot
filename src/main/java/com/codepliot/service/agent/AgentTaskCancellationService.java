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

    public void throwIfCancelRequested(Long taskId) {
        AgentTask task = agentTaskMapper.selectById(taskId);
        if (task != null && AgentTaskStatus.CANCEL_REQUESTED.name().equals(task.getStatus())) {
            throw new AgentTaskCancelledException("Agent task was cancelled by user");
        }
    }

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
