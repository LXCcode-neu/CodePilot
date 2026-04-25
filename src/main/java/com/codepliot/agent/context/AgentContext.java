package com.codepliot.agent.context;

/**
 * Agent 执行上下文，保存一次任务运行需要的核心数据。
 */
public record AgentContext(
        Long taskId,
        Long userId,
        Long projectId,
        String repoUrl,
        String repoName,
        String localPath,
        String issueTitle,
        String issueDescription
) {
}
