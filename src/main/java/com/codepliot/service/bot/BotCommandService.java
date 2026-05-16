package com.codepliot.service.bot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.client.FeishuBotClient;
import com.codepliot.entity.AgentTask;
import com.codepliot.entity.BotActionCode;
import com.codepliot.entity.PatchRecord;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.BotActionStatus;
import com.codepliot.model.BotCommand;
import com.codepliot.model.BotCommandType;
import com.codepliot.model.BotIncomingMessage;
import com.codepliot.model.GitHubIssueEventRunResult;
import com.codepliot.model.PullRequestSubmitResult;
import com.codepliot.repository.AgentTaskMapper;
import com.codepliot.repository.PatchRecordMapper;
import com.codepliot.service.githubIssue.GitHubIssueEventService;
import com.codepliot.service.patch.PatchService;
import com.codepliot.service.patch.PullRequestSubmitService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BotCommandService {

    private final BotCommandParser botCommandParser;
    private final BotActionCodeService botActionCodeService;
    private final GitHubIssueEventService gitHubIssueEventService;
    private final PatchService patchService;
    private final PullRequestSubmitService pullRequestSubmitService;
    private final AgentTaskMapper agentTaskMapper;
    private final PatchRecordMapper patchRecordMapper;
    private final FeishuBotClient feishuBotClient;

    public BotCommandService(BotCommandParser botCommandParser,
                             BotActionCodeService botActionCodeService,
                             GitHubIssueEventService gitHubIssueEventService,
                             PatchService patchService,
                             PullRequestSubmitService pullRequestSubmitService,
                             AgentTaskMapper agentTaskMapper,
                             PatchRecordMapper patchRecordMapper,
                             FeishuBotClient feishuBotClient) {
        this.botCommandParser = botCommandParser;
        this.botActionCodeService = botActionCodeService;
        this.gitHubIssueEventService = gitHubIssueEventService;
        this.patchService = patchService;
        this.pullRequestSubmitService = pullRequestSubmitService;
        this.agentTaskMapper = agentTaskMapper;
        this.patchRecordMapper = patchRecordMapper;
        this.feishuBotClient = feishuBotClient;
    }

    public void handle(BotIncomingMessage message) {
        Optional<BotCommand> parsed = botCommandParser.parse(message.text());
        if (parsed.isEmpty()) {
            return;
        }
        BotCommand command = parsed.get();
        try {
            BotActionCode code = botActionCodeService.requireUsableCode(
                    command.actionCode(),
                    message.channelType(),
                    message.chatId()
            );
            code = botActionCodeService.bindChatAndMessage(code, message.chatId(), message.messageId());
            String reply = execute(command, code);
            reply(message, reply);
        } catch (BusinessException exception) {
            reply(message, "CodePilot 操作失败：" + exception.getMessage());
        } catch (RuntimeException exception) {
            reply(message, "CodePilot 操作失败：" + safeMessage(exception));
        }
    }

    private String execute(BotCommand command, BotActionCode code) {
        return switch (command.type()) {
            case RUN_FIX -> runFix(code);
            case IGNORE -> ignore(code);
            case STATUS -> status(code);
            case CONFIRM_PR -> confirmPr(code);
            case CANCEL -> cancel(code);
        };
    }

    private String runFix(BotActionCode code) {
        if (code.getTaskId() != null) {
            return "该 Issue 已经创建过修复任务。\n操作码：" + code.getActionCode()
                    + "\n任务 ID：" + code.getTaskId()
                    + "\n当前状态：" + statusLabel(code);
        }
        GitHubIssueEventRunResult result = gitHubIssueEventService.runFromNotification(
                code.getIssueEventId(),
                code.getUserId()
        );
        botActionCodeService.markRunning(code, result.taskId());
        return "已开始修复。\n操作码：" + code.getActionCode() + "\n任务 ID：" + result.taskId();
    }

    private String ignore(BotActionCode code) {
        if (code.getTaskId() != null) {
            return "该 Issue 已经进入修复流程，不能再忽略。\n操作码：" + code.getActionCode()
                    + "\n任务 ID：" + code.getTaskId();
        }
        gitHubIssueEventService.ignoreFromNotification(code.getIssueEventId(), code.getUserId());
        botActionCodeService.markIgnored(code);
        return "已忽略该 Issue。\n操作码：" + code.getActionCode();
    }

    private String status(BotActionCode code) {
        StringBuilder reply = new StringBuilder();
        reply.append("CodePilot 状态\n操作码：").append(code.getActionCode())
                .append("\n当前状态：").append(statusLabel(code));
        if (code.getTaskId() != null) {
            AgentTask task = agentTaskMapper.selectById(code.getTaskId());
            reply.append("\n任务 ID：").append(code.getTaskId());
            if (task != null) {
                reply.append("\n任务状态：").append(task.getStatus());
                if (task.getErrorMessage() != null && !task.getErrorMessage().isBlank()) {
                    reply.append("\n错误：").append(task.getErrorMessage());
                }
            }
        }
        if (code.getPatchId() != null) {
            reply.append("\nPatch ID：").append(code.getPatchId());
        }
        return reply.toString();
    }

    private String confirmPr(BotActionCode code) {
        if (code.getTaskId() == null) {
            return "还没有修复任务，先回复：修复 " + code.getActionCode();
        }
        PatchRecord patchRecord = patchRecordMapper.selectOne(new LambdaQueryWrapper<PatchRecord>()
                .eq(PatchRecord::getTaskId, code.getTaskId())
                .last("limit 1"));
        if (patchRecord == null) {
            return "Patch 还没有生成，请稍后回复：状态 " + code.getActionCode();
        }
        if (!Boolean.TRUE.equals(patchRecord.getConfirmed())) {
            patchService.confirmTaskPatch(code.getTaskId(), code.getUserId());
        }
        PullRequestSubmitResult result = pullRequestSubmitService.submit(code.getTaskId(), code.getUserId());
        botActionCodeService.markPrCreated(code);
        return "Pull Request 已提交。\n操作码：" + code.getActionCode()
                + "\nPR #" + result.number()
                + "\n" + result.url();
    }

    private String cancel(BotActionCode code) {
        if (BotActionStatus.RUNNING.name().equals(code.getStatus())) {
            return "任务已经在运行中，当前版本暂不支持从群里强制停止。\n操作码：" + code.getActionCode();
        }
        botActionCodeService.markCancelled(code);
        return "已放弃该操作码。\n操作码：" + code.getActionCode();
    }

    private String statusLabel(BotActionCode code) {
        return code.getStatus() == null ? "UNKNOWN" : code.getStatus();
    }

    private void reply(BotIncomingMessage message, String text) {
        if (message.channelType() == com.codepliot.model.NotificationChannelType.FEISHU) {
            feishuBotClient.sendTextToChat(message.chatId(), text);
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
