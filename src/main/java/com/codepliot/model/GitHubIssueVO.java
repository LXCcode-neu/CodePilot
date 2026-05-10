package com.codepliot.model;

import java.time.OffsetDateTime;
import java.util.List;

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
