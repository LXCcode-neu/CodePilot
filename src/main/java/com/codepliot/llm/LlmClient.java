package com.codepliot.llm;

/**
 * LLM 客户端抽象。
 * 屏蔽不同模型供应方的调用细节，对上层统一暴露文本生成能力。
 */
public interface LlmClient {

    /**
     * 基于 system prompt 和 user prompt 生成文本结果。
     */
    String generate(String systemPrompt, String userPrompt);
}
