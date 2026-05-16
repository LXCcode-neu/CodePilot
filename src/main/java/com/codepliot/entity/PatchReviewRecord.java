package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("patch_review_record")
@EqualsAndHashCode(callSuper = true)
public class PatchReviewRecord extends BaseEntity {

    @TableField("task_id")
    private Long taskId;

    @TableField("patch_record_id")
    private Long patchRecordId;

    @TableField("reviewer_provider")
    private String reviewerProvider;

    @TableField("reviewer_model_name")
    private String reviewerModelName;

    @TableField("passed")
    private Boolean passed;

    @TableField("score")
    private Integer score;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("summary")
    private String summary;

    @TableField("findings")
    private String findings;

    @TableField("recommendations")
    private String recommendations;

    @TableField("raw_response")
    private String rawResponse;
}
