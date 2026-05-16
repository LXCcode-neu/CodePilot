package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sentry_project_mapping")
@EqualsAndHashCode(callSuper = true)
public class SentryProjectMapping extends BaseEntity {

    @TableField("project_id")
    private Long projectId;

    @TableField("user_id")
    private Long userId;

    @TableField("sentry_organization_slug")
    private String sentryOrganizationSlug;

    @TableField("sentry_project_slug")
    private String sentryProjectSlug;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("auto_run_enabled")
    private Boolean autoRunEnabled;
}
