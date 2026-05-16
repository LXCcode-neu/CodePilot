package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sentry_alert_event")
@EqualsAndHashCode(callSuper = true)
public class SentryAlertEvent extends BaseEntity {

    @TableField("sentry_organization_slug")
    private String sentryOrganizationSlug;

    @TableField("sentry_project_slug")
    private String sentryProjectSlug;

    @TableField("sentry_issue_id")
    private String sentryIssueId;

    @TableField("sentry_event_id")
    private String sentryEventId;

    @TableField("sentry_alert_rule_id")
    private String sentryAlertRuleId;

    @TableField("sentry_alert_rule_name")
    private String sentryAlertRuleName;

    @TableField("fingerprint")
    private String fingerprint;

    @TableField("title")
    private String title;

    @TableField("level")
    private String level;

    @TableField("platform")
    private String platform;

    @TableField("culprit")
    private String culprit;

    @TableField("web_url")
    private String webUrl;

    @TableField("status")
    private String status;

    @TableField("agent_task_id")
    private Long agentTaskId;

    @TableField("dedupe_key")
    private String dedupeKey;

    @TableField("raw_payload")
    private String rawPayload;

    @TableField("enriched_context")
    private String enrichedContext;
}
