package com.codepliot.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.AgentStep;
import com.codepliot.entity.PatchRecord;
import com.codepliot.repository.AgentStepMapper;
import com.codepliot.repository.PatchRecordMapper;
import com.codepliot.service.patch.PatchReviewRecordService;
import com.codepliot.service.patch.PatchVerificationRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 运行清理服务。
 * <p>
 * 在 Agent 任务重新运行之前，清理该任务关联的历史数据，
 * 包括执行步骤记录（AgentStep）、补丁验证记录、补丁审查记录和补丁记录（PatchRecord），
 * 确保每次运行都是一个干净的起点。
 * </p>
 */
@Service
public class AgentRunCleanupService {

    private final AgentStepMapper agentStepMapper;
    private final PatchRecordMapper patchRecordMapper;
    private final PatchVerificationRecordService patchVerificationRecordService;
    private final PatchReviewRecordService patchReviewRecordService;

    public AgentRunCleanupService(AgentStepMapper agentStepMapper,
                                  PatchRecordMapper patchRecordMapper,
                                  PatchVerificationRecordService patchVerificationRecordService,
                                  PatchReviewRecordService patchReviewRecordService) {
        this.agentStepMapper = agentStepMapper;
        this.patchRecordMapper = patchRecordMapper;
        this.patchVerificationRecordService = patchVerificationRecordService;
        this.patchReviewRecordService = patchReviewRecordService;
    }

    /**
     * 在任务运行前清理历史数据。
     * <p>
     * 在同一事务中删除指定任务关联的所有历史记录，包括执行步骤、
     * 补丁验证记录、补丁审查记录和补丁记录。
     * </p>
     *
     * @param taskId 需要清理的任务 ID
     */
    @Transactional
    public void cleanupBeforeRun(Long taskId) {
        if (taskId == null) {
            return;
        }
        agentStepMapper.delete(new LambdaQueryWrapper<AgentStep>()
                .eq(AgentStep::getTaskId, taskId));
        patchVerificationRecordService.deleteByTaskIds(List.of(taskId));
        patchReviewRecordService.deleteByTaskIds(List.of(taskId));
        patchRecordMapper.delete(new LambdaQueryWrapper<PatchRecord>()
                .eq(PatchRecord::getTaskId, taskId));
    }
}
