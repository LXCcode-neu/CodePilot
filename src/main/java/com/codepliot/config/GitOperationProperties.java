package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "codepilot.git")
public class GitOperationProperties {

    private int cloneTimeoutSeconds = 120;

    public int getCloneTimeoutSeconds() {
        return cloneTimeoutSeconds;
    }

    public void setCloneTimeoutSeconds(int cloneTimeoutSeconds) {
        this.cloneTimeoutSeconds = cloneTimeoutSeconds;
    }
}
