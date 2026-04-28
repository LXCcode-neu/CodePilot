package com.codepliot.service.patch;

import com.codepliot.entity.PatchRecord;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.PatchGenerateResult;
import com.codepliot.model.PatchRecordVO;
import com.codepliot.model.PatchSafetyCheckResult;

/**
 * Patch 相关服务接口。
 */
public interface PatchService {

    /**
     * 保存成功生成的 patch 记录。
     */
    PatchRecord saveGeneratedPatch(Long taskId,
                                   PatchGenerateResult result,
                                   String rawOutput,
                                   PatchSafetyCheckResult safetyCheckResult);

    /**
     * 保存 patch 生成失败记录。
     */
    PatchRecord saveFailedPatch(Long taskId, String rawOutput, String risk);

    /**
     * 查询任务对应的 patch 记录。
     */
    PatchRecordVO getTaskPatch(Long taskId);

    /**
     * 确认任务对应的 patch，并推进任务到已完成状态。
     */
    AgentTaskVO confirmTaskPatch(Long taskId);
}