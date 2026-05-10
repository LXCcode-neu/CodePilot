package com.codepliot.model;

import java.time.LocalDateTime;

public record PullRequestSubmitResult(
        Long taskId,
        Integer number,
        String url,
        String branch,
        LocalDateTime submittedAt
) {
}
