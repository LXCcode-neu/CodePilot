package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Sentry项目映射保存请求对象。
 * <p>用于接收前端提交的Sentry项目与本地项目的关联映射配置。</p>
 */
public record SentryProjectMappingSaveRequest(
        /** Sentry组织标识（slug） */
        @NotBlank(message = "sentryOrganizationSlug cannot be blank")
        String sentryOrganizationSlug,
        /** Sentry项目标识（slug） */
        @NotBlank(message = "sentryProjectSlug cannot be blank")
        String sentryProjectSlug,
        /** 是否启用该映射 */
        Boolean enabled,
        /** 是否启用收到告警后自动触发修复任务 */
        Boolean autoRunEnabled
) {
}
