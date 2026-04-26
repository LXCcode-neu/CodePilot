package com.codepliot.llm;

import com.codepliot.llm.config.LlmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 本地开发用 Mock LLM 客户端。
 * 不调用真实模型，主要用于联调分析与 patch 生成流程。
 */
@Component
@ConditionalOnProperty(prefix = "codepilot.llm", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    private final LlmProperties llmProperties;

    public MockLlmClient(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        String normalizedPrompt = userPrompt == null ? "" : userPrompt.toLowerCase();
        // Patch 生成场景统一返回符合约束的 JSON，方便本地验证解析和落库流程。
        if (normalizedPrompt.contains("\"patch\"") && normalizedPrompt.contains("\"risk\"")) {
            return """
                    {
                      "analysis": "这是基于当前 Issue 和检索代码片段生成的 Mock 分析结果，仅用于联调流程。",
                      "solution": "Mock 客户端不会生成可信 patch，请结合分析结果由开发者手动修改代码。",
                      "patch": "",
                      "risk": "当前启用了 codepilot.llm.provider=%s，因此这里不包含真实模型推理结果。"
                    }
                    """.formatted(llmProperties.getProvider());
        }

        return """
                Mock 分析结果：
                1. 当前结果由 MockLlmClient 生成，仅用于联调分析链路。
                2. 结论只基于提供的 Issue 信息和检索到的代码片段。
                3. 跨语言文件之间的关系仍需人工进一步确认。
                4. 如果证据不足，应继续补充更多代码上下文后再做真实分析。
                """;
    }
}
