package com.codepliot.model;

/**
 * 可用LLM模型视图对象。
 * <p>描述系统内置支持的大语言模型信息，用于前端模型选择列表。</p>
 */
public record LlmAvailableModelVO(
        /** LLM服务提供商标识 */
        String provider,
        /** 模型名称 */
        String modelName,
        /** 模型显示名称（用于前端展示） */
        String displayName,
        /** 默认API基础URL地址 */
        String defaultBaseUrl,
        /** 是否支持工具调用（Function Calling） */
        Boolean supportsTools
) {
    /**
     * 兼容不支持工具调用的构造器，默认 supportsTools 为 false。
     */
    public LlmAvailableModelVO(String provider, String modelName, String displayName, String defaultBaseUrl) {
        this(provider, modelName, displayName, defaultBaseUrl, false);
    }
}
