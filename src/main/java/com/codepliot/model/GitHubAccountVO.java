package com.codepliot.model;

import com.codepliot.entity.UserGitHubAccount;
import java.time.LocalDateTime;

public record GitHubAccountVO(
        boolean connected,
        String githubLogin,
        String githubName,
        String githubAvatarUrl,
        String scope,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GitHubAccountVO disconnected() {
        return new GitHubAccountVO(false, null, null, null, null, null, null);
    }

    public static GitHubAccountVO from(UserGitHubAccount account) {
        return new GitHubAccountVO(
                true,
                account.getGithubLogin(),
                account.getGithubName(),
                account.getGithubAvatarUrl(),
                account.getScope(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
