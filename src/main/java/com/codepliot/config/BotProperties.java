package com.codepliot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "codepilot.bot")
public class BotProperties {

    private final Feishu feishu = new Feishu();

    public Feishu getFeishu() {
        return feishu;
    }

    public static class Feishu {
        private boolean enabled = false;
        private String baseUrl = "https://open.feishu.cn";
        private String appId = "";
        private String appSecret = "";
        private String verificationToken = "";
        private String encryptKey = "";
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
