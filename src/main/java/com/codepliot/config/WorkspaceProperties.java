package com.codepliot.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
/**
 * WorkspaceProperties 配置类，负责注册或绑定应用配置。
 */
@Component
@Validated
@ConfigurationProperties(prefix = "codepilot.workspace")
public class WorkspaceProperties {

    @NotBlank
    private String root;
/**
 * 获取Root相关逻辑。
 */
public String getRoot() {
        return root;
    }
/**
 * 设置Root相关逻辑。
 */
public void setRoot(String root) {
        this.root = root;
    }
}

