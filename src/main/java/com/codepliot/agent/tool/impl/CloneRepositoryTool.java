package com.codepliot.agent.tool.impl;

import com.codepliot.agent.context.AgentContext;
import com.codepliot.agent.tool.AgentTool;
import com.codepliot.agent.tool.ToolResult;
import com.codepliot.git.service.GitService;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.trace.entity.AgentStepType;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 仓库拉取工具。
 * 在 Agent 流程中负责调用 GitService 执行真实仓库 clone。
 */
@Component
@Order(10)
public class CloneRepositoryTool implements AgentTool {

    private final GitService gitService;

    public CloneRepositoryTool(GitService gitService) {
        this.gitService = gitService;
    }

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
        String localPath = gitService.cloneRepository(context.projectId());
        context.updateLocalPath(localPath);
        return ToolResult.success("repository clone completed", Map.of("localPath", localPath));
    }
}
