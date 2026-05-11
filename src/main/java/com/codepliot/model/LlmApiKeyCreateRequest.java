package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

public record LlmApiKeyCreateRequest(
        @NotBlank(message = "name cannot be blank")
        String name,
        @NotBlank(message = "provider cannot be blank")
        String provider,
        @NotBlank(message = "modelName cannot be blank")
        String modelName,
        @NotBlank(message = "displayName cannot be blank")
        String displayName,
        @NotBlank(message = "baseUrl cannot be blank")
        String baseUrl,
        @NotBlank(message = "apiKey cannot be blank")
        String apiKey
) {
}
