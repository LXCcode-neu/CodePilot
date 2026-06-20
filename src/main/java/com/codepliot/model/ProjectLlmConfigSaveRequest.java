package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 项目级LLM配置保存请求对象。
 * <p>用于接收前端提交的项目级大语言模型配置参数，允许项目覆盖全局默认模型设置。</p>
 */
public record ProjectLlmConfigSaveRequest(
        /** LLM服务提供商标识 */
        @NotBlank(message = "provider cannot be blank")
        String provider,
        /** 模型名称 */
        @NotBlank(message = "modelName cannot be blank")
        String modelName,
        /** 模型显示名称 */
        @NotBlank(message = "displayName cannot be blank")
        String displayName,
        /** API基础URL地址 */
        @NotBlank(message = "baseUrl cannot be blank")
        String baseUrl,
        /** API密钥（可选，为空时使用全局配置） */
        String apiKey
) {
}
