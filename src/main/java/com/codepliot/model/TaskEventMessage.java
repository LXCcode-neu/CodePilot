package com.codepliot.model;

import java.time.LocalDateTime;
/**
 * TaskEventMessage 模型类，用于承载流程中的数据结构。
 */
public record TaskEventMessage(
        Long taskId,
        String taskStatus,
        String phase,
        String stepType,
        String message,
        LocalDateTime timestamp
) {
}

