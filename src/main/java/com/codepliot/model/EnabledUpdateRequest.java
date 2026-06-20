package com.codepliot.model;

import jakarta.validation.constraints.NotNull;

/**
 * 启用/禁用状态更新请求。
 * <p>用于切换某个功能或监控的启用/禁用状态。</p>
 *
 * @param enabled 是否启用，不能为 null
 */
public record EnabledUpdateRequest(
        @NotNull(message = "enabled cannot be null")
        Boolean enabled
) {
}
