package com.codepliot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.PatchRecord;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.AgentExecutionDecision;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.PatchGenerateResult;
import com.codepliot.model.PatchRecordVO;
import com.codepliot.model.PatchSafetyCheckResult;
import com.codepliot.model.TaskEventMessage;
import com.codepliot.policy.AgentExecutionPolicy;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.repository.PatchRecordMapper;
import com.codepliot.utils.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Patch 服务实现。
 *
 * <p>负责 patch_record 的读写、人工确认落库，以及确认后任务状态推进。
 */
@Service
public class PatchServiceImpl implements PatchService {

    private final PatchRecordMapper patchRecordMapper;
    private final AgentTaskMapper agentTaskMapper;
    private final ObjectMapper objectMapper;
    private final AgentExecutionPolicy agentExecutionPolicy;
    private final SseService sseService;

    /**
     * 创建 Patch 服务实现。
     */
    public PatchServiceImpl(PatchRecordMapper patchRecordMapper,
                            AgentTaskMapper agentTaskMapper,
                            ObjectMapper objectMapper,
                            AgentExecutionPolicy agentExecutionPolicy,
                            SseService sseService) {
        this.patchRecordMapper = patchRecordMapper;
        this.agentTaskMapper = agentTaskMapper;
        this.objectMapper = objectMapper;
        this.agentExecutionPolicy = agentExecutionPolicy;
        this.sseService = sseService;
    }

    /**
     * 保存成功生成的 patch 和安全检查结果。
     */
    @Override
    @Transactional
    public PatchRecord saveGeneratedPatch(Long taskId,
                                          PatchGenerateResult result,
                                          String rawOutput,
                                          PatchSafetyCheckResult safetyCheckResult) {
        requireTask(taskId);
        PatchRecord patchRecord = findOrCreate(taskId);
        patchRecord.setAnalysis(result.analysis());
        patchRecord.setSolution(result.solution());
        patchRecord.setPatch(result.patch());
        patchRecord.setRisk(result.risk());
        patchRecord.setSafetyCheckResult(toJson(safetyCheckResult));
        patchRecord.setRawOutput(rawOutput);
        patchRecord.setConfirmed(Boolean.FALSE);
        patchRecord.setConfirmedAt(null);
        persist(patchRecord);
        return patchRecord;
    }

    /**
     * 保存 patch 生成失败记录。
     */
    @Override
    @Transactional
    public PatchRecord saveFailedPatch(Long taskId, String rawOutput, String risk) {
        requireTask(taskId);
        PatchRecord patchRecord = findOrCreate(taskId);
        patchRecord.setAnalysis(null);
        patchRecord.setSolution(null);
        patchRecord.setPatch(null);
        patchRecord.setRisk(risk);
        patchRecord.setSafetyCheckResult(null);
        patchRecord.setRawOutput(rawOutput);
        patchRecord.setConfirmed(Boolean.FALSE);
        patchRecord.setConfirmedAt(null);
        persist(patchRecord);
        return patchRecord;
    }

    /**
     * 查询当前用户任务对应的 patch 记录。
     */
    @Override
    public PatchRecordVO getTaskPatch(Long taskId) {
        requireOwnedTask(taskId);
        return PatchRecordVO.from(requirePatchRecord(taskId));
    }

    /**
     * 人工确认 patch，并把任务推进到完成状态。
     */
    @Override
    @Transactional
    public AgentTaskVO confirmTaskPatch(Long taskId) {
        AgentTask agentTask = requireOwnedTask(taskId);
        if (!AgentTaskStatus.WAITING_CONFIRM.name().equals(agentTask.getStatus())) {
            throw new BusinessException(ErrorCode.PATCH_CONFIRM_NOT_ALLOWED, "Current task is not waiting for confirmation");
        }

        PatchRecord patchRecord = requirePatchRecord(taskId);
        patchRecord.setConfirmed(Boolean.TRUE);
        patchRecord.setConfirmedAt(LocalDateTime.now());
        persist(patchRecord);

        AgentExecutionDecision decision = agentExecutionPolicy.afterUserConfirmed();
        agentTask.setStatus(decision.status().name());
        agentTask.setResultSummary(decision.resultSummary());
        agentTask.setErrorMessage(null);
        agentTaskMapper.updateById(agentTask);

        sseService.push(new TaskEventMessage(
                taskId,
                decision.status().name(),
                "COMPLETED",
                null,
                decision.eventMessage(),
                LocalDateTime.now()
        ));
        sseService.complete(taskId);

        return AgentTaskVO.from(agentTaskMapper.selectById(taskId));
    }

    /**
     * 查询已有 patch 记录，不存在则创建空记录。
     */
    private PatchRecord findOrCreate(Long taskId) {
        PatchRecord existing = patchRecordMapper.selectOne(new LambdaQueryWrapper<PatchRecord>()
                .eq(PatchRecord::getTaskId, taskId)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        PatchRecord patchRecord = new PatchRecord();
        patchRecord.setTaskId(taskId);
        patchRecord.setConfirmed(Boolean.FALSE);
        return patchRecord;
    }

    /**
     * 持久化 patch 记录。
     */
    private void persist(PatchRecord patchRecord) {
        if (patchRecord.getId() == null) {
            patchRecordMapper.insert(patchRecord);
        } else {
            patchRecordMapper.updateById(patchRecord);
        }
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
     * 校验任务是否存在。
     */
    private AgentTask requireTask(Long taskId) {
        AgentTask agentTask = agentTaskMapper.selectById(taskId);
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }

    /**
     * 查询 patch 记录，不存在则抛出异常。
     */
    private PatchRecord requirePatchRecord(Long taskId) {
        PatchRecord patchRecord = patchRecordMapper.selectOne(new LambdaQueryWrapper<PatchRecord>()
                .eq(PatchRecord::getTaskId, taskId)
                .last("limit 1"));
        if (patchRecord == null) {
            throw new BusinessException(ErrorCode.PATCH_RECORD_NOT_FOUND);
        }
        return patchRecord;
    }

    /**
     * 序列化安全检查结果，便于直接落库。
     */
    private String toJson(PatchSafetyCheckResult safetyCheckResult) {
        if (safetyCheckResult == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(safetyCheckResult);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to serialize patch safety check result");
        }
    }
}
