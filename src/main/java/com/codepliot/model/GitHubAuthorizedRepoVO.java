package com.codepliot.model;

public record GitHubAuthorizedRepoVO(
        Long id,
        String owner,
        String name,
        String fullName,
        boolean privateRepo,
        String defaultBranch,
        String htmlUrl,
        String cloneUrl
) {
}
