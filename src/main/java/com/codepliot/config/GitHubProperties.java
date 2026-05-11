package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "codepilot.github")
public class GitHubProperties {

    private String baseUrl = "https://github.com";
    private String apiBaseUrl = "https://api.github.com";
    private String token = "";
    private String clientId = "";
    private String clientSecret = "";
    private String oauthRedirectUri = "";
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
