package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;
/**
 * ProjectCreateRequest 模型类，用于承载流程中的数据结构。
 */
public record ProjectCreateRequest(
        @NotBlank(message = "repoUrl cannot be blank")
        String repoUrl
) {
}

