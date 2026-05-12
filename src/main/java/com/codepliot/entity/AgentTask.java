package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * AgentTask 实体类，用于映射数据库表或持久化结构。
 */
@Data
@TableName("agent_task")
@EqualsAndHashCode(callSuper = true)
public class AgentTask extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("project_id")
    private Long projectId;

    @TableField("issue_title")
    private String issueTitle;

    @TableField("issue_description")
    private String issueDescription;

    @TableField("status")
    private String status;

    @TableField("result_summary")
    private String resultSummary;

    @TableField("error_message")
    private String errorMessage;

    @TableField("llm_provider")
    private String llmProvider;

    @TableField("llm_model_name")
    private String llmModelName;

    @TableField("llm_display_name")
    private String llmDisplayName;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private Long sourceId;
}

