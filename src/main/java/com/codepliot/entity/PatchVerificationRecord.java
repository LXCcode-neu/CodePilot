package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 补丁验证记录实体，对应数据库表 patch_verification_record。
 * <p>记录补丁应用后的验证执行结果，如编译、测试等命令的执行情况。</p>
 */
@Data
@TableName("patch_verification_record")
@EqualsAndHashCode(callSuper = true)
public class PatchVerificationRecord extends BaseEntity {

    /** 关联的代理任务ID */
    @TableField("task_id")
    private Long taskId;

    /** 关联的补丁记录ID */
    @TableField("patch_record_id")
    private Long patchRecordId;

    /** 验证尝试次数 */
    @TableField("attempt_no")
    private Integer attemptNo;

    /** 验证命令名称（如 build、test 等） */
    @TableField("command_name")
    private String commandName;

    /** 实际执行的命令文本 */
    @TableField("command_text")
    private String commandText;

    /** 命令执行的工作目录 */
    @TableField("working_directory")
    private String workingDirectory;

    /** 命令退出码，0 表示成功 */
    @TableField("exit_code")
    private Integer exitCode;

    /** 验证是否通过 */
    @TableField("passed")
    private Boolean passed;

    /** 是否因超时而终止 */
    @TableField("timed_out")
    private Boolean timedOut;

    /** 命令输出摘要 */
    @TableField("output_summary")
    private String outputSummary;
}
