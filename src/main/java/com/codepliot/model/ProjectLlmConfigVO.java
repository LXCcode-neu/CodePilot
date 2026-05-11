package com.codepliot.model;

import com.codepliot.entity.ProjectLlmConfig;

public record ProjectLlmConfigVO(
        Long projectRepoId,
        String provider,
        String modelName,
        String displayName,
        String baseUrl,
        Boolean hasApiKey,
        String apiKeyMask,
        Boolean enabled
) {
    public static ProjectLlmConfigVO from(ProjectLlmConfig config, String apiKeyMask) {
        if (config == null) {
            return null;
        }
        return new ProjectLlmConfigVO(
                config.getProjectRepoId(),
                config.getProvider(),
                config.getModelName(),
                config.getDisplayName(),
                config.getBaseUrl(),
                config.getApiKeyEncrypted() != null && !config.getApiKeyEncrypted().isBlank(),
                apiKeyMask,
                config.getEnabled()
        );
    }
}
