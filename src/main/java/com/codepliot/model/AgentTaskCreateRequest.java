package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
/**
 * AgentTaskCreateRequest 模型类，用于承载流程中的数据结构。
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

