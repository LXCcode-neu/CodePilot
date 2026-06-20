package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * GitHub 仓库导入请求。
 * <p>用于将用户在 GitHub 上已授权的仓库导入到系统中进行管理。</p>
 *
 * @param githubRepoId GitHub 仓库的远程标识 ID
 * @param owner        仓库所有者（用户名或组织名）
 * @param repoName     仓库名称
 */
public record GitHubRepoImportRequest(
        @NotNull(message = "githubRepoId cannot be null")
        Long githubRepoId,
        @NotBlank(message = "owner cannot be blank")
        String owner,
        @NotBlank(message = "repoName cannot be blank")
        String repoName
) {
}
