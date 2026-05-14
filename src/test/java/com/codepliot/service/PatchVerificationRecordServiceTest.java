package com.codepliot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codepliot.entity.PatchVerificationRecord;
import com.codepliot.model.PatchVerificationCommandResult;
import com.codepliot.model.PatchVerificationResult;
import com.codepliot.repository.PatchVerificationRecordMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PatchVerificationRecordServiceTest {

    @Test
    void saveVerificationResultUsesNextAttemptNumber() {
        PatchVerificationRecordMapper mapper = org.mockito.Mockito.mock(PatchVerificationRecordMapper.class);
        PatchVerificationRecord latest = new PatchVerificationRecord();
        latest.setAttemptNo(2);
        when(mapper.selectOne(any())).thenReturn(latest);

        PatchVerificationRecordService service = new PatchVerificationRecordService(mapper);
        PatchVerificationResult result = new PatchVerificationResult(
                false,
                true,
                false,
                "failed",
                List.of("MAVEN"),
                List.of(new PatchVerificationCommandResult(
                        "maven test",
                        "mvn test",
                        "repo",
                        1,
                        false,
                        false,
                        "test failed"
                ))
        );

        service.saveVerificationResult(10L, 20L, result);

        ArgumentCaptor<PatchVerificationRecord> captor = ArgumentCaptor.forClass(PatchVerificationRecord.class);
        verify(mapper).insert(captor.capture());
        PatchVerificationRecord record = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(10L, record.getTaskId());
        org.junit.jupiter.api.Assertions.assertEquals(20L, record.getPatchRecordId());
        org.junit.jupiter.api.Assertions.assertEquals(3, record.getAttemptNo());
        org.junit.jupiter.api.Assertions.assertEquals("maven test", record.getCommandName());
        org.junit.jupiter.api.Assertions.assertEquals(1, record.getExitCode());
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, record.getPassed());
    }
}
