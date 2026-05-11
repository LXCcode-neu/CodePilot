package com.codepliot.model;

public record LlmRuntimeConfig(
        String provider,
        String modelName,
        String displayName,
        String baseUrl,
        String apiKey
) {
}
