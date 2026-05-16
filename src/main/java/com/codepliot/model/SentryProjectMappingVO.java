package com.codepliot.model;

import com.codepliot.entity.SentryProjectMapping;
import java.time.LocalDateTime;

public record SentryProjectMappingVO(
        Long id,
        Long projectId,
        String sentryOrganizationSlug,
        String sentryProjectSlug,
        Boolean enabled,
        Boolean autoRunEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
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
