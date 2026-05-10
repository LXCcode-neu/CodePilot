package com.codepliot.model;

import java.util.List;

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
