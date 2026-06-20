package com.codepliot.service.githubIssue;

import com.codepliot.model.PatchGeneratedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * GitHub Issue 补丁生成事件监听器。
 * <p>
 * 监听 {@link PatchGeneratedEvent} 事件，根据补丁生成结果更新 GitHub Issue 事件状态：
 * <ul>
 *   <li>生成成功时，标记事件为"补丁就绪"状态</li>
 *   <li>生成失败时，标记事件为"失败"状态</li>
 * </ul>
 */
@Component
public class GitHubIssuePatchEventListener {

    private final GitHubIssueEventService gitHubIssueEventService;

    public GitHubIssuePatchEventListener(GitHubIssueEventService gitHubIssueEventService) {
        this.gitHubIssueEventService = gitHubIssueEventService;
    }

    /**
     * 处理补丁生成完成事件，根据成功或失败更新对应的 GitHub Issue 事件状态。
     *
     * @param event 补丁生成事件，包含任务 ID、补丁 ID、成功标志和失败原因
     */
    @EventListener
    public void onPatchGenerated(PatchGeneratedEvent event) {
        if (event.success()) {
            gitHubIssueEventService.markPatchReady(event.taskId(), event.patchId());
        } else {
            gitHubIssueEventService.markFailed(event.taskId(), event.reason());
        }
    }
}
