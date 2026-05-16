package com.codepliot.model;

import com.codepliot.entity.PatchReviewRecord;
import java.time.LocalDateTime;

public record PatchReviewRecordVO(
        Long id,
        Long taskId,
        Long patchRecordId,
        String reviewerProvider,
        String reviewerModelName,
        Boolean passed,
        Integer score,
        String riskLevel,
        String summary,
        String findings,
        String recommendations,
        String rawResponse,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
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
