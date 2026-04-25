package com.codepliot.task.vo;

import com.codepliot.task.entity.AgentTask;
import java.time.LocalDateTime;

/**
 * 返回给前端的 Agent 任务信息。
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 将实体对象转换为返回对象。
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
                agentTask.getCreatedAt(),
                agentTask.getUpdatedAt()
        );
    }
}
