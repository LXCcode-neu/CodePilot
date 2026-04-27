package com.codepliot.model;

import com.codepliot.entity.PatchRecord;
import java.time.LocalDateTime;
/**
 * PatchRecordVO 模型类，用于承载流程中的数据结构。
 */
public record PatchRecordVO(
        Long id,
        Long taskId,
        String analysis,
        String solution,
        String patch,
        String risk,
        String rawOutput,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
/**
 * 执行 from 相关逻辑。
 */
public static PatchRecordVO from(PatchRecord patchRecord) {
        return new PatchRecordVO(
                patchRecord.getId(),
                patchRecord.getTaskId(),
                patchRecord.getAnalysis(),
                patchRecord.getSolution(),
                patchRecord.getPatch(),
                patchRecord.getRisk(),
                patchRecord.getRawOutput(),
                patchRecord.getCreatedAt(),
                patchRecord.getUpdatedAt()
        );
    }
}

