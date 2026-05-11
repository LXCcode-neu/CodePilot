package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

public record GitHubConnectRequest(
        @NotBlank(message = "code cannot be blank")
        String code,
        @NotBlank(message = "state cannot be blank")
        String state
) {
}
