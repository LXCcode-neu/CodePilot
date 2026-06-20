package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 补丁评审记录实体，对应数据库表 patch_review_record。
 * <p>记录 AI 对生成补丁的评审结果，包括评分、风险等级和改进建议。</p>
 */
@Data
@TableName("patch_review_record")
@EqualsAndHashCode(callSuper = true)
public class PatchReviewRecord extends BaseEntity {

    /** 关联的代理任务ID */
    @TableField("task_id")
    private Long taskId;

    /** 关联的补丁记录ID */
    @TableField("patch_record_id")
    private Long patchRecordId;

    /** 评审所使用的模型提供商 */
    @TableField("reviewer_provider")
    private String reviewerProvider;

    /** 评审所使用的模型名称 */
    @TableField("reviewer_model_name")
    private String reviewerModelName;

    /** 是否通过评审 */
    @TableField("passed")
    private Boolean passed;

    /** 评审评分（0-100） */
    @TableField("score")
    private Integer score;

    /** 风险等级（如 low、medium、high） */
    @TableField("risk_level")
    private String riskLevel;

    /** 评审摘要 */
    @TableField("summary")
    private String summary;

    /** 评审发现的问题 */
    @TableField("findings")
    private String findings;

    /** 改进建议 */
    @TableField("recommendations")
    private String recommendations;

    /** AI 原始返回的评审结果 */
    @TableField("raw_response")
    private String rawResponse;
}
