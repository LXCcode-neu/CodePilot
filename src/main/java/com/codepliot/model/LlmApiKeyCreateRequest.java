package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

/**
 * LLM API密钥创建请求对象。
 * <p>用于接收前端提交的新增大语言模型API密钥配置的请求参数。</p>
 */
public record LlmApiKeyCreateRequest(
        /** 密钥配置名称（唯一标识） */
        @NotBlank(message = "name cannot be blank")
        String name,
        /** LLM服务提供商标识（如 openai、deepseek 等） */
        @NotBlank(message = "provider cannot be blank")
        String provider,
        /** 模型名称（如 gpt-4、deepseek-chat 等） */
        @NotBlank(message = "modelName cannot be blank")
        String modelName,
        /** 模型显示名称（用于前端展示） */
        @NotBlank(message = "displayName cannot be blank")
        String displayName,
        /** API基础URL地址 */
        @NotBlank(message = "baseUrl cannot be blank")
        String baseUrl,
        /** API密钥（明文，后端加密存储） */
        @NotBlank(message = "apiKey cannot be blank")
        String apiKey
) {
}
