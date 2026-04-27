package com.codepliot.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * RegisterRequest 模型类，用于承载流程中的数据结构。
 */
public record RegisterRequest(
        @NotBlank(message = "username cannot be blank")
        @Size(min = 4, max = 32, message = "username length must be between 4 and 32")
        String username,

        @NotBlank(message = "password cannot be blank")
        @Size(min = 6, max = 64, message = "password length must be between 6 and 64")
        String password,

        @NotBlank(message = "email cannot be blank")
        @Email(message = "email format is invalid")
        String email
) {
}

