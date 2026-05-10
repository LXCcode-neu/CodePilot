package com.codepliot.service.agent.tool;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentContext;
import com.codepliot.model.PatchGenerateResult;
import com.codepliot.model.PatchVerificationResult;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.PatchVerificationService;
import com.codepliot.service.agent.ToolResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Patch 自动验证工具。
 */
@Component
@Order(60)
public class VerifyPatchTool implements AgentTool {

    private final PatchVerificationService patchVerificationService;

    public VerifyPatchTool(PatchVerificationService patchVerificationService) {
        this.patchVerificationService = patchVerificationService;
    }

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.VERIFYING;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.VERIFY_PATCH;
    }

    @Override
    public String stepName() {
        return "验证 Patch";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        PatchGenerateResult patchGenerateResult = context.patchGenerateResult();
        String patchText = patchGenerateResult == null ? null : patchGenerateResult.patch();
        PatchVerificationResult result = patchVerificationService.verify(
                context.localPath(),
                context.taskId(),
                patchText
        );
        return ToolResult.success(result.summary(), result);
    }
}
