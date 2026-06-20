package com.codepliot.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Sentry 错误监控配置属性。
 * <p>
 * 绑定 {@code codepilot.sentry} 前缀下的配置项，提供 Sentry 平台的连接参数、
 * Webhook 验证、项目映射及自动修复等配置。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "codepilot.sentry")
public class SentryProperties {

    /** 是否启用 Sentry 集成，默认关闭 */
    private boolean enabled = false;
    /** Sentry Webhook 验证 Token */
    private String webhookToken = "";
    /** Sentry API 基础地址 */
    private String apiBaseUrl = "https://sentry.io/api/0";
    /** Sentry API 认证令牌 */
    private String authToken = "";
    /** Sentry 组织标识（slug） */
    private String organizationSlug = "";
    /** 收到 Sentry 告警后是否自动触发修复流程，默认开启 */
    private boolean autoRunEnabled = true;
    /** Sentry 项目名称到本地项目仓库 ID 的映射关系 */
    private Map<String, Long> projectMappings = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookToken() {
        return webhookToken;
    }

    public void setWebhookToken(String webhookToken) {
        this.webhookToken = webhookToken;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getOrganizationSlug() {
        return organizationSlug;
    }

    public void setOrganizationSlug(String organizationSlug) {
        this.organizationSlug = organizationSlug;
    }

    public boolean isAutoRunEnabled() {
        return autoRunEnabled;
    }

    public void setAutoRunEnabled(boolean autoRunEnabled) {
        this.autoRunEnabled = autoRunEnabled;
    }

    public Map<String, Long> getProjectMappings() {
        return projectMappings;
    }

    public void setProjectMappings(Map<String, Long> projectMappings) {
        this.projectMappings = projectMappings == null ? new HashMap<>() : projectMappings;
    }
}
