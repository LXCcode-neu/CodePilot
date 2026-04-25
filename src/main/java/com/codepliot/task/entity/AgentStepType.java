package com.codepliot.task.entity;

/**
 * Agent Step 类型，表示当前执行步骤属于哪一类操作。
 */
public enum AgentStepType {
    /** 仓库准备阶段。 */
    CLONING,
    /** 代码索引阶段。 */
    INDEXING,
    /** 代码检索阶段。 */
    RETRIEVING,
    /** 问题分析阶段。 */
    ANALYZING,
    /** Patch 生成阶段。 */
    GENERATING_PATCH,
    /** 等待用户确认阶段。 */
    WAITING_CONFIRM,
    /** 其他通用步骤。 */
    GENERAL
}
