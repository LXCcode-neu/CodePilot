package com.codepliot.client;

import com.codepliot.config.LlmProperties;
import com.codepliot.model.LlmMessage;
import com.codepliot.model.LlmToolCall;
import com.codepliot.model.LlmToolChatResponse;
import com.codepliot.model.LlmToolDefinition;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
/**
 * MockLlmClient 客户端实现，负责封装外部系统调用。
 */
@Component
@ConditionalOnProperty(prefix = "codepilot.llm", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    private final LlmProperties llmProperties;
/**
 * 创建 MockLlmClient 实例。
 */
public MockLlmClient(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
    }
    /**
     * 执行 generate 相关逻辑。
     */
@Override
public String generate(String systemPrompt, String userPrompt) {
        String normalizedPrompt = userPrompt == null ? "" : userPrompt.toLowerCase();

        // Patch 提示词：返回严格 JSON，便于下游继续解析。
        if (normalizedPrompt.contains("\"patch\"") && normalizedPrompt.contains("\"risk\"")) {
            return """
                    {
                      "analysis": "ǻ MockLlmClient ɵķڱ̡",
                      "solution": "ȷϼǷ֣лʵģɿĵ patch",
                      "patch": "",
                      "risk": "ǰʹõ mock providercodepilot.llm.provider=%sʵģͷ"
                    }
                    """.formatted(llmProperties.getProvider());
        }

        return """
                Mock 
                1. ǰ MockLlmClient ɣʾ
                2. Issue 漰ǰĴƬΣﲻʵ
                3. Ҫɿۣлʵģṩ
                4. ʵУ retrievedChunksanalysis  patch ֤
                """;
    }

@Override
public LlmToolChatResponse chatWithTools(List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        boolean hasToolResult = messages != null && messages.stream().anyMatch(message -> "tool".equals(message.role()));
        if (hasToolResult || tools == null || tools.isEmpty()) {
            return new LlmToolChatResponse("", "stop", List.of());
        }
        String toolName = tools.stream()
                .map(LlmToolDefinition::name)
                .filter("grep"::equals)
                .findFirst()
                .orElse(tools.get(0).name());
        return new LlmToolChatResponse("", "tool_calls", List.of(
                new LlmToolCall("mock_tool_call_1", toolName, "{\"query\":\"class\",\"globPatterns\":[\"**/*.java\"],\"maxResults\":5}")
        ));
    }
}
