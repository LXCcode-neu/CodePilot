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

@Component
public class FeishuBotClient {

    private final BotProperties botProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private String cachedTenantAccessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public FeishuBotClient(BotProperties botProperties,
                           ObjectMapper objectMapper,
                           RestClient.Builder restClientBuilder) {
        this.botProperties = botProperties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public boolean isEnabled() {
        BotProperties.Feishu feishu = botProperties.getFeishu();
        return feishu.isEnabled()
                && hasText(feishu.getAppId())
                && hasText(feishu.getAppSecret());
    }

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
