package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用基础配置属性。
 * <p>
 * 绑定 {@code codepilot.app} 前缀下的配置项，提供应用公共访问地址等基础设置。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "codepilot.app")
public class AppProperties {

    /** 应用公共访问基础 URL，默认为本地开发地址 */
    private String publicBaseUrl = "http://localhost:8080";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * 拼接完整的访问 URL。
     * <p>
     * 将公共基础 URL 与给定路径拼接，自动处理末尾斜杠和路径前缀。
     * </p>
     *
     * @param path 需要拼接的路径
     * @return 完整的 URL 地址
     */
    public String buildUrl(String path) {
        String base = publicBaseUrl == null || publicBaseUrl.isBlank()
                ? "http://localhost:8080"
                : publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (path == null || path.isBlank()) {
            return base;
        }
        return path.startsWith("/") ? base + path : base + "/" + path;
    }
}
