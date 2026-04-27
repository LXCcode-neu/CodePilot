package com.codepliot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.utils.SecurityUtils;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.PatchGenerateResult;
import com.codepliot.entity.PatchRecord;
import com.codepliot.repository.PatchRecordMapper;
import com.codepliot.service.PatchService;
import com.codepliot.model.PatchRecordVO;
import com.codepliot.entity.AgentTask;
import com.codepliot.repository.AgentTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * PatchServiceImpl 服务类，负责封装业务流程和领域能力。
 */
@Service
public class PatchServiceImpl implements PatchService {

    private final PatchRecordMapper patchRecordMapper;
    private final AgentTaskMapper agentTaskMapper;
/**
 * 创建 PatchServiceImpl 实例。
 */
public PatchServiceImpl(PatchRecordMapper patchRecordMapper, AgentTaskMapper agentTaskMapper) {
        this.patchRecordMapper = patchRecordMapper;
        this.agentTaskMapper = agentTaskMapper;
    }
    /**
     * 保存Generated Patch相关逻辑。
     */
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
    /**
     * 保存Failed Patch相关逻辑。
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
        patchRecord.setRawOutput(rawOutput);
        persist(patchRecord);
        return patchRecord;
    }
    /**
     * 获取Task Patch相关逻辑。
     */
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
/**
 * 查找Or Create相关逻辑。
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
        return patchRecord;
    }
/**
 * 执行 persist 相关逻辑。
 */
private void persist(PatchRecord patchRecord) {
        if (patchRecord.getId() == null) {
            patchRecordMapper.insert(patchRecord);
        } else {
            patchRecordMapper.updateById(patchRecord);
        }
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
}


