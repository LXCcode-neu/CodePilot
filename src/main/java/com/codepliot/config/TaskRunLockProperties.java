package com.codepliot.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
/**
 * TaskRunLockProperties 配置类，负责注册或绑定应用配置。
 */
@Component
@Validated
@ConfigurationProperties(prefix = "codepilot.lock.task-run")
public class TaskRunLockProperties {

    @NotNull
    private Duration ttl = Duration.ofMinutes(30);
/**
 * 获取Ttl相关逻辑。
 */
public Duration getTtl() {
        return ttl;
    }
/**
 * 设置Ttl相关逻辑。
 */
public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}

