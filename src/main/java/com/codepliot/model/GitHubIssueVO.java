package com.codepliot.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * GitHub Issue 视图对象。
 * <p>用于展示单个 GitHub Issue 的详细信息。</p>
 *
 * @param id          Issue 的内部标识
 * @param number      Issue 在仓库中的编号
 * @param title       Issue 标题
 * @param body        Issue 正文内容
 * @param state       Issue 状态（如 open、closed）
 * @param htmlUrl     Issue 页面的 HTML 链接
 * @param authorLogin Issue 创建者的 GitHub 用户名
 * @param labels      Issue 关联的标签列表
 * @param comments    评论数量
 * @param createdAt   创建时间
 * @param updatedAt   更新时间
 */
public record GitHubIssueVO(
        Long id,
        Integer number,
        String title,
        String body,
        String state,
        String htmlUrl,
        String authorLogin,
        List<String> labels,
        Integer comments,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
