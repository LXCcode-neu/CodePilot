package com.codepliot.model;

public record NotificationActionExecutionResult(
        boolean success,
        String title,
        String message,
        String linkUrl
) {
}
