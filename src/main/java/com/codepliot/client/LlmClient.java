package com.codepliot.client;

import com.codepliot.model.LlmMessage;
import com.codepliot.model.LlmToolChatResponse;
import com.codepliot.model.LlmToolDefinition;
import java.util.List;

public interface LlmClient {

    String generate(String systemPrompt, String userPrompt);

    default LlmToolChatResponse chatWithTools(List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        return new LlmToolChatResponse("", "stop", List.of());
    }
}
