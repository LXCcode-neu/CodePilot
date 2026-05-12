package com.codepliot.model;

public record NotificationSendResult(
        boolean success,
        String message
) {
}
