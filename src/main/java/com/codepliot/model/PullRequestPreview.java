package com.codepliot.model;

import java.util.List;

public record PullRequestPreview(
        String title,
        String branchName,
        String commitMessage,
        String body,
        Integer changedFiles,
        Integer addedLines,
        Integer removedLines,
        List<String> touchedFiles,
        Boolean ready,
        String status
) {
}
