package com.codepliot.model;

import com.codepliot.entity.GitHubIssueEvent;
import java.time.LocalDateTime;

public record GitHubIssueEventVO(
        Long id,
        Long repoWatchId,
        Long projectRepoId,
        Integer issueNumber,
        String issueTitle,
        String issueBody,
        String issueUrl,
        String issueState,
        String senderLogin,
        String status,
        Long agentTaskId,
        LocalDateTime notifiedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GitHubIssueEventVO from(GitHubIssueEvent event) {
        return new GitHubIssueEventVO(
                event.getId(),
                event.getRepoWatchId(),
                event.getProjectRepoId(),
                event.getIssueNumber(),
                event.getIssueTitle(),
                event.getIssueBody(),
                event.getIssueUrl(),
                event.getIssueState(),
                event.getSenderLogin(),
                event.getStatus(),
                event.getAgentTaskId(),
                event.getNotifiedAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
