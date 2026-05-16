package com.codepliot.model;

import com.codepliot.entity.AgentTask;
import java.time.LocalDateTime;
/**
 * AgentTaskVO 模型类，用于承载流程中的数据结构。
 */
public record AgentTaskVO(
        Long id,
        Long userId,
        Long projectId,
        String issueTitle,
        String issueDescription,
        String status,
        String resultSummary,
        String errorMessage,
        String llmProvider,
        String llmModelName,
        String llmDisplayName,
        String sourceType,
        Long sourceId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
/**
 * 执行 from 相关逻辑。
 */
public static AgentTaskVO from(AgentTask agentTask) {
        return new AgentTaskVO(
                agentTask.getId(),
                agentTask.getUserId(),
                agentTask.getProjectId(),
                agentTask.getIssueTitle(),
                agentTask.getIssueDescription(),
                agentTask.getStatus(),
                agentTask.getResultSummary(),
                agentTask.getErrorMessage(),
                agentTask.getLlmProvider(),
                agentTask.getLlmModelName(),
                agentTask.getLlmDisplayName(),
                agentTask.getSourceType(),
                agentTask.getSourceId(),
                agentTask.getCreatedAt(),
                agentTask.getUpdatedAt()
        );
    }
}

