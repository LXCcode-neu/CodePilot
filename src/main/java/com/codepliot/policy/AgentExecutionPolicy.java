package com.codepliot.policy;

import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentExecutionDecision;
import com.codepliot.model.PatchSafetyCheckResult;
import org.springframework.stereotype.Component;

/**
 * Agent 执行策略。
 *
 * <p>负责决定 patch 生成之后，以及用户人工确认之后，任务应该进入的状态和摘要文案。
 */
@Component
public class AgentExecutionPolicy {

    /**
     * 决定 patch 生成完成后的任务状态。
     *
     * <p>当前版本统一进入 WAITING_CONFIRM，由用户确认后再进入最终完成状态。
     */
    public AgentExecutionDecision afterPatchGenerated(PatchSafetyCheckResult safetyCheckResult) {
        String summary = "Patch 已生成，等待人工确认";
        if (safetyCheckResult != null && safetyCheckResult.requiresAttention()) {
            summary = "Patch 已生成，存在风险提示，等待人工确认";
        }
        return new AgentExecutionDecision(
                AgentTaskStatus.WAITING_CONFIRM,
                summary,
                summary + "。"
        );
    }

    /**
     * 决定用户确认后的任务状态。
     */
    public AgentExecutionDecision afterUserConfirmed() {
        return new AgentExecutionDecision(
                AgentTaskStatus.COMPLETED,
                "Patch 已人工确认",
                "任务已确认，可进入后续流程。"
        );
    }
}
