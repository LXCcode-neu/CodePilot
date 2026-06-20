package com.codepliot.model;

import com.codepliot.entity.LlmApiKeyConfig;
import java.time.LocalDateTime;

/**
 * LLM API密钥配置的视图对象（VO）。
 * <p>用于向前端展示API密钥配置信息，其中API密钥经过脱敏处理。</p>
 */
public record LlmApiKeyVO(
        /** 配置记录ID */
        Long id,
        /** 密钥配置名称 */
        String name,
        /** LLM服务提供商标识 */
        String provider,
        /** 模型名称 */
        String modelName,
        /** 模型显示名称 */
        String displayName,
        /** API基础URL地址 */
        String baseUrl,
        /** API密钥脱敏后的掩码字符串 */
        String apiKeyMask,
        /** 是否启用 */
        Boolean active,
        /** 创建时间 */
        LocalDateTime createdAt,
        /** 最后使用时间 */
        LocalDateTime lastUsedAt
) {
    /**
     * 从实体对象转换为视图对象。
     *
     * @param config      API密钥配置实体
     * @param apiKeyMask  脱敏后的API密钥掩码
     * @return 视图对象
     */
    public static LlmApiKeyVO from(LlmApiKeyConfig config, String apiKeyMask) {
        return new LlmApiKeyVO(
                config.getId(),
                config.getKeyName(),
                config.getProvider(),
                config.getModelName(),
                config.getDisplayName(),
                config.getBaseUrl(),
                apiKeyMask,
                config.getActive(),
                config.getCreatedAt(),
                config.getLastUsedAt()
        );
    }
}
