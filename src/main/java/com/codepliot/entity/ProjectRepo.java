package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * ProjectRepo 实体类，用于映射数据库表或持久化结构。
 */
@Data
@TableName("project_repo")
@EqualsAndHashCode(callSuper = true)
public class ProjectRepo extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("repo_url")
    private String repoUrl;

    @TableField("repo_name")
    private String repoName;

    @TableField("github_owner")
    private String githubOwner;

    @TableField("github_repo_name")
    private String githubRepoName;

    @TableField("github_repo_id")
    private Long githubRepoId;

    @TableField("github_private")
    private Boolean githubPrivate;

    @TableField("local_path")
    private String localPath;

    @TableField("default_branch")
    private String defaultBranch;

    @TableField("status")
    private String status;
}

