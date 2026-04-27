package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * PatchRecord 实体类，用于映射数据库表或持久化结构。
 */
@Data
@TableName("patch_record")
@EqualsAndHashCode(callSuper = true)
public class PatchRecord extends BaseEntity {

    @TableField("task_id")
    private Long taskId;

    @TableField("analysis")
    private String analysis;

    @TableField("solution")
    private String solution;

    @TableField("patch")
    private String patch;

    @TableField("risk")
    private String risk;

    @TableField("raw_output")
    private String rawOutput;
}

