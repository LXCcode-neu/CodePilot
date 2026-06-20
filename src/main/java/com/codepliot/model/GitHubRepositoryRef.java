package com.codepliot.model;

/**
 * GitHub 仓库引用记录。
 * <p>用于标识一个 GitHub 仓库的基本引用信息，便于定位和克隆。</p>
 *
 * @param owner    仓库所有者（用户名或组织名）
 * @param repo     仓库名称
 * @param cloneUrl 仓库克隆地址
 */
public record GitHubRepositoryRef(
        String owner,
        String repo,
        String cloneUrl
) {
}
