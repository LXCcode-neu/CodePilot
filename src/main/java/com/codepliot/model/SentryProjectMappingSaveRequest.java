package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

public record SentryProjectMappingSaveRequest(
        @NotBlank(message = "sentryOrganizationSlug cannot be blank")
        String sentryOrganizationSlug,
        @NotBlank(message = "sentryProjectSlug cannot be blank")
        String sentryProjectSlug,
        Boolean enabled,
        Boolean autoRunEnabled
) {
}
