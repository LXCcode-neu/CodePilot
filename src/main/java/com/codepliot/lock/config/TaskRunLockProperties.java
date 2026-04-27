package com.codepliot.lock.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 任务运行锁配置。
 * 用于控制 Redis 任务运行锁的过期时间。
 */
@Component
@Validated
@ConfigurationProperties(prefix = "codepilot.lock.task-run")
public class TaskRunLockProperties {

    @NotNull
    private Duration ttl = Duration.ofMinutes(30);

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
