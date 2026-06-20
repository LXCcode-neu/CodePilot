package com.codepliot.model;

/**
 * 通知操作按钮对象。
 * <p>描述通知消息中附带的一个可交互操作按钮（如"运行修复"、"忽略"等）。</p>
 */
public record NotificationAction(
        /** 按钮显示文本 */
        String label,
        /** 操作类型 */
        NotificationActionType actionType,
        /** 操作跳转链接（可选） */
        String url
) {
}
