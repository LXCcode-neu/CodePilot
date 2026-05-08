package com.codepliot.model;

import java.util.List;

public record LlmToolChatResponse(
        String content,
        String finishReason,
        List<LlmToolCall> toolCalls
) {

    public LlmToolChatResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
