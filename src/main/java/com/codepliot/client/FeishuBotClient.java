package com.codepliot.client;

import com.codepliot.config.BotProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 飞书机器人客户端。
 * <p>
 * 封装飞书开放平台 API，支持向群聊发送文本消息。
 * 自动管理 tenant_access_token 的获取和缓存。
 * </p>
 */
@Component
public class FeishuBotClient {

    /** 机器人配置属性，包含飞书应用的 AppID、AppSecret 等 */
    private final BotProperties botProperties;

    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper;

    /** REST 客户端，用于发送 HTTP 请求 */
    private final RestClient restClient;

    /** 缓存的租户访问令牌 */
    private String cachedTenantAccessToken;

    /** 令牌过期时间 */
    private Instant tokenExpiresAt = Instant.EPOCH;

    /**
     * 构造方法，注入配置属性和 REST 客户端构建器。
     *
     * @param botProperties    机器人配置属性
     * @param objectMapper     JSON 对象映射器
     * @param restClientBuilder REST 客户端构建器
     */
    public FeishuBotClient(BotProperties botProperties,
                           ObjectMapper objectMapper,
                           RestClient.Builder restClientBuilder) {
        this.botProperties = botProperties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    /**
     * 检查飞书机器人是否已启用且配置完整。
     *
     * @return 如果已启用且 AppID 和 AppSecret 均已配置则返回 true
     */
    public boolean isEnabled() {
        BotProperties.Feishu feishu = botProperties.getFeishu();
        return feishu.isEnabled()
                && hasText(feishu.getAppId())
                && hasText(feishu.getAppSecret());
    }

    /**
     * 向指定飞书群聊发送文本消息。
     *
     * @param chatId 群聊 ID
     * @param text   消息文本内容
     */
    public void sendTextToChat(String chatId, String text) {
        if (!isEnabled() || !hasText(chatId)) {
            return;
        }
        try {
            restClient.post()
                    .uri(normalizedBaseUrl() + "/open-apis/im/v1/messages?receive_id_type=chat_id")
                    .header("Authorization", "Bearer " + tenantAccessToken())
                    .body(Map.of(
                            "receive_id", chatId,
                            "msg_type", "text",
                            "content", objectMapper.writeValueAsString(Map.of("text", text == null ? "" : text))
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to build Feishu message content");
        }
    }

    /**
     * 获取租户访问令牌，支持自动缓存和刷新。
     * <p>
     * 令牌在过期前60秒会自动刷新，使用 synchronized 保证线程安全。
     * </p>
     *
     * @return 租户访问令牌
     */
    private synchronized String tenantAccessToken() {
        if (hasText(cachedTenantAccessToken) && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedTenantAccessToken;
        }
        JsonNode response = restClient.post()
                .uri(normalizedBaseUrl() + "/open-apis/auth/v3/tenant_access_token/internal")
                .body(Map.of(
                        "app_id", botProperties.getFeishu().getAppId(),
                        "app_secret", botProperties.getFeishu().getAppSecret()
                ))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || response.path("code").asInt(-1) != 0 || !hasText(response.path("tenant_access_token").asText())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to get Feishu tenant_access_token");
        }
        cachedTenantAccessToken = response.path("tenant_access_token").asText();
        int expireSeconds = Math.max(response.path("expire").asInt(7200), 120);
        tokenExpiresAt = Instant.now().plusSeconds(expireSeconds);
        return cachedTenantAccessToken;
    }

    private String normalizedBaseUrl() {
        String baseUrl = botProperties.getFeishu().getBaseUrl();
        if (!hasText(baseUrl)) {
            return "https://open.feishu.cn";
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
