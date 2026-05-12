package com.codepliot.model;

public record LlmAvailableModelVO(
        String provider,
        String modelName,
        String displayName,
        String defaultBaseUrl,
        Boolean supportsTools
) {
    public LlmAvailableModelVO(String provider, String modelName, String displayName, String defaultBaseUrl) {
        this(provider, modelName, displayName, defaultBaseUrl, false);
    }
}
