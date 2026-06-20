package com.codepliot.service.notification;

import com.codepliot.model.NotificationChannelType;
import com.codepliot.model.NotificationMessage;

/**
 * 通知发送器接口。
 * <p>
 * 定义通知发送的统一抽象，不同渠道（如飞书、钉钉、邮件等）实现此接口。
 */
public interface NotificationSender {

    /**
     * 获取该发送器支持的通知渠道类型。
     *
     * @return 通知渠道类型枚举
     */
    NotificationChannelType type();

    /**
     * 通过指定的 Webhook URL 发送通知消息。
     *
     * @param webhookUrl 目标 Webhook URL
     * @param message    通知消息内容
     */
    void send(String webhookUrl, NotificationMessage message);
}
