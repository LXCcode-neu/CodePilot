package com.codepliot.model;

import com.codepliot.entity.GitHubIssueEvent;
import java.time.LocalDateTime;

/**
 * GitHub Issue 事件视图对象。
 * <p>用于展示从 GitHub 接收到的 Issue 事件的详细信息，包括 Issue 内容、状态及处理进度。</p>
 *
 * @param id            事件记录 ID
 * @param repoWatchId   仓库监控记录 ID
 * @param projectRepoId 项目仓库 ID
 * @param issueNumber   Issue 编号
 * @param issueTitle    Issue 标题
 * @param issueBody     Issue 正文内容
 * @param issueUrl      Issue 页面链接
 * @param issueState    Issue 状态（如 open、closed）
 * @param senderLogin   事件触发者的 GitHub 用户名
 * @param status        当前处理状态
 * @param agentTaskId   关联的代理任务 ID
 * @param notifiedAt    通知发送时间
 * @param createdAt     事件创建时间
 * @param updatedAt     事件更新时间
 */
public record GitHubIssueEventVO(
        Long id,
        Long repoWatchId,
        Long projectRepoId,
        Integer issueNumber,
        String issueTitle,
        String issueBody,
        String issueUrl,
        String issueState,
        String senderLogin,
        String status,
        Long agentTaskId,
        LocalDateTime notifiedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 从 GitHub Issue 事件实体转换为视图对象。
     *
     * @param event GitHub Issue 事件实体
     * @return 包含事件详情的 {@link GitHubIssueEventVO}
     */
    public static GitHubIssueEventVO from(GitHubIssueEvent event) {
        return new GitHubIssueEventVO(
                event.getId(),
                event.getRepoWatchId(),
                event.getProjectRepoId(),
                event.getIssueNumber(),
                event.getIssueTitle(),
                event.getIssueBody(),
                event.getIssueUrl(),
                event.getIssueState(),
                event.getSenderLogin(),
                event.getStatus(),
                event.getAgentTaskId(),
                event.getNotifiedAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
