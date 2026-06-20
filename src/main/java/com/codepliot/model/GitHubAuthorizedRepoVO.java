package com.codepliot.model;

/**
 * GitHub 已授权仓库视图对象。
 * <p>用于展示用户在 GitHub 上已授权访问的仓库信息。</p>
 *
 * @param id            仓库的内部标识
 * @param owner         仓库所有者（用户名或组织名）
 * @param name          仓库名称
 * @param fullName      仓库全名（格式：owner/name）
 * @param privateRepo   是否为私有仓库
 * @param defaultBranch 默认分支名称
 * @param htmlUrl       仓库页面的 HTML 链接
 * @param cloneUrl      仓库克隆地址
 */
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
