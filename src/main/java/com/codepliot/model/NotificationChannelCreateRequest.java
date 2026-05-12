package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

public record NotificationChannelCreateRequest(
        @NotBlank(message = "channelType cannot be blank")
        String channelType,
        String channelName,
        @NotBlank(message = "webhookUrl cannot be blank")
        String webhookUrl
) {
}
