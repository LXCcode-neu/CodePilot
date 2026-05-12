package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("user_repo_watch")
@EqualsAndHashCode(callSuper = true)
public class UserRepoWatch extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("project_repo_id")
    private Long projectRepoId;

    @TableField("owner")
    private String owner;

    @TableField("repo_name")
    private String repoName;

    @TableField("repo_url")
    private String repoUrl;

    @TableField("default_branch")
    private String defaultBranch;

    @TableField("watch_enabled")
    private Boolean watchEnabled;

    @TableField("watch_mode")
    private String watchMode;

    @TableField("last_checked_at")
    private LocalDateTime lastCheckedAt;
}
