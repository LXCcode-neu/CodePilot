package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Sentry 项目映射实体，对应数据库表 sentry_project_mapping。
 * <p>建立 CodePilot 项目与 Sentry 项目之间的关联关系，控制告警自动处理的行为。</p>
 */
@Data
@TableName("sentry_project_mapping")
@EqualsAndHashCode(callSuper = true)
public class SentryProjectMapping extends BaseEntity {

    /** 关联的 CodePilot 项目ID */
    @TableField("project_id")
    private Long projectId;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** Sentry 组织标识（slug） */
    @TableField("sentry_organization_slug")
    private String sentryOrganizationSlug;

    /** Sentry 项目标识（slug） */
    @TableField("sentry_project_slug")
    private String sentryProjectSlug;

    /** 是否启用该映射 */
    @TableField("enabled")
    private Boolean enabled;

    /** 是否启用自动运行（收到告警后自动触发修复流程） */
    @TableField("auto_run_enabled")
    private Boolean autoRunEnabled;
}
