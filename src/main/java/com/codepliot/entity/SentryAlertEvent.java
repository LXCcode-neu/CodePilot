package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Sentry 告警事件实体，对应数据库表 sentry_alert_event。
 * <p>记录从 Sentry 接收到的告警信息，用于触发自动化的错误分析和修复流程。</p>
 */
@Data
@TableName("sentry_alert_event")
@EqualsAndHashCode(callSuper = true)
public class SentryAlertEvent extends BaseEntity {

    /** Sentry 组织标识（slug） */
    @TableField("sentry_organization_slug")
    private String sentryOrganizationSlug;

    /** Sentry 项目标识（slug） */
    @TableField("sentry_project_slug")
    private String sentryProjectSlug;

    /** Sentry Issue ID */
    @TableField("sentry_issue_id")
    private String sentryIssueId;

    /** Sentry 事件ID */
    @TableField("sentry_event_id")
    private String sentryEventId;

    /** Sentry 告警规则ID */
    @TableField("sentry_alert_rule_id")
    private String sentryAlertRuleId;

    /** Sentry 告警规则名称 */
    @TableField("sentry_alert_rule_name")
    private String sentryAlertRuleName;

    /** 错误指纹，用于去重和归类 */
    @TableField("fingerprint")
    private String fingerprint;

    /** 告警标题 */
    @TableField("title")
    private String title;

    /** 错误级别（error、warning、fatal 等） */
    @TableField("level")
    private String level;

    /** 平台（如 python、javascript、java 等） */
    @TableField("platform")
    private String platform;

    /** 错误触发位置（culprit） */
    @TableField("culprit")
    private String culprit;

    /** Sentry Web 链接地址 */
    @TableField("web_url")
    private String webUrl;

    /** 处理状态 */
    @TableField("status")
    private String status;

    /** 关联的代理任务ID */
    @TableField("agent_task_id")
    private Long agentTaskId;

    /** 去重键，防止重复处理同一告警 */
    @TableField("dedupe_key")
    private String dedupeKey;

    /** Sentry 原始 Webhook 载荷（JSON） */
    @TableField("raw_payload")
    private String rawPayload;

    /** 补充上下文信息（如代码片段、堆栈分析等） */
    @TableField("enriched_context")
    private String enrichedContext;
}
