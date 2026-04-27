package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * AgentStep 实体类，用于映射数据库表或持久化结构。
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

