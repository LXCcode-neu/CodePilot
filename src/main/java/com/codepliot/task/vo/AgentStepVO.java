package com.codepliot.task.vo;

import com.codepliot.task.entity.AgentStep;
import java.time.LocalDateTime;

/**
 * 返回给前端的 Agent Step 信息。
 */
public record AgentStepVO(
        Long id,
        Long taskId,
        String stepType,
        String stepName,
        String input,
        String output,
        String status,
        String errorMessage,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 将实体对象转换为返回对象。
     */
    public static AgentStepVO from(AgentStep agentStep) {
        return new AgentStepVO(
                agentStep.getId(),
                agentStep.getTaskId(),
                agentStep.getStepType(),
                agentStep.getStepName(),
                agentStep.getInput(),
                agentStep.getOutput(),
                agentStep.getStatus(),
                agentStep.getErrorMessage(),
                agentStep.getStartTime(),
                agentStep.getEndTime(),
                agentStep.getCreatedAt(),
                agentStep.getUpdatedAt()
        );
    }
}
