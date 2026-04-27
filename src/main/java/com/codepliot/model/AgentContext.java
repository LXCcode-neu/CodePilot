package com.codepliot.model;

import java.util.List;

/**
 * Agent 执行上下文。
 *
 * <p>用于在各个 AgentTool 之间传递仓库路径、Issue 信息、检索结果、分析结果和 patch 安全检查结果。
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
    private PatchSafetyCheckResult patchSafetyCheckResult;

    /**
     * 创建 Agent 执行上下文。
     */
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
        this.patchSafetyCheckResult = null;
    }

    /**
     * 返回任务 ID。
     */
    public Long taskId() {
        return taskId;
    }

    /**
     * 返回用户 ID。
     */
    public Long userId() {
        return userId;
    }

    /**
     * 返回项目 ID。
     */
    public Long projectId() {
        return projectId;
    }

    /**
     * 返回仓库 URL。
     */
    public String repoUrl() {
        return repoUrl;
    }

    /**
     * 返回仓库名称。
     */
    public String repoName() {
        return repoName;
    }

    /**
     * 返回本地工作目录。
     */
    public String localPath() {
        return localPath;
    }

    /**
     * 返回 Issue 标题。
     */
    public String issueTitle() {
        return issueTitle;
    }

    /**
     * 返回 Issue 描述。
     */
    public String issueDescription() {
        return issueDescription;
    }

    /**
     * 返回检索到的相关代码片段。
     */
    public List<RetrievedCodeChunk> retrievedChunks() {
        return retrievedChunks;
    }

    /**
     * 返回 LLM 分析结果。
     */
    public String analysis() {
        return analysis;
    }

    /**
     * 返回 patch 安全检查结果。
     */
    public PatchSafetyCheckResult patchSafetyCheckResult() {
        return patchSafetyCheckResult;
    }

    /**
     * 更新本地工作目录。
     */
    public void updateLocalPath(String localPath) {
        this.localPath = localPath;
    }

    /**
     * 更新检索结果。
     */
    public void updateRetrievedChunks(List<RetrievedCodeChunk> retrievedChunks) {
        this.retrievedChunks = retrievedChunks == null ? List.of() : List.copyOf(retrievedChunks);
    }

    /**
     * 更新分析结果。
     */
    public void updateAnalysis(String analysis) {
        this.analysis = analysis == null || analysis.isBlank() ? null : analysis.trim();
    }

    /**
     * 更新 patch 安全检查结果。
     */
    public void updatePatchSafetyCheckResult(PatchSafetyCheckResult patchSafetyCheckResult) {
        this.patchSafetyCheckResult = patchSafetyCheckResult;
    }
}
