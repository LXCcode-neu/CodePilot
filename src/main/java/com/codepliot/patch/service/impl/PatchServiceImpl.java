package com.codepliot.patch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.auth.security.SecurityUtils;
import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.patch.dto.PatchGenerateResult;
import com.codepliot.patch.entity.PatchRecord;
import com.codepliot.patch.mapper.PatchRecordMapper;
import com.codepliot.patch.service.PatchService;
import com.codepliot.patch.vo.PatchRecordVO;
import com.codepliot.task.entity.AgentTask;
import com.codepliot.task.mapper.AgentTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatchServiceImpl implements PatchService {

    private final PatchRecordMapper patchRecordMapper;
    private final AgentTaskMapper agentTaskMapper;

    public PatchServiceImpl(PatchRecordMapper patchRecordMapper, AgentTaskMapper agentTaskMapper) {
        this.patchRecordMapper = patchRecordMapper;
        this.agentTaskMapper = agentTaskMapper;
    }

    @Override
    @Transactional
    public PatchRecord saveGeneratedPatch(Long taskId, PatchGenerateResult result, String rawOutput) {
        requireTask(taskId);
        PatchRecord patchRecord = findOrCreate(taskId);
        patchRecord.setAnalysis(result.analysis());
        patchRecord.setSolution(result.solution());
        patchRecord.setPatch(result.patch());
        patchRecord.setRisk(result.risk());
        patchRecord.setRawOutput(rawOutput);
        persist(patchRecord);
        return patchRecord;
    }

    @Override
    @Transactional
    public PatchRecord saveFailedPatch(Long taskId, String rawOutput, String risk) {
        requireTask(taskId);
        PatchRecord patchRecord = findOrCreate(taskId);
        patchRecord.setAnalysis(null);
        patchRecord.setSolution(null);
        patchRecord.setPatch(null);
        patchRecord.setRisk(risk);
        patchRecord.setRawOutput(rawOutput);
        persist(patchRecord);
        return patchRecord;
    }

    @Override
    public PatchRecordVO getTaskPatch(Long taskId) {
        requireOwnedTask(taskId);
        PatchRecord patchRecord = patchRecordMapper.selectOne(new LambdaQueryWrapper<PatchRecord>()
                .eq(PatchRecord::getTaskId, taskId)
                .last("limit 1"));
        if (patchRecord == null) {
            throw new BusinessException(ErrorCode.PATCH_RECORD_NOT_FOUND);
        }
        return PatchRecordVO.from(patchRecord);
    }

    private PatchRecord findOrCreate(Long taskId) {
        PatchRecord existing = patchRecordMapper.selectOne(new LambdaQueryWrapper<PatchRecord>()
                .eq(PatchRecord::getTaskId, taskId)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        PatchRecord patchRecord = new PatchRecord();
        patchRecord.setTaskId(taskId);
        return patchRecord;
    }

    private void persist(PatchRecord patchRecord) {
        if (patchRecord.getId() == null) {
            patchRecordMapper.insert(patchRecord);
        } else {
            patchRecordMapper.updateById(patchRecord);
        }
    }

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

    private AgentTask requireTask(Long taskId) {
        AgentTask agentTask = agentTaskMapper.selectById(taskId);
        if (agentTask == null) {
            throw new BusinessException(ErrorCode.AGENT_TASK_NOT_FOUND);
        }
        return agentTask;
    }
}
