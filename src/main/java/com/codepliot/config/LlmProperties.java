package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
/**
 * LlmProperties 配置类，负责注册或绑定应用配置。
 */
@Component
@ConfigurationProperties(prefix = "codepilot.llm")
public class LlmProperties {

    private String provider = "deepseek";
    private String apiKey;
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
/**
 * 获取Provider相关逻辑。
 */
public String getProvider() {
        return provider;
    }
/**
 * 设置Provider相关逻辑。
 */
public void setProvider(String provider) {
        this.provider = provider;
    }
/**
 * 获取Api Key相关逻辑。
 */
public String getApiKey() {
        return apiKey;
    }
/**
 * 设置Api Key相关逻辑。
 */
public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
/**
 * 获取Base Url相关逻辑。
 */
public String getBaseUrl() {
        return baseUrl;
    }
/**
 * 设置Base Url相关逻辑。
 */
public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
/**
 * 获取Model相关逻辑。
 */
public String getModel() {
        return model;
    }
/**
 * 设置Model相关逻辑。
 */
public void setModel(String model) {
        this.model = model;
    }
}

