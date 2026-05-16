package com.codepliot.service.githubIssue;

import com.codepliot.model.PatchGeneratedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GitHubIssuePatchEventListener {

    private final GitHubIssueEventService gitHubIssueEventService;

    public GitHubIssuePatchEventListener(GitHubIssueEventService gitHubIssueEventService) {
        this.gitHubIssueEventService = gitHubIssueEventService;
    }

    @EventListener
    public void onPatchGenerated(PatchGeneratedEvent event) {
        if (event.success()) {
            gitHubIssueEventService.markPatchReady(event.taskId(), event.patchId());
        } else {
            gitHubIssueEventService.markFailed(event.taskId(), event.reason());
        }
    }
}
