package com.codepliot.service.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codepliot.config.PatchVerificationProperties;
import com.codepliot.entity.PatchRecord;
import com.codepliot.model.AgentContext;
import com.codepliot.model.PatchGenerateResult;
import com.codepliot.model.PatchVerificationResult;
import com.codepliot.policy.PatchSafetyPolicy;
import com.codepliot.service.llm.LlmService;
import com.codepliot.service.llm.PatchPromptBuilder;
import com.codepliot.service.patch.PatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class PatchRepairServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void repairUntilVerifiedGeneratesReplacementPatchAndUpdatesContext() {
        PatchService patchService = mock(PatchService.class);
        PatchVerificationService patchVerificationService = mock(PatchVerificationService.class);
        PatchRecord patchRecord = new PatchRecord();
        patchRecord.setId(20L);
        patchRecord.setTaskId(10L);
        when(patchService.saveGeneratedPatch(anyLong(), any(), anyString(), any())).thenReturn(patchRecord);
        PatchVerificationResult passedVerification = new PatchVerificationResult(
                false,
                true,
                true,
                "passed",
                List.of("MAVEN"),
                List.of()
        );
        when(patchVerificationService.verify(anyString(), anyLong(), anyLong(), anyString())).thenReturn(passedVerification);

        LlmService llmService = new LlmService((system, user) -> """
                {
                  "analysis": "repair analysis",
                  "solution": "repair solution",
                  "patch": "diff --git a/A.java b/A.java\\n--- a/A.java\\n+++ b/A.java\\n@@ -1 +1 @@\\n-old\\n+new\\n",
                  "risk": "low"
                }
                """);
        PatchVerificationProperties properties = new PatchVerificationProperties();
        properties.setMaxRepairAttempts(3);
        PatchRepairService service = new PatchRepairService(
                llmService,
                new PatchPromptBuilder(),
                patchService,
                new PatchSafetyPolicy(),
                new PatchTextNormalizer(),
                patchVerificationService,
                properties,
                new ObjectMapper()
        );

        AgentContext context = new AgentContext(
                10L,
                7L,
                3L,
                "https://github.com/acme/repo",
                "repo",
                tempDir.toString(),
                "bug",
                "fix it"
        );
        context.updatePatchGenerateResult(new PatchGenerateResult("old", "old", "old patch", "risk"));
        context.updatePatchVerificationResult(new PatchVerificationResult(
                false,
                true,
                false,
                "failed",
                List.of("MAVEN"),
                List.of()
        ));

        Map<String, Object> result = service.repairUntilVerified(context);

        assertEquals(Boolean.TRUE, result.get("attempted"));
        assertEquals(1, result.get("attemptCount"));
        assertTrue(context.patchVerificationResult().passed());
        assertEquals(20L, context.patchRecordId());
        verify(patchService).saveGeneratedPatch(anyLong(), any(), anyString(), any());
    }
}
