package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 密钥配置属性。
 * <p>
 * 绑定 {@code codepilot.secret} 前缀下的配置项，提供 API Key 等敏感信息的加密密钥。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "codepilot.secret")
public class SecretProperties {

    /** API Key 加密密钥，用于对存储的敏感信息进行加密和解密 */
    private String apiKeyEncryptionKey;

    public String getApiKeyEncryptionKey() {
        return apiKeyEncryptionKey;
    }

    public void setApiKeyEncryptionKey(String apiKeyEncryptionKey) {
        this.apiKeyEncryptionKey = apiKeyEncryptionKey;
    }
}
