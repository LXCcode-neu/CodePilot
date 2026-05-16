package com.codepliot.entity;
/**
 * AgentStepType 实体类，用于映射数据库表或持久化结构。
 */
public enum AgentStepType {
CLONE_REPOSITORY,
SEARCH_RELEVANT_CODE,
ANALYZE_ISSUE,
GENERATE_PATCH,
VERIFY_PATCH,
REPAIR_PATCH,
REVIEW_PATCH,
COMPLETE_RUN
}

