package com.codepliot.client;

import com.codepliot.config.LlmProperties;
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

        // Patch prompt: return strict JSON so downstream parsing can proceed.
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
}
