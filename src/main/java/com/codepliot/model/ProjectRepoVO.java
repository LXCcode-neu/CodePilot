package com.codepliot.model;

import com.codepliot.entity.ProjectRepo;
import java.time.LocalDateTime;
/**
 * ProjectRepoVO 模型类，用于承载流程中的数据结构。
 */
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
/**
 * 执行 from 相关逻辑。
 */
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

