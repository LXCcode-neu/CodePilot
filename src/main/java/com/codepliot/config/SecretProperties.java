package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "codepilot.secret")
public class SecretProperties {

    private String apiKeyEncryptionKey;

    public String getApiKeyEncryptionKey() {
        return apiKeyEncryptionKey;
    }

    public void setApiKeyEncryptionKey(String apiKeyEncryptionKey) {
        this.apiKeyEncryptionKey = apiKeyEncryptionKey;
    }
}
