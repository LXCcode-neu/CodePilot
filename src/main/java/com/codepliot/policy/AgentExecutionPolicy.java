package com.codepliot.policy;

import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentExecutionDecision;
import com.codepliot.model.PatchReviewResult;
import com.codepliot.model.PatchSafetyCheckResult;
import com.codepliot.model.PatchVerificationResult;
import org.springframework.stereotype.Component;

@Component
public class AgentExecutionPolicy {

    public AgentExecutionDecision afterPatchGenerated(PatchSafetyCheckResult safetyCheckResult,
                                                      PatchVerificationResult verificationResult,
                                                      PatchReviewResult reviewResult) {
        if (verificationResult != null && !verificationResult.passed()) {
            String summary = nullToFallback(verificationResult.summary(),
                    "Patch automatic verification failed. PR confirmation is blocked.");
            return new AgentExecutionDecision(AgentTaskStatus.VERIFY_FAILED, summary, summary);
        }

        if (reviewResult != null && !reviewResult.passed()) {
            String summary = nullToFallback(reviewResult.summary(),
                    "AI Patch Review failed. PR confirmation is blocked.");
            return new AgentExecutionDecision(AgentTaskStatus.VERIFY_FAILED, summary, summary);
        }

        String summary = "Patch generated and verified. Waiting for manual confirmation.";
        if (reviewResult != null && "MEDIUM".equalsIgnoreCase(reviewResult.riskLevel())) {
            summary = "Patch passed verification and AI Review with medium-risk notes. Waiting for manual confirmation.";
        }
        if (safetyCheckResult != null && safetyCheckResult.requiresAttention()) {
            summary = "Patch generated with safety notes. Waiting for manual confirmation.";
        }
        return new AgentExecutionDecision(
                AgentTaskStatus.WAITING_CONFIRM,
                summary,
                summary
        );
    }

    public AgentExecutionDecision afterUserConfirmed() {
        return new AgentExecutionDecision(
                AgentTaskStatus.COMPLETED,
                "Patch manually confirmed.",
                "Task confirmed and ready for the next workflow."
        );
    }

    private String nullToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
