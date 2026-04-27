package com.codepliot.model;

import com.codepliot.entity.PatchRecord;
import java.time.LocalDateTime;

/**
 * Patch 记录视图对象。
 */
public record PatchRecordVO(
        Long id,
        Long taskId,
        String analysis,
        String solution,
        String patch,
        String risk,
        String safetyCheckResult,
        String rawOutput,
        Boolean confirmed,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 从实体转换为视图对象。
     */
    public static PatchRecordVO from(PatchRecord patchRecord) {
        return new PatchRecordVO(
                patchRecord.getId(),
                patchRecord.getTaskId(),
                patchRecord.getAnalysis(),
                patchRecord.getSolution(),
                patchRecord.getPatch(),
                patchRecord.getRisk(),
                patchRecord.getSafetyCheckResult(),
                patchRecord.getRawOutput(),
                patchRecord.getConfirmed(),
                patchRecord.getConfirmedAt(),
                patchRecord.getCreatedAt(),
                patchRecord.getUpdatedAt()
        );
    }
}
