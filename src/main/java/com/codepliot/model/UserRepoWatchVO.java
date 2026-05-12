package com.codepliot.model;

import com.codepliot.entity.UserRepoWatch;
import java.time.LocalDateTime;

public record UserRepoWatchVO(
        Long id,
        Long projectRepoId,
        String owner,
        String repoName,
        String repoUrl,
        String defaultBranch,
        Boolean watchEnabled,
        String watchMode,
        LocalDateTime lastCheckedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserRepoWatchVO from(UserRepoWatch watch) {
        return new UserRepoWatchVO(
                watch.getId(),
                watch.getProjectRepoId(),
                watch.getOwner(),
                watch.getRepoName(),
                watch.getRepoUrl(),
                watch.getDefaultBranch(),
                watch.getWatchEnabled(),
                watch.getWatchMode(),
                watch.getLastCheckedAt(),
                watch.getCreatedAt(),
                watch.getUpdatedAt()
        );
    }
}
