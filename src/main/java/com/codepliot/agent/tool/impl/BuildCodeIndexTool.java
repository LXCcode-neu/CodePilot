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
 * Mock 代码索引构建工具。
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
        return "构建代码索引";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        return ToolResult.success("mock code index completed", Map.of(
                "projectId", context.projectId(),
                "localPath", context.localPath() == null ? "" : context.localPath()
        ));
    }
}
