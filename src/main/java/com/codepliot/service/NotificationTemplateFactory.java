package com.codepliot.service;

import com.codepliot.entity.GitHubIssueEvent;
import com.codepliot.entity.UserRepoWatch;
import com.codepliot.model.NotificationEventType;
import com.codepliot.model.NotificationMessage;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateFactory {

    public NotificationMessage newIssue(UserRepoWatch watch, GitHubIssueEvent event) {
        return new NotificationMessage(
                "CodePilot 发现新的 GitHub Issue",
                watch.getOwner() + "/" + watch.getRepoName()
                        + " #" + event.getIssueNumber()
                        + "\n" + event.getIssueTitle()
                        + "\n\n可在 CodePilot 中确认是否执行自动修复。",
                NotificationEventType.NEW_ISSUE,
                event.getIssueUrl()
        );
    }

    public NotificationMessage repairStarted(UserRepoWatch watch, GitHubIssueEvent event, Long taskId) {
        return new NotificationMessage(
                "CodePilot 已开始修复 Issue",
                watch.getOwner() + "/" + watch.getRepoName()
                        + " #" + event.getIssueNumber()
                        + "\n任务 ID：" + taskId,
                NotificationEventType.REPAIR_STARTED,
                "/tasks/" + taskId
        );
    }

    public NotificationMessage patchReady(UserRepoWatch watch, GitHubIssueEvent event, Long taskId, Long patchId) {
        return new NotificationMessage(
                "CodePilot 已生成修复 Diff",
                watch.getOwner() + "/" + watch.getRepoName()
                        + " #" + event.getIssueNumber()
                        + "\n任务 ID：" + taskId
                        + "\nPatch ID：" + patchId
                        + "\n\n请在 CodePilot 中确认后再提交 PR。",
                NotificationEventType.PATCH_READY,
                "/tasks/" + taskId
        );
    }

    public NotificationMessage repairFailed(UserRepoWatch watch, GitHubIssueEvent event, Long taskId, String reason) {
        return new NotificationMessage(
                "CodePilot 修复 Issue 失败",
                watch.getOwner() + "/" + watch.getRepoName()
                        + " #" + event.getIssueNumber()
                        + "\n任务 ID：" + taskId
                        + "\n原因：" + (reason == null || reason.isBlank() ? "未提供失败原因" : reason),
                NotificationEventType.REPAIR_FAILED,
                "/tasks/" + taskId
        );
    }
}
