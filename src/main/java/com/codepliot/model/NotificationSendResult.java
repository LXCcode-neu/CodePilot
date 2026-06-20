package com.codepliot.model;

/**
 * 通知发送结果。
 * <p>封装通知消息发送到指定渠道后的执行结果。</p>
 */
public record NotificationSendResult(
        /** 是否发送成功 */
        boolean success,
        /** 发送结果描述信息 */
        String message
) {
}
