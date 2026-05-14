package com.codepliot.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentExecutionDecision;
import com.codepliot.model.PatchVerificationResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentExecutionPolicyTest {

    @Test
    void afterPatchGeneratedBlocksConfirmationWhenVerificationFails() {
        AgentExecutionPolicy policy = new AgentExecutionPolicy();
        PatchVerificationResult verificationResult = new PatchVerificationResult(
                false,
                true,
                false,
                "mvn test failed",
                List.of("MAVEN"),
                List.of()
        );

        AgentExecutionDecision decision = policy.afterPatchGenerated(null, verificationResult);

        assertEquals(AgentTaskStatus.VERIFY_FAILED, decision.status());
        assertEquals("mvn test failed", decision.resultSummary());
    }

    @Test
    void afterPatchGeneratedWaitsForConfirmationWhenVerificationPasses() {
        AgentExecutionPolicy policy = new AgentExecutionPolicy();
        PatchVerificationResult verificationResult = new PatchVerificationResult(
                false,
                true,
                true,
                "passed",
                List.of("MAVEN"),
                List.of()
        );

        AgentExecutionDecision decision = policy.afterPatchGenerated(null, verificationResult);

        assertEquals(AgentTaskStatus.WAITING_CONFIRM, decision.status());
    }
}
