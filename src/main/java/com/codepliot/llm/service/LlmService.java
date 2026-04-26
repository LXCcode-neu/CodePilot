package com.codepliot.llm.service;

import com.codepliot.llm.LlmClient;
import org.springframework.stereotype.Service;

/**
 * LLM 调用门面服务。
 * 上层工具只依赖这个服务，不感知具体底层接的是 mock 还是真实模型。
 */
@Service
public class LlmService {

    private final LlmClient llmClient;

    public LlmService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 将 Prompt 转发给当前启用的 LLM 客户端。
     */
    public String generate(String systemPrompt, String userPrompt) {
        return llmClient.generate(systemPrompt, userPrompt);
    }
}
