package com.codepliot.trace.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent Step 实体，对应 agent_step 表。
 */
@Data
@TableName("agent_step")
@EqualsAndHashCode(callSuper = true)
public class AgentStep extends BaseEntity {

    @TableField("task_id")
    private Long taskId;

    @TableField("step_type")
    private String stepType;

    @TableField("step_name")
    private String stepName;

    @TableField("input")
    private String input;

    @TableField("output")
    private String output;

    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;
}
