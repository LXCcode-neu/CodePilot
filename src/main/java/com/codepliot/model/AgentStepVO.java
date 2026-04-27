package com.codepliot.model;

import com.codepliot.entity.AgentStep;
import java.time.LocalDateTime;
/**
 * AgentStepVO 模型类，用于承载流程中的数据结构。
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
 * 执行 from 相关逻辑。
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

