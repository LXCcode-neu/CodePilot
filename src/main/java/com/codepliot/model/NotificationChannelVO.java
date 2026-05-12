package com.codepliot.model;

import com.codepliot.entity.NotificationChannel;
import java.time.LocalDateTime;

public record NotificationChannelVO(
        Long id,
        String channelType,
        String channelName,
        String webhookMasked,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NotificationChannelVO from(NotificationChannel channel, String webhookMasked) {
        return new NotificationChannelVO(
                channel.getId(),
                channel.getChannelType(),
                channel.getChannelName(),
                webhookMasked,
                channel.getEnabled(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}
