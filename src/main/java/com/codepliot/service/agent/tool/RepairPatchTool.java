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

/**
 * 修复补丁工具。
 * <p>
 * Agent 工具链中的补丁修复步骤（优先级 60）。当补丁验证失败时，
 * 委托 {@link PatchRepairService} 进行多轮 LLM 驱动的自动修复，
 * 直到验证通过或达到最大尝试次数。如果验证已通过则跳过修复。
 * </p>
 */
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

    /**
     * 执行补丁修复。
     * <p>
     * 如果补丁验证结果为空或已通过，则跳过修复直接返回成功；
     * 否则调用修复服务进行多轮自动修复。
     * </p>
     *
     * @param context Agent 执行上下文
     * @return 工具执行结果
     */
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
