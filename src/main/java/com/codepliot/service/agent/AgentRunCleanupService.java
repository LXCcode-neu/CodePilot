package com.codepliot.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.AgentStep;
import com.codepliot.entity.PatchRecord;
import com.codepliot.repository.AgentStepMapper;
import com.codepliot.repository.PatchRecordMapper;
import com.codepliot.service.PatchVerificationRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentRunCleanupService {

    private final AgentStepMapper agentStepMapper;
    private final PatchRecordMapper patchRecordMapper;
    private final PatchVerificationRecordService patchVerificationRecordService;

    public AgentRunCleanupService(AgentStepMapper agentStepMapper,
                                  PatchRecordMapper patchRecordMapper,
                                  PatchVerificationRecordService patchVerificationRecordService) {
        this.agentStepMapper = agentStepMapper;
        this.patchRecordMapper = patchRecordMapper;
        this.patchVerificationRecordService = patchVerificationRecordService;
    }

    @Transactional
    public void cleanupBeforeRun(Long taskId) {
        if (taskId == null) {
            return;
        }
        agentStepMapper.delete(new LambdaQueryWrapper<AgentStep>()
                .eq(AgentStep::getTaskId, taskId));
        patchVerificationRecordService.deleteByTaskIds(List.of(taskId));
        patchRecordMapper.delete(new LambdaQueryWrapper<PatchRecord>()
                .eq(PatchRecord::getTaskId, taskId));
    }
}
