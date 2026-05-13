package com.codepliot.model;

import java.util.List;

public record NotificationMessage(
        String title,
        String content,
        NotificationEventType eventType,
        String linkUrl,
        List<NotificationAction> actions
) {
    public NotificationMessage(String title,
                               String content,
                               NotificationEventType eventType,
                               String linkUrl) {
        this(title, content, eventType, linkUrl, List.of());
    }

    public NotificationMessage {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
