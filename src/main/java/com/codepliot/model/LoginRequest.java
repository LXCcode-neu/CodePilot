package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;
/**
 * LoginRequest 模型类，用于承载流程中的数据结构。
 */
public record LoginRequest(
        @NotBlank(message = "username cannot be blank")
        String username,

        @NotBlank(message = "password cannot be blank")
        String password
) {
}

