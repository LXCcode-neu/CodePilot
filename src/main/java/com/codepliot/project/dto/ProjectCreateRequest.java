package com.codepliot.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectCreateRequest(
        @NotBlank(message = "repoUrl cannot be blank")
        String repoUrl
) {
}
