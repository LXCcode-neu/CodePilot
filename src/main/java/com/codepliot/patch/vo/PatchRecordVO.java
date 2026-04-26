package com.codepliot.patch.vo;

import com.codepliot.patch.entity.PatchRecord;
import java.time.LocalDateTime;

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
