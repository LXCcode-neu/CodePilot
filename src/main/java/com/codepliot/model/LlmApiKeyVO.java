package com.codepliot.model;

import com.codepliot.entity.LlmApiKeyConfig;
import java.time.LocalDateTime;

public record LlmApiKeyVO(
        Long id,
        String name,
        String provider,
        String modelName,
        String displayName,
        String baseUrl,
        String apiKeyMask,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt
) {
    public static LlmApiKeyVO from(LlmApiKeyConfig config, String apiKeyMask) {
        return new LlmApiKeyVO(
                config.getId(),
                config.getKeyName(),
                config.getProvider(),
                config.getModelName(),
                config.getDisplayName(),
                config.getBaseUrl(),
                apiKeyMask,
                config.getActive(),
                config.getCreatedAt(),
                config.getLastUsedAt()
        );
    }
}
