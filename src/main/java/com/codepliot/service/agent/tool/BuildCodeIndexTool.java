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
 * 旧索引构建阶段的兼容步骤。
 *
 * <p>代码检索现在使用按需 grep，因此该工具只保留现有 Agent 步骤和状态顺序，
 * 不再调用已移除的预构建索引流程。
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
