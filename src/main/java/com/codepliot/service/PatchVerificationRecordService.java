package com.codepliot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codepliot.entity.PatchVerificationRecord;
import com.codepliot.model.PatchVerificationCommandResult;
import com.codepliot.model.PatchVerificationResult;
import com.codepliot.repository.PatchVerificationRecordMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatchVerificationRecordService {

    private final PatchVerificationRecordMapper patchVerificationRecordMapper;

    public PatchVerificationRecordService(PatchVerificationRecordMapper patchVerificationRecordMapper) {
        this.patchVerificationRecordMapper = patchVerificationRecordMapper;
    }

    @Transactional
    public void saveVerificationResult(Long taskId, Long patchRecordId, PatchVerificationResult result) {
        if (taskId == null || result == null) {
            return;
        }
        int attemptNo = nextAttemptNo(taskId);
        List<PatchVerificationCommandResult> commands = result.commands();
        if (commands.isEmpty()) {
            insertRecord(taskId, patchRecordId, attemptNo, summaryCommand(result));
            return;
        }
        for (PatchVerificationCommandResult command : commands) {
            insertRecord(taskId, patchRecordId, attemptNo, command);
        }
    }

    @Transactional
    public void deleteByTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        patchVerificationRecordMapper.delete(new LambdaUpdateWrapper<PatchVerificationRecord>()
                .in(PatchVerificationRecord::getTaskId, taskIds));
    }

    private int nextAttemptNo(Long taskId) {
        PatchVerificationRecord latest = patchVerificationRecordMapper.selectOne(
                new LambdaQueryWrapper<PatchVerificationRecord>()
                        .eq(PatchVerificationRecord::getTaskId, taskId)
                        .orderByDesc(PatchVerificationRecord::getAttemptNo)
                        .last("limit 1")
        );
        if (latest == null || latest.getAttemptNo() == null) {
            return 1;
        }
        return latest.getAttemptNo() + 1;
    }

    private void insertRecord(Long taskId,
                              Long patchRecordId,
                              int attemptNo,
                              PatchVerificationCommandResult command) {
        PatchVerificationRecord record = new PatchVerificationRecord();
        record.setTaskId(taskId);
        record.setPatchRecordId(patchRecordId);
        record.setAttemptNo(attemptNo);
        record.setCommandName(nullToFallback(command.name(), "verification"));
        record.setCommandText(command.command());
        record.setWorkingDirectory(command.workingDirectory());
        record.setExitCode(command.exitCode());
        record.setPassed(command.passed());
        record.setTimedOut(command.timedOut());
        record.setOutputSummary(command.outputSummary());
        patchVerificationRecordMapper.insert(record);
    }

    private PatchVerificationCommandResult summaryCommand(PatchVerificationResult result) {
        return new PatchVerificationCommandResult(
                "verification summary",
                "",
                "",
                result.passed() ? 0 : -1,
                result.passed(),
                false,
                result.summary()
        );
    }

    private String nullToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
