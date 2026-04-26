package com.codepliot.llm;

/**
 * LLM 客户端抽象。
 * 屏蔽不同模型提供方的调用细节，统一暴露文本生成能力。
 */
public interface LlmClient {

    String generate(String systemPrompt, String userPrompt);
}
