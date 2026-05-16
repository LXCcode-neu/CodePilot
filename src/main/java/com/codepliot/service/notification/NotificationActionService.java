package com.codepliot.service.notification;

import com.codepliot.config.AppProperties;
import com.codepliot.entity.NotificationActionToken;
import com.codepliot.model.GitHubIssueEventRunResult;
import com.codepliot.model.NotificationActionExecutionResult;
import com.codepliot.model.NotificationActionType;
import com.codepliot.service.githubIssue.GitHubIssueEventService;
import org.springframework.stereotype.Service;

@Service
public class NotificationActionService {

    private final NotificationActionTokenService notificationActionTokenService;
    private final GitHubIssueEventService gitHubIssueEventService;
    private final AppProperties appProperties;

    public NotificationActionService(NotificationActionTokenService notificationActionTokenService,
                                     GitHubIssueEventService gitHubIssueEventService,
                                     AppProperties appProperties) {
        this.notificationActionTokenService = notificationActionTokenService;
        this.gitHubIssueEventService = gitHubIssueEventService;
        this.appProperties = appProperties;
    }

    public NotificationActionExecutionResult runFix(String rawToken) {
        NotificationActionToken token = notificationActionTokenService.claim(rawToken, NotificationActionType.RUN_FIX);
        GitHubIssueEventRunResult result = gitHubIssueEventService.runFromNotification(
                token.getIssueEventId(),
                token.getUserId()
        );
        return new NotificationActionExecutionResult(
                true,
                "已开始修复",
                "CodePilot 已创建并启动修复任务，任务 ID：" + result.taskId(),
                appProperties.buildUrl("/tasks/" + result.taskId())
        );
    }

    public NotificationActionExecutionResult ignore(String rawToken) {
        NotificationActionToken token = notificationActionTokenService.claim(rawToken, NotificationActionType.IGNORE);
        gitHubIssueEventService.ignoreFromNotification(token.getIssueEventId(), token.getUserId());
        return new NotificationActionExecutionResult(
                true,
                "已忽略 Issue",
                "CodePilot 已将该 Issue 标记为忽略。",
                appProperties.buildUrl("/issue-automation")
        );
    }
}
