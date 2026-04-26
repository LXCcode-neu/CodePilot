package com.codepliot.git.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 工作区配置。
 * 负责读取本地仓库根目录，例如 data/workspace。
 */
@Component
@Validated
@ConfigurationProperties(prefix = "codepilot.workspace")
public class WorkspaceProperties {

    @NotBlank
    private String root;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }
}
