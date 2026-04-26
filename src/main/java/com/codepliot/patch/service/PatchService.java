package com.codepliot.patch.service;

import com.codepliot.patch.dto.PatchGenerateResult;
import com.codepliot.patch.entity.PatchRecord;
import com.codepliot.patch.vo.PatchRecordVO;

public interface PatchService {

    PatchRecord saveGeneratedPatch(Long taskId, PatchGenerateResult result, String rawOutput);

    PatchRecord saveFailedPatch(Long taskId, String rawOutput, String risk);

    PatchRecordVO getTaskPatch(Long taskId);
}
