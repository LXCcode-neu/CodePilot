package com.codepliot.trace.entity;

/**
 * Agent 执行步骤类型。
 */
public enum AgentStepType {
    /** 拉取仓库。 */
    CLONE_REPOSITORY,
    /** 构建代码索引。 */
    BUILD_CODE_INDEX,
    /** 检索相关代码。 */
    SEARCH_RELEVANT_CODE,
    /** 分析 Issue。 */
    ANALYZE_ISSUE,
    /** 生成 Patch。 */
    GENERATE_PATCH,
    /** Mock 流程完成。 */
    COMPLETE_RUN
}
