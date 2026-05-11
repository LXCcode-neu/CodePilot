package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

public record ProjectLlmConfigSaveRequest(
        @NotBlank(message = "provider cannot be blank")
        String provider,
        @NotBlank(message = "modelName cannot be blank")
        String modelName,
        @NotBlank(message = "displayName cannot be blank")
        String displayName,
        @NotBlank(message = "baseUrl cannot be blank")
        String baseUrl,
        String apiKey
) {
}
