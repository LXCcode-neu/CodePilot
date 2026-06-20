package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 通知渠道创建请求对象。
 * <p>用于接收前端提交的新增通知渠道（如飞书、企业微信）的请求参数。</p>
 */
public record NotificationChannelCreateRequest(
        /** 渠道类型（如 FEISHU、WE_COM） */
        @NotBlank(message = "channelType cannot be blank")
        String channelType,
        /** 渠道名称（可选，用于自定义标识） */
        String channelName,
        /** Webhook回调URL地址 */
        @NotBlank(message = "webhookUrl cannot be blank")
        String webhookUrl
) {
}
