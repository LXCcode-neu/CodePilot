package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GitHub Issue 事件实体，对应数据库表 github_issue_event。
 * <p>记录从 GitHub Webhook 接收到的 Issue 相关事件，用于触发后续的自动化处理流程。</p>
 */
@Data
@TableName("github_issue_event")
@EqualsAndHashCode(callSuper = true)
public class GitHubIssueEvent extends BaseEntity {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 关联的仓库订阅ID */
    @TableField("repo_watch_id")
    private Long repoWatchId;

    /** 关联的项目仓库ID */
    @TableField("project_repo_id")
    private Long projectRepoId;

    /** GitHub Issue 的原始ID */
    @TableField("github_issue_id")
    private Long githubIssueId;

    /** Issue 编号 */
    @TableField("issue_number")
    private Integer issueNumber;

    /** Issue 标题 */
    @TableField("issue_title")
    private String issueTitle;

    /** Issue 正文内容 */
    @TableField("issue_body")
    private String issueBody;

    /** Issue 的 GitHub 链接地址 */
    @TableField("issue_url")
    private String issueUrl;

    /** Issue 状态（open、closed 等） */
    @TableField("issue_state")
    private String issueState;

    /** 事件发送者的 GitHub 用户名 */
    @TableField("sender_login")
    private String senderLogin;

    /** 事件动作类型（opened、edited、closed 等） */
    @TableField("event_action")
    private String eventAction;

    /** 处理状态 */
    @TableField("status")
    private String status;

    /** 关联的代理任务ID */
    @TableField("agent_task_id")
    private Long agentTaskId;

    /** 通知发送时间 */
    @TableField("notified_at")
    private LocalDateTime notifiedAt;
}
