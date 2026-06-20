package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GitHub 相关配置属性。
 * <p>
 * 绑定 {@code codepilot.github} 前缀下的配置项，提供 GitHub API 访问地址、
 * 认证令牌及 OAuth 应用凭证等配置。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "codepilot.github")
public class GitHubProperties {

    /** GitHub 站点基础 URL */
    private String baseUrl = "https://github.com";
    /** GitHub API 基础 URL */
    private String apiBaseUrl = "https://api.github.com";
    /** GitHub 个人访问令牌（Personal Access Token） */
    private String token = "";
    /** GitHub OAuth 应用 Client ID */
    private String clientId = "";
    /** GitHub OAuth 应用 Client Secret */
    private String clientSecret = "";
    /** GitHub OAuth 回调地址 */
    private String oauthRedirectUri = "";
    /** GitHub OAuth 授权范围 */
    private String oauthScope = "repo read:user";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getOauthRedirectUri() {
        return oauthRedirectUri;
    }

    public void setOauthRedirectUri(String oauthRedirectUri) {
        this.oauthRedirectUri = oauthRedirectUri;
    }

    public String getOauthScope() {
        return oauthScope;
    }

    public void setOauthScope(String oauthScope) {
        this.oauthScope = oauthScope;
    }
}
