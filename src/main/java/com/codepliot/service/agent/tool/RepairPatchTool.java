package com.codepliot.service.agent.tool;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentContext;
import com.codepliot.model.PatchVerificationResult;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.PatchRepairService;
import com.codepliot.service.agent.ToolResult;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(60)
public class RepairPatchTool implements AgentTool {

    private final PatchRepairService patchRepairService;

    public RepairPatchTool(PatchRepairService patchRepairService) {
        this.patchRepairService = patchRepairService;
    }

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.REPAIRING_PATCH;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.REPAIR_PATCH;
    }

    @Override
    public String stepName() {
        return "Repair Patch";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        PatchVerificationResult verification = context.patchVerificationResult();
        if (verification == null || verification.passed()) {
            return ToolResult.success("patch repair skipped", Map.of("attempted", false));
        }
        Map<String, Object> result = patchRepairService.repairUntilVerified(context);
        return ToolResult.success("patch repair completed", result);
    }
}
