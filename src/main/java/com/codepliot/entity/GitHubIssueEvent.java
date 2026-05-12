package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("github_issue_event")
@EqualsAndHashCode(callSuper = true)
public class GitHubIssueEvent extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("repo_watch_id")
    private Long repoWatchId;

    @TableField("project_repo_id")
    private Long projectRepoId;

    @TableField("github_issue_id")
    private Long githubIssueId;

    @TableField("issue_number")
    private Integer issueNumber;

    @TableField("issue_title")
    private String issueTitle;

    @TableField("issue_body")
    private String issueBody;

    @TableField("issue_url")
    private String issueUrl;

    @TableField("issue_state")
    private String issueState;

    @TableField("sender_login")
    private String senderLogin;

    @TableField("event_action")
    private String eventAction;

    @TableField("status")
    private String status;

    @TableField("agent_task_id")
    private Long agentTaskId;

    @TableField("notified_at")
    private LocalDateTime notifiedAt;
}
