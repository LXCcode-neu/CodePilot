package com.codepliot.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.auth.security.SecurityUtils;
import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.task.entity.AgentStep;
import com.codepliot.task.entity.AgentStepStatus;
import com.codepliot.task.entity.AgentStepType;
import com.codepliot.task.entity.AgentTask;
import com.codepliot.task.mapper.AgentStepMapper;
import com.codepliot.task.mapper.AgentTaskMapper;
import com.codepliot.task.vo.AgentStepVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 处理 Agent Step 记录的创建、查询和执行结果更新。
 */
@Service
public class AgentStepService {

    private final AgentStepMapper agentStepMapper;
    private final AgentTaskMapper agentTaskMapper;

    public AgentStepService(AgentStepMapper agentStepMapper, AgentTaskMapper agentTaskMapper) {
        this.agentStepMapper = agentStepMapper;
        this.agentTaskMapper = agentTaskMapper;
    }

    /**
     * 开始记录一个步骤，并返回步骤 id，供后续继续更新。
     */
    @Transactional
    public Long startStep(Long taskId, AgentStepType stepType, String stepName, String input) {
        requireTask(taskId);
        if (stepType == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "stepType cannot be null");
        }

        AgentStep agentStep = new AgentStep();
        agentStep.setTaskId(taskId);
        agentStep.setStepType(stepType.name());
        agentStep.setStepName(stepName == null ? "" : stepName.trim());
        agentStep.setInput(input);
        agentStep.setOutput(null);
        agentStep.setStatus(AgentStepStatus.RUNNING.name());
        agentStep.setErrorMessage(null);
        agentStep.setStartTime(LocalDateTime.now());
        agentStep.setEndTime(null);
        agentStepMapper.insert(agentStep);
        return agentStep.getId();
    }

    /**
     * 将步骤标记为成功，并记录输出和结束时间。
     */
    @Transactional
    public void successStep(Long stepId, String output) {
        AgentStep agentStep = requireStep(stepId);
        requireRunningStep(agentStep);
        agentStep.setOutput(output);
        agentStep.setStatus(AgentStepStatus.SUCCESS.name());
        agentStep.setErrorMessage(null);
        agentStep.setEndTime(LocalDateTime.now());
        agentStepMapper.updateById(agentStep);
    }

    /**
     * 将步骤标记为失败，并记录错误信息和结束时间。
     */
    @Transactional
    public void failStep(Long stepId, String errorMessage) {
        AgentStep agentStep = requireStep(stepId);
        requireRunningStep(agentStep);
        agentStep.setStatus(AgentStepStatus.FAILED.name());
        agentStep.setErrorMessage(errorMessage);
        agentStep.setEndTime(LocalDateTime.now());
        agentStepMapper.updateById(agentStep);
    }

    /**
     * 查询某个任务下的所有步骤，并校验任务归属。
     */
    public List<AgentStepVO> listTaskSteps(Long taskId) {
        requireOwnedTask(taskId);
        return agentStepMapper.selectList(new LambdaQueryWrapper<AgentStep>()
                        .eq(AgentStep::getTaskId, taskId)
                        .orderByAsc(AgentStep::getCreatedAt)
                        .orderByAsc(AgentStep::getId))
                .stream()
                .map(AgentStepVO::from)
                .toList();
    }

    /**
     * 校验任务是否属于当前用户。
     */
    private AgentTask requireOwnedTask(Long taskId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AgentTask agentTask = agentTaskMapper.selectOne(new LambdaQueryWrapper<AgentTask>()
                .eq(AgentTask::getId, taskId)
                .eq(AgentTask::getUserId, currentUserId)
                .last("limit 1"));
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }

    /**
     * 按任务 id 查询任务，供内部步骤记录使用。
     */
    private AgentTask requireTask(Long taskId) {
        AgentTask agentTask = agentTaskMapper.selectById(taskId);
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }

    /**
     * 按步骤 id 查询步骤，供内部更新使用。
     */
    private AgentStep requireStep(Long stepId) {
        AgentStep agentStep = agentStepMapper.selectById(stepId);
        if (agentStep == null) {
            throw new BusinessException(ErrorCode.AGENT_STEP_NOT_FOUND);
        }
        return agentStep;
    }

    /**
     * 只允许运行中的步骤进入终态，避免重复覆盖执行结果。
     */
    private void requireRunningStep(AgentStep agentStep) {
        if (!AgentStepStatus.RUNNING.name().equals(agentStep.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Agent step is already finished");
        }
    }
}
