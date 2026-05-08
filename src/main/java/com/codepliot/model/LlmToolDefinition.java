package com.codepliot.model;

import com.fasterxml.jackson.databind.JsonNode;

public record LlmToolDefinition(
        String name,
        String description,
        JsonNode parameters
) {
}
