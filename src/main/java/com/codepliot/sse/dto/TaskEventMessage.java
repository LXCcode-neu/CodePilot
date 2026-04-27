package com.codepliot.sse.dto;

import java.time.LocalDateTime;

/**
 * 任务 SSE 事件消息。
 */
public record TaskEventMessage(
        Long taskId,
        String taskStatus,
        String stepType,
        String message,
        LocalDateTime timestamp
) {
}
