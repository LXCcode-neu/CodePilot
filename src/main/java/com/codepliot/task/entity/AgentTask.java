package com.codepliot.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("agent_task")
@EqualsAndHashCode(callSuper = true)
/**
 * Agent 任务实体，对应 agent_task 表。
 */
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
}
