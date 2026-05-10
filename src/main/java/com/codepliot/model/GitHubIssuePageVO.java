package com.codepliot.model;

import java.util.List;

public record GitHubIssuePageVO(
        List<GitHubIssueVO> items,
        Integer page,
        Integer pageSize,
        Boolean hasNext
) {
}
