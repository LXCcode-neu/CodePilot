package com.codepliot.model;

import com.codepliot.entity.PatchReviewRecord;
import java.time.LocalDateTime;

/**
 * 补丁审核记录视图对象（VO）。
 * <p>用于向前端展示AI补丁审核的完整结果，包括评分、风险等级、发现项和建议等。</p>
 */
public record PatchReviewRecordVO(
        /** 审核记录ID */
        Long id,
        /** 关联的修复任务ID */
        Long taskId,
        /** 关联的补丁记录ID */
        Long patchRecordId,
        /** 审核模型的服务提供商 */
        String reviewerProvider,
        /** 审核使用的模型名称 */
        String reviewerModelName,
        /** 审核是否通过 */
        Boolean passed,
        /** 审核评分（0-100） */
        Integer score,
        /** 风险等级（如 HIGH、MEDIUM、LOW） */
        String riskLevel,
        /** 审核摘要 */
        String summary,
        /** 审核发现的问题列表（JSON格式） */
        String findings,
        /** 改进建议（JSON格式） */
        String recommendations,
        /** AI模型返回的原始响应内容 */
        String rawResponse,
        /** 创建时间 */
        LocalDateTime createdAt,
        /** 更新时间 */
        LocalDateTime updatedAt
) {
    /**
     * 从实体对象转换为视图对象。
     *
     * @param record 补丁审核记录实体
     * @return 视图对象，若输入为null则返回null
     */
    public static PatchReviewRecordVO from(PatchReviewRecord record) {
        if (record == null) {
            return null;
        }
        return new PatchReviewRecordVO(
                record.getId(),
                record.getTaskId(),
                record.getPatchRecordId(),
                record.getReviewerProvider(),
                record.getReviewerModelName(),
                record.getPassed(),
                record.getScore(),
                record.getRiskLevel(),
                record.getSummary(),
                record.getFindings(),
                record.getRecommendations(),
                record.getRawResponse(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
