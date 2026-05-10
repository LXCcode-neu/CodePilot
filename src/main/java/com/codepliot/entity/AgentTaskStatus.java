package com.codepliot.entity;
/**
 * AgentTaskStatus 实体类，用于映射数据库表或持久化结构。
 */
public enum AgentTaskStatus {
PENDING,
CLONING,
RETRIEVING,
ANALYZING,
GENERATING_PATCH,
VERIFYING,
WAITING_CONFIRM,
COMPLETED,
FAILED
}

