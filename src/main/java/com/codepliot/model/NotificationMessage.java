package com.codepliot.model;

public record NotificationMessage(
        String title,
        String content,
        NotificationEventType eventType,
        String linkUrl
) {
}
