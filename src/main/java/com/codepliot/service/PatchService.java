package com.codepliot.service;

import com.codepliot.model.PatchGenerateResult;
import com.codepliot.entity.PatchRecord;
import com.codepliot.model.PatchRecordVO;
/**
 * PatchService 服务类，负责封装业务流程和领域能力。
 */
public interface PatchService {

    PatchRecord saveGeneratedPatch(Long taskId, PatchGenerateResult result, String rawOutput);

    PatchRecord saveFailedPatch(Long taskId, String rawOutput, String risk);

    PatchRecordVO getTaskPatch(Long taskId);
}

