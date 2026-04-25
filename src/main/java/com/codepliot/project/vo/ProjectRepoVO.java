package com.codepliot.project.vo;

import com.codepliot.project.entity.ProjectRepo;
import java.time.LocalDateTime;

public record ProjectRepoVO(
        Long id,
        Long userId,
        String repoUrl,
        String repoName,
        String localPath,
        String defaultBranch,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProjectRepoVO from(ProjectRepo projectRepo) {
        return new ProjectRepoVO(
                projectRepo.getId(),
                projectRepo.getUserId(),
                projectRepo.getRepoUrl(),
                projectRepo.getRepoName(),
                projectRepo.getLocalPath(),
                projectRepo.getDefaultBranch(),
                projectRepo.getStatus(),
                projectRepo.getCreatedAt(),
                projectRepo.getUpdatedAt()
        );
    }
}
