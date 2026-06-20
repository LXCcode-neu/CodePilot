package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 机器人配置属性。
 * <p>
 * 绑定 {@code codepilot.bot} 前缀下的配置项，管理各类即时通讯机器人的连接参数。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "codepilot.bot")
public class BotProperties {

    /** 飞书机器人配置 */
    private final Feishu feishu = new Feishu();

    public Feishu getFeishu() {
        return feishu;
    }

    /**
     * 飞书机器人配置内部类。
     * <p>
     * 包含飞书开放平台的应用凭证、回调验证及默认群聊等配置。
     * </p>
     */
    public static class Feishu {
        /** 是否启用飞书机器人 */
        private boolean enabled = false;
        /** 飞书开放平台 API 基础地址 */
        private String baseUrl = "https://open.feishu.cn";
        /** 飞书应用 App ID */
        private String appId = "";
        /** 飞书应用 App Secret */
        private String appSecret = "";
        /** 飞书事件回调验证 Token */
        private String verificationToken = "";
        /** 飞书事件回调加密密钥 */
        private String encryptKey = "";
        /** 默认群聊 ID，用于主动推送消息 */
        private String defaultChatId = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getVerificationToken() {
            return verificationToken;
        }

        public void setVerificationToken(String verificationToken) {
            this.verificationToken = verificationToken;
        }

        public String getEncryptKey() {
            return encryptKey;
        }

        public void setEncryptKey(String encryptKey) {
            this.encryptKey = encryptKey;
        }

        public String getDefaultChatId() {
            return defaultChatId;
        }

        public void setDefaultChatId(String defaultChatId) {
            this.defaultChatId = defaultChatId;
        }
    }
}
