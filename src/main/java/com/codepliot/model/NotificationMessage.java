package com.codepliot.model;

import java.util.List;

/**
 * 通知消息对象。
 * <p>封装一条完整的通知消息，包含标题、内容、事件类型、跳转链接和可选的操作按钮。</p>
 */
public record NotificationMessage(
        /** 消息标题 */
        String title,
        /** 消息正文内容 */
        String content,
        /** 触发通知的事件类型 */
        NotificationEventType eventType,
        /** 消息跳转链接 */
        String linkUrl,
        /** 附带的操作按钮列表 */
        List<NotificationAction> actions
) {
    /**
     * 不带操作按钮的便捷构造器。
     */
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
