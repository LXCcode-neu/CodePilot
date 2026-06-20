package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目级大模型配置实体，对应数据库表 project_llm_config。
 * <p>存储项目维度的 LLM 配置，可覆盖用户级配置，用于指定项目使用的模型和密钥。</p>
 */
@Data
@TableName("project_llm_config")
@EqualsAndHashCode(callSuper = true)
public class ProjectLlmConfig extends BaseEntity {

    /** 关联的项目仓库ID */
    @TableField("project_repo_id")
    private Long projectRepoId;

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
    @TableField("enabled")
    private Boolean enabled;
}
