package com.codepliot.model;

/**
 * GitHub Pull Request 创建结果。
 * <p>表示成功创建 Pull Request 后返回的摘要信息。</p>
 *
 * @param number  Pull Request 编号
 * @param htmlUrl Pull Request 页面的 HTML 链接
 */
public record GitHubPullRequestCreateResult(
        Integer number,
        String htmlUrl
) {
}
