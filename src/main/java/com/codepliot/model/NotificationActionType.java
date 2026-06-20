package com.codepliot.model;

/**
 * 通知操作类型枚举。
 * <p>定义通知消息中可执行的操作类型。</p>
 */
public enum NotificationActionType {
    /** 运行自动修复 */
    RUN_FIX,
    /** 忽略该告警 */
    IGNORE
}
