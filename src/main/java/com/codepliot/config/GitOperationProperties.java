package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Git 操作相关配置属性。
 * <p>
 * 绑定 {@code codepilot.git} 前缀下的配置项，控制 Git 仓库克隆等操作的超时时间。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "codepilot.git")
public class GitOperationProperties {

    /** 仓库克隆操作超时时间（秒），默认 120 秒 */
    private int cloneTimeoutSeconds = 120;

    public int getCloneTimeoutSeconds() {
        return cloneTimeoutSeconds;
    }

    public void setCloneTimeoutSeconds(int cloneTimeoutSeconds) {
        this.cloneTimeoutSeconds = cloneTimeoutSeconds;
    }
}
