package com.codepliot.service;

import com.codepliot.client.LlmClient;
import org.springframework.stereotype.Service;
/**
 * LlmService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class LlmService {

    private final LlmClient llmClient;
/**
 * 创建 LlmService 实例。
 */
public LlmService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }
/**
 * 执行 generate 相关逻辑。
 */
public String generate(String systemPrompt, String userPrompt) {
        return llmClient.generate(systemPrompt, userPrompt);
    }
}

