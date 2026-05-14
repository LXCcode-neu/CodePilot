package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("patch_verification_record")
@EqualsAndHashCode(callSuper = true)
public class PatchVerificationRecord extends BaseEntity {

    @TableField("task_id")
    private Long taskId;

    @TableField("patch_record_id")
    private Long patchRecordId;

    @TableField("attempt_no")
    private Integer attemptNo;

    @TableField("command_name")
    private String commandName;

    @TableField("command_text")
    private String commandText;

    @TableField("working_directory")
    private String workingDirectory;

    @TableField("exit_code")
    private Integer exitCode;

    @TableField("passed")
    private Boolean passed;

    @TableField("timed_out")
    private Boolean timedOut;

    @TableField("output_summary")
    private String outputSummary;
}
