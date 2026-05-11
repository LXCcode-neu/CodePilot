package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GitHubRepoImportRequest(
        @NotNull(message = "githubRepoId cannot be null")
        Long githubRepoId,
        @NotBlank(message = "owner cannot be blank")
        String owner,
        @NotBlank(message = "repoName cannot be blank")
        String repoName
) {
}
