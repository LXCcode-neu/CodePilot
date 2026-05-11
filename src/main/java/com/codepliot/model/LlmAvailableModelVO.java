package com.codepliot.model;

public record LlmAvailableModelVO(
        String provider,
        String modelName,
        String displayName,
        String defaultBaseUrl
) {
}
