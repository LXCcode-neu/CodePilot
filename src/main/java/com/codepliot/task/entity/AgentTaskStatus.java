package com.codepliot.task.entity;

/**
 * Agent 任务生命周期状态。
 */
public enum AgentTaskStatus {
    /** 等待执行，任务刚创建完成。 */
    PENDING,
    /** 正在准备拉取仓库。 */
    CLONING,
    /** 正在扫描和建立代码索引。 */
    INDEXING,
    /** 正在检索与 Issue 相关的代码上下文。 */
    RETRIEVING,
    /** 正在分析问题并生成修复思路。 */
    ANALYZING,
    /** 正在生成修复建议和 patch。 */
    GENERATING_PATCH,
    /** 已生成结果，等待用户确认是否继续。 */
    WAITING_CONFIRM,
    /** 任务已成功完成。 */
    COMPLETED,
    /** 任务执行失败。 */
    FAILED
}
