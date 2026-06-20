package com.codepliot.model;

import com.codepliot.entity.UserRepoWatch;
import java.time.LocalDateTime;

/**
 * 用户仓库监控视图对象（VO）。
 * <p>用于向前端展示用户配置的仓库监控信息，包括监控状态、模式和最后检查时间等。</p>
 */
public record UserRepoWatchVO(
        /** 监控记录ID */
        Long id,
        /** 关联的项目仓库ID */
        Long projectRepoId,
        /** 仓库所有者 */
        String owner,
        /** 仓库名称 */
        String repoName,
        /** 仓库URL地址 */
        String repoUrl,
        /** 默认监控分支 */
        String defaultBranch,
        /** 是否启用监控 */
        Boolean watchEnabled,
        /** 监控模式（如 auto、manual） */
        String watchMode,
        /** 最后检查时间 */
        LocalDateTime lastCheckedAt,
        /** 创建时间 */
        LocalDateTime createdAt,
        /** 更新时间 */
        LocalDateTime updatedAt
) {
    /**
     * 从实体对象转换为视图对象。
     *
     * @param watch 用户仓库监控实体
     * @return 视图对象
     */
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
