package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("user_github_account")
@EqualsAndHashCode(callSuper = true)
public class UserGitHubAccount extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("github_user_id")
    private Long githubUserId;

    @TableField("github_login")
    private String githubLogin;

    @TableField("github_name")
    private String githubName;

    @TableField("github_avatar_url")
    private String githubAvatarUrl;

    @TableField("access_token_encrypted")
    private String accessTokenEncrypted;

    @TableField("scope")
    private String scope;
}
