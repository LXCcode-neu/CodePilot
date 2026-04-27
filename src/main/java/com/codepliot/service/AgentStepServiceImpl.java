package com.codepliot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.utils.SecurityUtils;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.entity.AgentTask;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.entity.AgentStep;
import com.codepliot.entity.AgentStepStatus;
import com.codepliot.entity.AgentStepType;
import com.codepliot.repository.AgentStepMapper;
import com.codepliot.service.AgentStepService;
import com.codepliot.model.AgentStepVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * AgentStepServiceImpl 服务类，负责封装业务流程和领域能力。
 */
@Service
public class AgentStepServiceImpl implements AgentStepService {

    private final AgentStepMapper agentStepMapper;
    private final AgentTaskMapper agentTaskMapper;
/**
 * 创建 AgentStepServiceImpl 实例。
 */
public AgentStepServiceImpl(AgentStepMapper agentStepMapper, AgentTaskMapper agentTaskMapper) {
        this.agentStepMapper = agentStepMapper;
        this.agentTaskMapper = agentTaskMapper;
    }
    /**
     * 执行 startStep 相关逻辑。
     */
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
    /**
     * 执行 successStep 相关逻辑。
     */
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
    /**
     * 执行 failStep 相关逻辑。
     */
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
    /**
     * 列出Task Steps相关逻辑。
     */
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
 * 检查并返回Owned Task相关逻辑。
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
 * 检查并返回Task相关逻辑。
 */
private AgentTask requireTask(Long taskId) {
        AgentTask agentTask = agentTaskMapper.selectById(taskId);
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }
/**
 * 检查并返回Step相关逻辑。
 */
private AgentStep requireStep(Long stepId) {
        AgentStep agentStep = agentStepMapper.selectById(stepId);
        if (agentStep == null) {
            throw new BusinessException(ErrorCode.AGENT_STEP_NOT_FOUND);
        }
        return agentStep;
    }
/**
 * 检查并返回Running Step相关逻辑。
 */
private void requireRunningStep(AgentStep agentStep) {
        if (!AgentStepStatus.RUNNING.name().equals(agentStep.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Agent step is already finished");
        }
    }
}


