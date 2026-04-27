package com.codepliot.service;

import com.codepliot.model.AgentContext;
import com.codepliot.service.AgentTool;
import com.codepliot.service.ToolResult;
import com.codepliot.service.GitService;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.AgentStepType;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
/**
 * CloneRepositoryTool 服务类，负责封装业务流程和领域能力。
 */
@Component
@Order(10)
public class CloneRepositoryTool implements AgentTool {

    private final GitService gitService;
/**
 * 创建 CloneRepositoryTool 实例。
 */
public CloneRepositoryTool(GitService gitService) {
        this.gitService = gitService;
    }
    /**
     * 执行 taskStatus 相关逻辑。
     */
@Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.CLONING;
    }
    /**
     * 执行 stepType 相关逻辑。
     */
@Override
    public AgentStepType stepType() {
        return AgentStepType.CLONE_REPOSITORY;
    }
    /**
     * 执行 stepName 相关逻辑。
     */
@Override
    public String stepName() {
        return "拉取仓库";
    }
    /**
     * 执行 execute 相关逻辑。
     */
@Override
    public ToolResult execute(AgentContext context) {
        String localPath = gitService.cloneRepository(context.projectId());
        context.updateLocalPath(localPath);
        return ToolResult.success("repository clone completed", Map.of("localPath", localPath));
    }
}

