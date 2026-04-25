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
 * Mock Patch 生成工具。
 */
@Component
@Order(50)
public class GeneratePatchTool implements AgentTool {

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.GENERATING_PATCH;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.GENERATE_PATCH;
    }

    @Override
    public String stepName() {
        return "生成 Patch";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        return ToolResult.success("mock patch generation completed", Map.of(
                "taskId", context.taskId(),
                "patch", "mock patch content"
        ));
    }
}
