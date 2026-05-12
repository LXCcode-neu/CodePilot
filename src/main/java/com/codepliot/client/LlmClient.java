package com.codepliot.client;

import com.codepliot.model.LlmMessage;
import com.codepliot.model.LlmRuntimeConfig;
import com.codepliot.model.LlmToolChatResponse;
import com.codepliot.model.LlmToolDefinition;
import java.util.List;

public interface LlmClient {

    default String provider() {
        return "mock";
    }

    default List<String> supportedProviders() {
        return List.of(provider());
    }

    default String generate(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("generate is not implemented");
    }

    default String generate(LlmRuntimeConfig config, String systemPrompt, String userPrompt) {
        return generate(systemPrompt, userPrompt);
    }

    default LlmToolChatResponse chatWithTools(List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        return new LlmToolChatResponse("", "stop", List.of());
    }

    default LlmToolChatResponse chatWithTools(LlmRuntimeConfig config, List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        return chatWithTools(messages, tools);
    }
}
