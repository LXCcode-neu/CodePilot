package com.codepliot.patch.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
