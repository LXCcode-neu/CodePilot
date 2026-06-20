package com.codepliot.model;

/**
 * 机器人接收的入站消息记录。
 * <p>表示从消息通道接收到的用户消息，包含消息来源和内容等信息。</p>
 *
 * @param channelType 消息通道类型（如飞书、钉钉等）
 * @param eventId     事件唯一标识
 * @param messageId   消息唯一标识
 * @param chatId      会话/群组标识
 * @param senderId    发送者标识
 * @param text        消息文本内容
 */
public record BotIncomingMessage(
        NotificationChannelType channelType,
        String eventId,
        String messageId,
        String chatId,
        String senderId,
        String text
) {
}
