package com.codepliot.model;

public record BotIncomingMessage(
        NotificationChannelType channelType,
        String eventId,
        String messageId,
        String chatId,
        String senderId,
        String text
) {
}
