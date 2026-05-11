package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("llm_api_key_config")
@EqualsAndHashCode(callSuper = true)
public class LlmApiKeyConfig extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("key_name")
    private String keyName;

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

    @TableField("active")
    private Boolean active;

    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;
}
