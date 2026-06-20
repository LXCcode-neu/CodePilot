package com.codepliot.service.agent.tool;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentContext;
import com.codepliot.model.PatchReviewResult;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.PatchReviewService;
import com.codepliot.service.agent.ToolResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AI 补丁审查工具。
 * <p>
 * Agent 工具链中的补丁审查步骤（优先级 70）。委托 {@link PatchReviewService}
 * 使用 LLM 对生成的补丁进行代码审查，评估补丁质量、安全性和正确性，
 * 并将审查结果更新到 Agent 上下文中。
 * </p>
 */
@Component
@Order(70)
public class ReviewPatchTool implements AgentTool {

    private final PatchReviewService patchReviewService;

    public ReviewPatchTool(PatchReviewService patchReviewService) {
        this.patchReviewService = patchReviewService;
    }

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.REVIEWING_PATCH;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.REVIEW_PATCH;
    }

    @Override
    public String stepName() {
        return "AI Patch Review";
    }

    /**
     * 执行 AI 补丁审查。
     *
     * @param context Agent 执行上下文
     * @return 包含审查摘要和详细结果的工具执行结果
     */
    @Override
    public ToolResult execute(AgentContext context) {
        PatchReviewResult result = patchReviewService.review(context);
        context.updatePatchReviewResult(result);
        return ToolResult.success(result.summary(), result);
    }
}
