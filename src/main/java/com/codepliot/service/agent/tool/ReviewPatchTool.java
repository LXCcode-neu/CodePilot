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

    @Override
    public ToolResult execute(AgentContext context) {
        PatchReviewResult result = patchReviewService.review(context);
        context.updatePatchReviewResult(result);
        return ToolResult.success(result.summary(), result);
    }
}
