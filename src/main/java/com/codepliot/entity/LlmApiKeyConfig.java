package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 大模型 API 密钥配置实体，对应数据库表 llm_api_key_config。
 * <p>存储用户自定义的 LLM API 密钥信息，支持多个模型提供商的密钥管理。</p>
 */
@Data
@TableName("llm_api_key_config")
@EqualsAndHashCode(callSuper = true)
public class LlmApiKeyConfig extends BaseEntity {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 密钥名称，用户自定义的标识 */
    @TableField("key_name")
    private String keyName;

    /** 模型提供商（如 openai、anthropic、deepseek 等） */
    @TableField("provider")
    private String provider;

    /** 模型名称（如 gpt-4、claude-3 等） */
    @TableField("model_name")
    private String modelName;

    /** 显示名称，用于界面展示 */
    @TableField("display_name")
    private String displayName;

    /** API 基础地址 */
    @TableField("base_url")
    private String baseUrl;

    /** 加密存储的 API 密钥 */
    @TableField("api_key_encrypted")
    private String apiKeyEncrypted;

    /** 是否启用 */
    @TableField("active")
    private Boolean active;

    /** 最后使用时间 */
    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;
}
