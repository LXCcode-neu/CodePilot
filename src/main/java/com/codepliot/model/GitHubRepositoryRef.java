package com.codepliot.model;

public record GitHubRepositoryRef(
        String owner,
        String repo,
        String cloneUrl
) {
}
