package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户 GitHub 账号绑定实体，对应数据库表 user_github_account。
 * <p>存储用户通过 OAuth 绑定的 GitHub 账号信息和访问令牌。</p>
 */
@Data
@TableName("user_github_account")
@EqualsAndHashCode(callSuper = true)
public class UserGitHubAccount extends BaseEntity {

    /** CodePilot 用户ID */
    @TableField("user_id")
    private Long userId;

    /** GitHub 用户ID */
    @TableField("github_user_id")
    private Long githubUserId;

    /** GitHub 用户名（登录名） */
    @TableField("github_login")
    private String githubLogin;

    /** GitHub 显示名称 */
    @TableField("github_name")
    private String githubName;

    /** GitHub 头像地址 */
    @TableField("github_avatar_url")
    private String githubAvatarUrl;

    /** 加密存储的 GitHub OAuth 访问令牌 */
    @TableField("access_token_encrypted")
    private String accessTokenEncrypted;

    /** OAuth 授权范围（scope） */
    @TableField("scope")
    private String scope;
}
