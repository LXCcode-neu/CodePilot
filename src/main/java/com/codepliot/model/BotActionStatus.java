package com.codepliot.model;

/**
 * 机器人动作状态枚举。
 * <p>用于表示机器人处理任务的各个阶段状态。</p>
 */
public enum BotActionStatus {

    /** 等待处理 */
    PENDING,

    /** 正在运行中 */
    RUNNING,

    /** 补丁已就绪，等待确认 */
    PATCH_READY,

    /** 已创建 Pull Request */
    PR_CREATED,

    /** 已忽略，不执行任何操作 */
    IGNORED,

    /** 已取消 */
    CANCELLED,

    /** 执行失败 */
    FAILED
}
