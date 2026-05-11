package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("project_llm_config")
@EqualsAndHashCode(callSuper = true)
public class ProjectLlmConfig extends BaseEntity {

    @TableField("project_repo_id")
    private Long projectRepoId;

    @TableField("provider")
    private String provider;

    @TableField("model_name")
    private String modelName;

    @TableField("display_name")
    private String displayName;

    @TableField("base_url")
    private String baseUrl;

    @TableField("api_key_encrypted")
    private String apiKeyEncrypted;

    @TableField("enabled")
    private Boolean enabled;
}
