package com.codepliot.trace.entity;

/**
 * Agent 执行步骤状态。
 */
public enum AgentStepStatus {
    /** 步骤执行中。 */
    RUNNING,
    /** 步骤执行成功。 */
    SUCCESS,
    /** 步骤执行失败。 */
    FAILED
}
