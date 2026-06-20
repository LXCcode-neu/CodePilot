package com.codepliot.model;

/**
 * LLM运行时配置。
 * <p>封装实际调用大语言模型API时所需的全部配置信息，包含解密后的真实API密钥。
 * 仅供内部服务使用，不对外暴露。</p>
 */
public record LlmRuntimeConfig(
        /** LLM服务提供商标识 */
        String provider,
        /** 模型名称 */
        String modelName,
        /** 模型显示名称 */
        String displayName,
        /** API基础URL地址 */
        String baseUrl,
        /** API密钥（明文，运行时解密） */
        String apiKey
) {
}
