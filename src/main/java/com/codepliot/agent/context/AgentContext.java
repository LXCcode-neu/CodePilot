package com.codepliot.agent.context;

import com.codepliot.index.dto.RetrievedCodeChunk;
import java.util.List;

/**
 * Agent 运行上下文。
 * 保存一次任务执行过程中需要被多个 Tool 共享的核心信息。
 */
public final class AgentContext {

    private final Long taskId;
    private final Long userId;
    private final Long projectId;
    private final String repoUrl;
    private final String repoName;
    private String localPath;
    private final String issueTitle;
    private final String issueDescription;
    private List<RetrievedCodeChunk> retrievedChunks;
    private String analysis;

    public AgentContext(Long taskId,
                        Long userId,
                        Long projectId,
                        String repoUrl,
                        String repoName,
                        String localPath,
                        String issueTitle,
                        String issueDescription) {
        this.taskId = taskId;
        this.userId = userId;
        this.projectId = projectId;
        this.repoUrl = repoUrl;
        this.repoName = repoName;
        this.localPath = localPath;
        this.issueTitle = issueTitle;
        this.issueDescription = issueDescription;
        this.retrievedChunks = List.of();
        this.analysis = null;
    }

    public Long taskId() {
        return taskId;
    }

    public Long userId() {
        return userId;
    }

    public Long projectId() {
        return projectId;
    }

    public String repoUrl() {
        return repoUrl;
    }

    public String repoName() {
        return repoName;
    }

    public String localPath() {
        return localPath;
    }

    public String issueTitle() {
        return issueTitle;
    }

    public String issueDescription() {
        return issueDescription;
    }

    public List<RetrievedCodeChunk> retrievedChunks() {
        return retrievedChunks;
    }

    public String analysis() {
        return analysis;
    }

    /**
     * 在运行过程中更新本地仓库路径，供后续 Tool 复用。
     */
    public void updateLocalPath(String localPath) {
        this.localPath = localPath;
    }

    /**
     * 保存检索到的相关代码片段，供后续 Tool 复用。
     */
    public void updateRetrievedChunks(List<RetrievedCodeChunk> retrievedChunks) {
        this.retrievedChunks = retrievedChunks == null ? List.of() : List.copyOf(retrievedChunks);
    }

    /**
     * 保存基于 Issue 和相关代码片段生成的分析结果。
     */
    public void updateAnalysis(String analysis) {
        this.analysis = analysis == null || analysis.isBlank() ? null : analysis.trim();
    }
}
