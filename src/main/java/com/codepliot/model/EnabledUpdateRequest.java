package com.codepliot.model;

import jakarta.validation.constraints.NotNull;

public record EnabledUpdateRequest(
        @NotNull(message = "enabled cannot be null")
        Boolean enabled
) {
}
