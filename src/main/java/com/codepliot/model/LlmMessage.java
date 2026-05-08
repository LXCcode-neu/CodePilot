package com.codepliot.model;

import java.util.List;

public record LlmMessage(
        String role,
        String content,
        String toolCallId,
        List<LlmToolCall> toolCalls
) {

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, List.of());
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, List.of());
    }

    public static LlmMessage assistant(String content, List<LlmToolCall> toolCalls) {
        return new LlmMessage("assistant", content, null, toolCalls == null ? List.of() : List.copyOf(toolCalls));
    }

    public static LlmMessage tool(String toolCallId, String content) {
        return new LlmMessage("tool", content, toolCallId, List.of());
    }
}
