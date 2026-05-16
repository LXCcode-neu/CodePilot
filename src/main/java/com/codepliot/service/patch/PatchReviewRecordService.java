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

@Service
public class PatchReviewRecordService {

    private final PatchReviewRecordMapper patchReviewRecordMapper;
    private final ObjectMapper objectMapper;

    public PatchReviewRecordService(PatchReviewRecordMapper patchReviewRecordMapper, ObjectMapper objectMapper) {
        this.patchReviewRecordMapper = patchReviewRecordMapper;
        this.objectMapper = objectMapper;
    }

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
