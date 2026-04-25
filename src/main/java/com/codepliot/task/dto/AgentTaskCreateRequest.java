package com.codepliot.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建 Agent 任务的请求参数。
 */
public record AgentTaskCreateRequest(
        @NotNull(message = "projectId cannot be null")
        Long projectId,
        @NotBlank(message = "issueTitle cannot be blank")
        String issueTitle,
        @NotBlank(message = "issueDescription cannot be blank")
        String issueDescription
) {
}
