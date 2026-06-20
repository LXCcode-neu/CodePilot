package com.codepliot.service.patch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codepliot.entity.PatchReviewRecord;
import com.codepliot.model.LlmRuntimeConfig;
import com.codepliot.model.PatchReviewRecordVO;
import com.codepliot.model.PatchReviewResult;
import com.codepliot.repository.PatchReviewRecordMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 补丁审查记录服务。
 * <p>
 * 管理 AI 对代码补丁的审查记录，包括：
 * <ul>
 *   <li>保存审查结果（通过/未通过、评分、风险等级、发现和建议）</li>
 *   <li>查询指定任务的最新审查记录</li>
 *   <li>按任务 ID 批量删除审查记录</li>
 * </ul>
 * <p>
 * 审查结果中的 findings 和 recommendations 以 JSON 格式存储。
 */
@Service
public class PatchReviewRecordService {

    private final PatchReviewRecordMapper patchReviewRecordMapper;
    private final ObjectMapper objectMapper;

    public PatchReviewRecordService(PatchReviewRecordMapper patchReviewRecordMapper, ObjectMapper objectMapper) {
        this.patchReviewRecordMapper = patchReviewRecordMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存补丁审查结果。
     *
     * @param taskId          Agent 任务 ID
     * @param patchRecordId   补丁记录 ID
     * @param llmRuntimeConfig 执行审查的 LLM 配置信息
     * @param result          审查结果
     * @return 保存的审查记录实体
     */
    @Transactional
    public PatchReviewRecord saveReviewResult(Long taskId,
                                              Long patchRecordId,
                                              LlmRuntimeConfig llmRuntimeConfig,
                                              PatchReviewResult result) {
        PatchReviewRecord record = new PatchReviewRecord();
        record.setTaskId(taskId);
        record.setPatchRecordId(patchRecordId);
        record.setReviewerProvider(llmRuntimeConfig == null ? null : llmRuntimeConfig.provider());
        record.setReviewerModelName(llmRuntimeConfig == null ? null : llmRuntimeConfig.modelName());
        record.setPassed(result.passed());
        record.setScore(result.score());
        record.setRiskLevel(result.riskLevel());
        record.setSummary(result.summary());
        record.setFindings(toJson(result.findings()));
        record.setRecommendations(toJson(result.recommendations()));
        record.setRawResponse(result.rawResponse());
        patchReviewRecordMapper.insert(record);
        return record;
    }

    /**
     * 查询指定任务的最新补丁审查记录。
     *
     * @param taskId Agent 任务 ID
     * @return 最新审查记录视图对象，若无记录则返回 null
     */
    public PatchReviewRecordVO getLatestByTaskId(Long taskId) {
        PatchReviewRecord record = patchReviewRecordMapper.selectOne(new LambdaQueryWrapper<PatchReviewRecord>()
                .eq(PatchReviewRecord::getTaskId, taskId)
                .orderByDesc(PatchReviewRecord::getCreatedAt)
                .last("limit 1"));
        return PatchReviewRecordVO.from(record);
    }

    @Transactional
    public void deleteByTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        patchReviewRecordMapper.delete(new LambdaUpdateWrapper<PatchReviewRecord>()
                .in(PatchReviewRecord::getTaskId, taskIds));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }
}
