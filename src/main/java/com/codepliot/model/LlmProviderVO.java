package com.codepliot.model;

import java.util.List;

/**
 * LLM服务提供商视图对象。
 * <p>描述一个大语言模型服务提供商的完整信息，包括其支持的默认模型列表。</p>
 */
public record LlmProviderVO(
        /** 服务提供商唯一标识 */
        String provider,
        /** 服务提供商显示名称 */
        String displayName,
        /** 默认API基础URL地址 */
        String defaultBaseUrl,
        /** 该提供商下的默认可用模型列表 */
        List<LlmAvailableModelVO> defaultModels,
        /** 该提供商是否支持工具调用（Function Calling） */
        Boolean supportsTools
) {
}
