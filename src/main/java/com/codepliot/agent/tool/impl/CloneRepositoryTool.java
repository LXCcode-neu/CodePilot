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
 * Mock 仓库拉取工具。
 */
@Component
@Order(10)
public class CloneRepositoryTool implements AgentTool {

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.CLONING;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.CLONE_REPOSITORY;
    }

    @Override
    public String stepName() {
        return "拉取仓库";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        return ToolResult.success("mock clone repository completed", Map.of(
                "repoUrl", context.repoUrl(),
                "repoName", context.repoName()
        ));
    }
}
