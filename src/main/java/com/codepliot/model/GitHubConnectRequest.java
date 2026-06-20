package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

/**
 * GitHub 关联请求。
 * <p>用于接收 OAuth 授权回调中的 code 和 state 参数，完成 GitHub 账户关联。</p>
 *
 * @param code  OAuth 授权码，由 GitHub 授权后回调返回
 * @param state OAuth 状态参数，用于防止 CSRF 攻击
 */
public record GitHubConnectRequest(
        @NotBlank(message = "code cannot be blank")
        String code,
        @NotBlank(message = "state cannot be blank")
        String state
) {
}
