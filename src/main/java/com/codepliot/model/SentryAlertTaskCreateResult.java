package com.codepliot.model;

public record SentryAlertTaskCreateResult(
        Long alertEventId,
        Long taskId,
        String status,
        String message
) {
}
