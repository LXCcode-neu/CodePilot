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
 * Mock 相关代码检索工具。
 */
@Component
@Order(30)
public class SearchRelevantCodeTool implements AgentTool {

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.RETRIEVING;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.SEARCH_RELEVANT_CODE;
    }

    @Override
    public String stepName() {
        return "检索相关代码";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        return ToolResult.success("mock relevant code search completed", Map.of(
                "issueTitle", context.issueTitle(),
                "matchedFiles", 0
        ));
    }
}
