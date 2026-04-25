package com.codepliot.task.entity;

/**
 * Agent Step 执行状态。
 */
public enum AgentStepStatus {
    /** 步骤正在执行中。 */
    RUNNING,
    /** 步骤执行成功。 */
    SUCCESS,
    /** 步骤执行失败。 */
    FAILED
}
