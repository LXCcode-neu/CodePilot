package com.codepliot.service.agent.tool;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentContext;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.ToolResult;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Compatibility step for the old index build phase.
 *
 * <p>Code search now uses on-demand grep, so this tool keeps the existing Agent
 * step/status sequence without invoking the removed prebuilt index pipeline.
 */
@Component
@Order(20)
public class BuildCodeIndexTool implements AgentTool {

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.INDEXING;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.BUILD_CODE_INDEX;
    }

    @Override
    public String stepName() {
        return "Skip code index build";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        return ToolResult.success("code index build skipped in grep search mode", Map.of(
                "mode", "grep",
                "skipped", true,
                "reason", "Code search uses on-demand grep mode; prebuilt indexes are no longer required"
        ));
    }
}
