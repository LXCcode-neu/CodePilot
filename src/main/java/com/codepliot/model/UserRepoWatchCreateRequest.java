package com.codepliot.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户仓库监控创建请求对象。
 * <p>用于接收前端提交的新增仓库监控配置请求，支持自动检测代码变更并触发修复流程。</p>
 */
public record UserRepoWatchCreateRequest(
        /** 仓库所有者（用户名或组织名） */
        @NotBlank(message = "owner cannot be blank")
        String owner,
        /** 仓库名称 */
        @NotBlank(message = "repoName cannot be blank")
        String repoName,
        /** 仓库URL地址 */
        @NotBlank(message = "repoUrl cannot be blank")
        String repoUrl,
        /** 默认监控分支（可选） */
        String defaultBranch
) {
}
