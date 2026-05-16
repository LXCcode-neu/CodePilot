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
                    "Patch 自动验证失败，已阻止 PR 确认。");
            return new AgentExecutionDecision(AgentTaskStatus.VERIFY_FAILED, summary, summary);
        }

        if (reviewResult != null && !reviewResult.passed()) {
            String summary = nullToFallback(reviewResult.summary(),
                    "AI 代码审查失败，已阻止 PR 确认。");
            return new AgentExecutionDecision(AgentTaskStatus.VERIFY_FAILED, summary, summary);
        }

        String summary = "Patch 已生成并通过验证，等待人工确认。";
        if (reviewResult != null && "MEDIUM".equalsIgnoreCase(reviewResult.riskLevel())) {
            summary = "Patch 已通过验证和 AI 审查，但存在中风险提示，等待人工确认。";
        }
        if (safetyCheckResult != null && safetyCheckResult.requiresAttention()) {
            summary = "Patch 已生成，但存在安全提示，等待人工确认。";
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
            "Patch 已人工确认。",
            "任务已确认，可以进入后续流程。"
        );
    }

    private String nullToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
