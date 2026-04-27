package com.codepliot.service;

import com.codepliot.model.AgentContext;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.AgentStepType;
/**
 * AgentTool 服务类，负责封装业务流程和领域能力。
 */
public interface AgentTool {
AgentTaskStatus taskStatus();
AgentStepType stepType();
String stepName();
ToolResult execute(AgentContext context);
}

