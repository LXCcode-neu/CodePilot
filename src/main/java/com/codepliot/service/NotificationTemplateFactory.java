package com.codepliot.service;

import com.codepliot.config.AppProperties;
import com.codepliot.entity.BotActionCode;
import com.codepliot.entity.GitHubIssueEvent;
import com.codepliot.entity.PatchRecord;
import com.codepliot.entity.UserRepoWatch;
import com.codepliot.model.NotificationEventType;
import com.codepliot.model.NotificationMessage;
import com.codepliot.model.PatchFileChange;
import com.codepliot.model.PatchRecordVO;
import com.codepliot.model.PullRequestPreview;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateFactory {

    private static final int MAX_TEXT_LENGTH = 3200;

    private final BotActionCodeService botActionCodeService;
    private final AppProperties appProperties;

    public NotificationTemplateFactory(BotActionCodeService botActionCodeService,
                                       AppProperties appProperties) {
        this.botActionCodeService = botActionCodeService;
        this.appProperties = appProperties;
    }

    public NotificationMessage newIssue(UserRepoWatch watch, GitHubIssueEvent event) {
        BotActionCode code = botActionCodeService.findOrCreateForIssue(event.getUserId(), event.getId());
        return new NotificationMessage(
                "CodePilot 发现新的 GitHub Issue",
                watch.getOwner() + "/" + watch.getRepoName()
                        + " #" + event.getIssueNumber()
                        + "\n" + event.getIssueTitle()
                        + "\n\n操作码：" + code.getActionCode()
                        + "\n\n手机端直接在群里回复："
                        + "\n修复 " + code.getActionCode()
                        + "\n忽略 " + code.getActionCode()
                        + "\n状态 " + code.getActionCode(),
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
                appProperties.buildUrl("/tasks/" + taskId)
        );
    }

    public NotificationMessage patchReady(UserRepoWatch watch, GitHubIssueEvent event, Long taskId, Long patchId) {
        return new NotificationMessage(
                "CodePilot 已生成修复 Diff",
                watch.getOwner() + "/" + watch.getRepoName()
                        + " #" + event.getIssueNumber()
                        + "\n任务 ID：" + taskId
                        + "\nPatch ID：" + patchId
                        + "\n\n请在群里回复“确认PR 操作码”来提交 PR，或打开 CodePilot 查看完整 Diff。",
                NotificationEventType.PATCH_READY,
                appProperties.buildUrl("/tasks/" + taskId)
        );
    }

    public NotificationMessage patchReady(UserRepoWatch watch, GitHubIssueEvent event, PatchRecord patchRecord) {
        PatchRecordVO patch = PatchRecordVO.from(patchRecord);
        PullRequestPreview pullRequest = patch.pullRequest();
        BotActionCode code = botActionCodeService.findByTaskId(patch.taskId());
        String actionCode = code == null ? "CP-未知" : code.getActionCode();
        String content = truncate(
                watch.getOwner() + "/" + watch.getRepoName()
                        + " #" + event.getIssueNumber()
                        + "\n任务 ID：" + patch.taskId()
                        + "\nPatch ID：" + patch.id()
                        + "\n操作码：" + actionCode
                        + "\n\nDiff 摘要：\n" + buildDiffSummary(patch)
                        + "\n\nPR Draft：\n" + buildPullRequestDraft(pullRequest)
                        + "\n\n手机端直接在群里回复："
                        + "\n确认PR " + actionCode
                        + "\n状态 " + actionCode,
                MAX_TEXT_LENGTH
        );

        return new NotificationMessage(
                "CodePilot 已生成修复 Diff 和 PR Draft",
                content,
                NotificationEventType.PATCH_READY,
                appProperties.buildUrl("/tasks/" + patch.taskId())
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
                appProperties.buildUrl("/tasks/" + taskId)
        );
    }

    private String buildDiffSummary(PatchRecordVO patch) {
        if (patch.fileChanges() == null || patch.fileChanges().isEmpty()) {
            return "未解析到结构化 Diff，请打开任务查看原始 patch。";
        }
        int addedLines = patch.fileChanges().stream().mapToInt(file -> valueOrZero(file.addedLines())).sum();
        int removedLines = patch.fileChanges().stream().mapToInt(file -> valueOrZero(file.removedLines())).sum();
        StringBuilder summary = new StringBuilder();
        summary.append("- 文件数：").append(patch.fileChanges().size()).append('\n')
                .append("- 新增行：+").append(addedLines).append('\n')
                .append("- 删除行：-").append(removedLines).append('\n')
                .append("- 变更文件：");
        int limit = Math.min(patch.fileChanges().size(), 8);
        for (int i = 0; i < limit; i++) {
            PatchFileChange file = patch.fileChanges().get(i);
            summary.append("\n  - ")
                    .append(file.filePath())
                    .append(" (+")
                    .append(valueOrZero(file.addedLines()))
                    .append(" / -")
                    .append(valueOrZero(file.removedLines()))
                    .append(')');
        }
        if (patch.fileChanges().size() > limit) {
            summary.append("\n  - ... 还有 ").append(patch.fileChanges().size() - limit).append(" 个文件");
        }
        return summary.toString();
    }

    private String buildPullRequestDraft(PullRequestPreview pullRequest) {
        if (pullRequest == null) {
            return "未生成 PR Draft。";
        }
        return "标题：" + nullToFallback(pullRequest.title(), "CodePilot patch")
                + "\n分支：" + nullToFallback(pullRequest.branchName(), "未生成")
                + "\nCommit：" + nullToFallback(pullRequest.commitMessage(), "未生成")
                + "\n\n" + truncate(nullToFallback(pullRequest.body(), "未生成 PR 描述。"), 1400);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String nullToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 20)).trim() + "\n...（已截断）";
    }
}
