package com.codepliot.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "codepilot.sentry")
public class SentryProperties {

    private boolean enabled = false;
    private String webhookToken = "";
    private String apiBaseUrl = "https://sentry.io/api/0";
    private String authToken = "";
    private String organizationSlug = "";
    private boolean autoRunEnabled = true;
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
