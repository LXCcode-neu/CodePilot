package com.codepliot.model;

/**
 * Sentry告警上下文信息。
 * <p>封装从Sentry Webhook接收到的告警事件完整上下文，用于触发自动修复流程。
 * 包含告警来源组织、项目、问题详情等关键信息。</p>
 */
public record SentryAlertContext(
        /** Sentry组织标识（slug） */
        String organizationSlug,
        /** Sentry项目标识（slug） */
        String projectSlug,
        /** Sentry问题ID */
        String issueId,
        /** Sentry事件ID */
        String eventId,
        /** 触发告警的规则ID */
        String alertRuleId,
        /** 触发告警的规则名称 */
        String alertRuleName,
        /** 告警标题 */
        String title,
        /** 错误级别（如 error、warning、fatal） */
        String level,
        /** 平台标识（如 java、python、javascript） */
        String platform,
        /** 错误触发位置/函数 */
        String culprit,
        /** Sentry问题页面链接 */
        String webUrl,
        /** 问题指纹（用于去重） */
        String fingerprint,
        /** Sentry推送的原始JSON报文 */
        String rawPayload,
        /** 经过富化处理后的事件数据 */
        String enrichedEvent
) {
}
