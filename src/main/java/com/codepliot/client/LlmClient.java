package com.codepliot.client;

import com.codepliot.model.LlmMessage;
import com.codepliot.model.LlmRuntimeConfig;
import com.codepliot.model.LlmToolChatResponse;
import com.codepliot.model.LlmToolDefinition;
import java.util.List;

/**
 * 大语言模型（LLM）客户端接口。
 * <p>
 * 定义了与 LLM 交互的标准契约，包括文本生成和工具调用功能。
 * 各个 LLM 提供商（如 DeepSeek、OpenAI 等）实现此接口以接入系统。
 * </p>
 */
public interface LlmClient {

    /**
     * 返回当前客户端对应的 LLM 提供商名称。
     *
     * @return 提供商标识，默认为 "mock"
     */
    default String provider() {
        return "mock";
    }

    /**
     * 返回当前客户端支持的所有提供商名称列表。
     *
     * @return 支持的提供商名称列表
     */
    default List<String> supportedProviders() {
        return List.of(provider());
    }

    /**
     * 使用默认配置生成文本响应。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 生成的文本内容
     */
    default String generate(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("generate is not implemented");
    }

    /**
     * 使用指定配置生成文本响应。
     *
     * @param config       运行时配置（模型名称、API密钥、基础URL等）
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 生成的文本内容
     */
    default String generate(LlmRuntimeConfig config, String systemPrompt, String userPrompt) {
        return generate(systemPrompt, userPrompt);
    }

    /**
     * 使用默认配置进行带工具调用的对话。
     *
     * @param messages 对话消息列表
     * @param tools    可用工具定义列表
     * @return 包含响应内容、完成原因和工具调用的响应对象
     */
    default LlmToolChatResponse chatWithTools(List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        return new LlmToolChatResponse("", "stop", List.of());
    }

    /**
     * 使用指定配置进行带工具调用的对话。
     *
     * @param config   运行时配置
     * @param messages 对话消息列表
     * @param tools    可用工具定义列表
     * @return 包含响应内容、完成原因和工具调用的响应对象
     */
    default LlmToolChatResponse chatWithTools(LlmRuntimeConfig config, List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        return chatWithTools(messages, tools);
    }
}
