package com.codepliot.model;

/**
 * Sentry告警任务创建结果。
 * <p>当接收到Sentry告警并成功创建自动修复任务后的返回信息。</p>
 */
public record SentryAlertTaskCreateResult(
        /** 告警事件记录ID */
        Long alertEventId,
        /** 创建的自动修复任务ID */
        Long taskId,
        /** 任务创建状态 */
        String status,
        /** 结果描述信息 */
        String message
) {
}
