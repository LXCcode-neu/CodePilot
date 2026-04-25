package com.codepliot.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.auth.security.SecurityUtils;
import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.task.entity.AgentTask;
import com.codepliot.task.mapper.AgentTaskMapper;
import com.codepliot.trace.entity.AgentStep;
import com.codepliot.trace.entity.AgentStepStatus;
import com.codepliot.trace.entity.AgentStepType;
import com.codepliot.trace.mapper.AgentStepMapper;
import com.codepliot.trace.service.AgentStepService;
import com.codepliot.trace.vo.AgentStepVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent Step 服务实现，负责记录和查询执行轨迹。
 */
@Service
public class AgentStepServiceImpl implements AgentStepService {

    private final AgentStepMapper agentStepMapper;
    private final AgentTaskMapper agentTaskMapper;

    public AgentStepServiceImpl(AgentStepMapper agentStepMapper, AgentTaskMapper agentTaskMapper) {
        this.agentStepMapper = agentStepMapper;
        this.agentTaskMapper = agentTaskMapper;
    }

    @Override
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

    @Override
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

    @Override
    @Transactional
    public void failStep(Long stepId, String errorMessage) {
        AgentStep agentStep = requireStep(stepId);
        requireRunningStep(agentStep);
        agentStep.setStatus(AgentStepStatus.FAILED.name());
        agentStep.setErrorMessage(errorMessage);
        agentStep.setEndTime(LocalDateTime.now());
        agentStepMapper.updateById(agentStep);
    }

    @Override
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
     * 校验任务是否属于当前登录用户。
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
     * 按任务 id 查询任务，供内部记录步骤时使用。
     */
    private AgentTask requireTask(Long taskId) {
        AgentTask agentTask = agentTaskMapper.selectById(taskId);
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }

    /**
     * 按步骤 id 查询步骤。
     */
    private AgentStep requireStep(Long stepId) {
        AgentStep agentStep = agentStepMapper.selectById(stepId);
        if (agentStep == null) {
            throw new BusinessException(ErrorCode.AGENT_STEP_NOT_FOUND);
        }
        return agentStep;
    }

    /**
     * 只允许运行中的步骤进入终态，避免重复覆盖结果。
     */
    private void requireRunningStep(AgentStep agentStep) {
        if (!AgentStepStatus.RUNNING.name().equals(agentStep.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Agent step is already finished");
        }
    }
}
