package com.codepliot.llm.service;

import com.codepliot.llm.LlmClient;
import org.springframework.stereotype.Service;

/**
 * LLM 调用门面服务。
 */
@Service
public class LlmService {

    private final LlmClient llmClient;

    public LlmService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public String generate(String systemPrompt, String userPrompt) {
        return llmClient.generate(systemPrompt, userPrompt);
    }
}
