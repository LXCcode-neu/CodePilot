package com.codepliot.model;

/**
 * 补丁生成事件。
 * <p>当自动修复任务生成补丁后发布的事件，用于触发后续流程（如AI审核、通知等）。</p>
 */
public record PatchGeneratedEvent(
        /** 关联的修复任务ID */
        Long taskId,
        /** 生成的补丁记录ID */
        Long patchId,
        /** 补丁生成是否成功 */
        boolean success,
        /** 生成失败时的原因说明 */
        String reason
) {
}
