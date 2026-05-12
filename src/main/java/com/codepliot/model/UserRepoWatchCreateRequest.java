package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

public record UserRepoWatchCreateRequest(
        @NotBlank(message = "owner cannot be blank")
        String owner,
        @NotBlank(message = "repoName cannot be blank")
        String repoName,
        @NotBlank(message = "repoUrl cannot be blank")
        String repoUrl,
        String defaultBranch
) {
}
