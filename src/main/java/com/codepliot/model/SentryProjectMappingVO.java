package com.codepliot.model;

import com.codepliot.entity.SentryProjectMapping;
import java.time.LocalDateTime;

/**
 * Sentry项目映射视图对象（VO）。
 * <p>用于向前端展示Sentry项目与本地项目的关联映射配置信息。</p>
 */
public record SentryProjectMappingVO(
        /** 映射记录ID */
        Long id,
        /** 关联的本地项目ID */
        Long projectId,
        /** Sentry组织标识（slug） */
        String sentryOrganizationSlug,
        /** Sentry项目标识（slug） */
        String sentryProjectSlug,
        /** 是否启用该映射 */
        Boolean enabled,
        /** 是否启用自动触发修复 */
        Boolean autoRunEnabled,
        /** 创建时间 */
        LocalDateTime createdAt,
        /** 更新时间 */
        LocalDateTime updatedAt
) {
    /**
     * 从实体对象转换为视图对象。
     *
     * @param mapping Sentry项目映射实体
     * @return 视图对象，若输入为null则返回null
     */
    public static SentryProjectMappingVO from(SentryProjectMapping mapping) {
        if (mapping == null) {
            return null;
        }
        return new SentryProjectMappingVO(
                mapping.getId(),
                mapping.getProjectId(),
                mapping.getSentryOrganizationSlug(),
                mapping.getSentryProjectSlug(),
                mapping.getEnabled(),
                mapping.getAutoRunEnabled(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt()
        );
    }
}
