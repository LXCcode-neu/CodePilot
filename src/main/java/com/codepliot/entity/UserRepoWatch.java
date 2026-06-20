package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户仓库订阅实体，对应数据库表 user_repo_watch。
 * <p>记录用户订阅监控的 GitHub 仓库信息，用于监听 Issue 事件并触发自动化处理。</p>
 */
@Data
@TableName("user_repo_watch")
@EqualsAndHashCode(callSuper = true)
public class UserRepoWatch extends BaseEntity {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 关联的项目仓库ID */
    @TableField("project_repo_id")
    private Long projectRepoId;

    /** 仓库所有者（GitHub 用户名或组织名） */
    @TableField("owner")
    private String owner;

    /** 仓库名称 */
    @TableField("repo_name")
    private String repoName;

    /** 仓库 GitHub 地址 */
    @TableField("repo_url")
    private String repoUrl;

    /** 默认分支名称（如 main、master） */
    @TableField("default_branch")
    private String defaultBranch;

    /** 是否启用监听 */
    @TableField("watch_enabled")
    private Boolean watchEnabled;

    /** 监听模式（如 all、mention 等） */
    @TableField("watch_mode")
    private String watchMode;

    /** 最后一次检查时间 */
    @TableField("last_checked_at")
    private LocalDateTime lastCheckedAt;
}
