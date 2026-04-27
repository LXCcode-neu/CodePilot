package com.codepliot.client;
/**
 * LlmClient 客户端实现，负责封装外部系统调用。
 */
public interface LlmClient {
String generate(String systemPrompt, String userPrompt);
}

