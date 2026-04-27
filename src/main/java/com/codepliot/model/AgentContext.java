package com.codepliot.model;

import com.codepliot.model.RetrievedCodeChunk;
import java.util.List;
/**
 * AgentContext 模型类，用于承载流程中的数据结构。
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
/**
 * 创建 AgentContext 实例。
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
    }
/**
 * 执行 taskId 相关逻辑。
 */
public Long taskId() {
        return taskId;
    }
/**
 * 执行 userId 相关逻辑。
 */
public Long userId() {
        return userId;
    }
/**
 * 执行 projectId 相关逻辑。
 */
public Long projectId() {
        return projectId;
    }
/**
 * 执行 repoUrl 相关逻辑。
 */
public String repoUrl() {
        return repoUrl;
    }
/**
 * 执行 repoName 相关逻辑。
 */
public String repoName() {
        return repoName;
    }
/**
 * 执行 localPath 相关逻辑。
 */
public String localPath() {
        return localPath;
    }
/**
 * 执行 issueTitle 相关逻辑。
 */
public String issueTitle() {
        return issueTitle;
    }
/**
 * 执行 issueDescription 相关逻辑。
 */
public String issueDescription() {
        return issueDescription;
    }
/**
 * 执行 retrievedChunks 相关逻辑。
 */
public List<RetrievedCodeChunk> retrievedChunks() {
        return retrievedChunks;
    }
/**
 * 执行 analysis 相关逻辑。
 */
public String analysis() {
        return analysis;
    }
/**
 * 更新Local Path相关逻辑。
 */
public void updateLocalPath(String localPath) {
        this.localPath = localPath;
    }
/**
 * 更新Retrieved Chunks相关逻辑。
 */
public void updateRetrievedChunks(List<RetrievedCodeChunk> retrievedChunks) {
        this.retrievedChunks = retrievedChunks == null ? List.of() : List.copyOf(retrievedChunks);
    }
/**
 * 更新Analysis相关逻辑。
 */
public void updateAnalysis(String analysis) {
        this.analysis = analysis == null || analysis.isBlank() ? null : analysis.trim();
    }
}

