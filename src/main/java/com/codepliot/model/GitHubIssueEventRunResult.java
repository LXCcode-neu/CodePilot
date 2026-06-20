package com.codepliot.model;

/**
 * GitHub Issue 事件处理结果。
 * <p>表示机器人对 GitHub Issue 事件执行处理后的返回结果。</p>
 *
 * @param taskId 代理任务 ID，用于追踪后续执行状态
 * @param status 任务处理状态
 */
public record GitHubIssueEventRunResult(
        Long taskId,
        String status
) {
}
