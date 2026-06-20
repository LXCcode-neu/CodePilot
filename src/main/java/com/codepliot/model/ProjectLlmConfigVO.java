package com.codepliot.model;

import com.codepliot.entity.ProjectLlmConfig;

/**
 * 项目级LLM配置视图对象（VO）。
 * <p>用于向前端展示项目关联的大语言模型配置信息，API密钥经过脱敏处理。</p>
 */
public record ProjectLlmConfigVO(
        /** 关联的项目仓库ID */
        Long projectRepoId,
        /** LLM服务提供商标识 */
        String provider,
        /** 模型名称 */
        String modelName,
        /** 模型显示名称 */
        String displayName,
        /** API基础URL地址 */
        String baseUrl,
        /** 是否已配置API密钥 */
        Boolean hasApiKey,
        /** API密钥脱敏后的掩码字符串 */
        String apiKeyMask,
        /** 是否启用该配置 */
        Boolean enabled
) {
    /**
     * 从实体对象转换为视图对象。
     *
     * @param config      项目LLM配置实体
     * @param apiKeyMask  脱敏后的API密钥掩码
     * @return 视图对象，若输入为null则返回null
     */
    public static ProjectLlmConfigVO from(ProjectLlmConfig config, String apiKeyMask) {
        if (config == null) {
            return null;
        }
        return new ProjectLlmConfigVO(
                config.getProjectRepoId(),
                config.getProvider(),
                config.getModelName(),
                config.getDisplayName(),
                config.getBaseUrl(),
                config.getApiKeyEncrypted() != null && !config.getApiKeyEncrypted().isBlank(),
                apiKeyMask,
                config.getEnabled()
        );
    }
}
