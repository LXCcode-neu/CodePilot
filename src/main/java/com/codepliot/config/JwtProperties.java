package com.codepliot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * JwtProperties 配置类，负责注册或绑定应用配置。
 */
@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret;

    private Long expireSeconds = 86400L;
}

