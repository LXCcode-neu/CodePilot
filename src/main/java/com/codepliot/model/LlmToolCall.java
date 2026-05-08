package com.codepliot.model;

public record LlmToolCall(
        String id,
        String name,
        String arguments
) {
}
