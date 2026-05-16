package com.codepliot.model;

public record PatchReviewFinding(
        String severity,
        String filePath,
        String message
) {
}
