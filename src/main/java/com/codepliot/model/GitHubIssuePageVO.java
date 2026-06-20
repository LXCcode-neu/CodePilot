package com.codepliot.model;

import java.util.List;

/**
 * GitHub Issue 分页视图对象。
 * <p>用于封装 Issue 列表的分页查询结果，包含分页元数据。</p>
 *
 * @param items       当前页的 Issue 列表
 * @param page        当前页码（从 1 开始）
 * @param pageSize    每页记录数
 * @param totalCount  总记录数
 * @param totalPages  总页数
 * @param hasPrevious 是否有上一页
 * @param hasNext     是否有下一页
 */
public record GitHubIssuePageVO(
        List<GitHubIssueVO> items,
        Integer page,
        Integer pageSize,
        Integer totalCount,
        Integer totalPages,
        Boolean hasPrevious,
        Boolean hasNext
) {
}
