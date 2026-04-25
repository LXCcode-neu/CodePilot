package com.codepliot.agent.tool;

import com.codepliot.agent.context.AgentContext;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.trace.entity.AgentStepType;

/**
 * Agent 工具接口，封装某一个执行步骤的具体动作。
 */
public interface AgentTool {

    /**
     * 当前工具对应的任务状态。
     */
    AgentTaskStatus taskStatus();

    /**
     * 当前工具对应的执行轨迹类型。
     */
    AgentStepType stepType();

    /**
     * 当前工具展示在执行轨迹中的步骤名称。
     */
    String stepName();

    /**
     * 执行工具动作。当前阶段只返回 mock 结果。
     */
    ToolResult execute(AgentContext context);
}
