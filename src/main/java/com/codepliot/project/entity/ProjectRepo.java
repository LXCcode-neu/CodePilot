package com.codepliot.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    @TableField("local_path")
    private String localPath;

    @TableField("default_branch")
    private String defaultBranch;

    @TableField("status")
    private String status;
}
