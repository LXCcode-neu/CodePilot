package com.codepliot.agent.tool.impl;

import com.codepliot.agent.context.AgentContext;
import com.codepliot.agent.tool.AgentTool;
import com.codepliot.agent.tool.ToolResult;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.trace.entity.AgentStepType;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Mock Issue 分析工具。
 */
@Component
@Order(40)
public class AnalyzeIssueTool implements AgentTool {

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.ANALYZING;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.ANALYZE_ISSUE;
    }

    @Override
    public String stepName() {
        return "分析 Issue";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        return ToolResult.success("mock issue analysis completed", Map.of(
                "issueTitle", context.issueTitle(),
                "summary", "mock analysis result"
        ));
    }
}
