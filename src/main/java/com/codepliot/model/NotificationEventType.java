package com.codepliot.model;

/**
 * 通知事件类型枚举。
 * <p>定义触发消息通知的业务事件类型。</p>
 */
public enum NotificationEventType {
    /** 新增Sentry告警/问题 */
    NEW_ISSUE,
    /** 自动修复任务已开始 */
    REPAIR_STARTED,
    /** 补丁已生成，等待审核 */
    PATCH_READY,
    /** 自动修复失败 */
    REPAIR_FAILED,
    /** Pull Request已创建 */
    PR_CREATED,
    /** 测试通知 */
    TEST
}
