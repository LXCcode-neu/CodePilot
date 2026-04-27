package com.codepliot.model;

import com.codepliot.entity.AgentTaskStatus;

/**
 * Agent 执行决策结果。
 *
 * <p>用于表达当前任务在某个执行节点之后应该进入的状态以及对应的摘要信息。
 */
public record AgentExecutionDecision(
        AgentTaskStatus status,
        String resultSummary,
        String eventMessage
) {
}
