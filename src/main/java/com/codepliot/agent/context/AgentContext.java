package com.codepliot.agent.context;

public final class AgentContext {

    private final Long taskId;
    private final Long userId;
    private final Long projectId;
    private final String repoUrl;
    private final String repoName;
    private String localPath;
    private final String issueTitle;
    private final String issueDescription;

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

    public void updateLocalPath(String localPath) {
        this.localPath = localPath;
    }
}
