package com.codepliot.model;

public record NotificationAction(
        String label,
        NotificationActionType actionType,
        String url
) {
}
