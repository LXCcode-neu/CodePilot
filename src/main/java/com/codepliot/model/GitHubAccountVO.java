package com.codepliot.model;

import com.codepliot.entity.UserGitHubAccount;
import java.time.LocalDateTime;

/**
 * GitHub 账户视图对象。
 * <p>用于向前端展示当前用户的 GitHub 账户关联信息。</p>
 *
 * @param connected       是否已关联 GitHub 账户
 * @param githubLogin     GitHub 用户名（登录名）
 * @param githubName      GitHub 显示名称
 * @param githubAvatarUrl GitHub 头像 URL
 * @param scope           OAuth 授权范围
 * @param createdAt       关联创建时间
 * @param updatedAt       关联更新时间
 */
public record GitHubAccountVO(
        boolean connected,
        String githubLogin,
        String githubName,
        String githubAvatarUrl,
        String scope,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 创建一个未关联 GitHub 的账户视图对象。
     *
     * @return 未关联状态的 {@link GitHubAccountVO}
     */
    public static GitHubAccountVO disconnected() {
        return new GitHubAccountVO(false, null, null, null, null, null, null);
    }

    /**
     * 从持久化的 GitHub 账户实体转换为视图对象。
     *
     * @param account GitHub 账户实体
     * @return 包含账户信息的 {@link GitHubAccountVO}
     */
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
