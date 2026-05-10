package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Patch 记录实体。
 *
 * <p>用于保存 LLM 生成的 patch 建议、风险说明、安全检查结果以及人工确认状态。
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

    @TableField("safety_check_result")
    private String safetyCheckResult;

    @TableField("raw_output")
    private String rawOutput;

    @TableField("confirmed")
    private Boolean confirmed;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    @TableField("pr_submitted")
    private Boolean prSubmitted;

    @TableField("pr_submitted_at")
    private LocalDateTime prSubmittedAt;

    @TableField("pr_url")
    private String prUrl;

    @TableField("pr_number")
    private Integer prNumber;

    @TableField("pr_branch")
    private String prBranch;
}
