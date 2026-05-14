package com.codepliot.service;

import com.codepliot.client.FeishuBotClient;
import com.codepliot.config.BotProperties;
import com.codepliot.entity.BotActionCode;
import com.codepliot.entity.GitHubIssueEvent;
import com.codepliot.entity.PatchRecord;
import com.codepliot.entity.UserRepoWatch;
import com.codepliot.model.NotificationMessage;
import org.springframework.stereotype.Service;

@Service
public class BotNotificationService {

    private final BotActionCodeService botActionCodeService;
    private final NotificationTemplateFactory notificationTemplateFactory;
    private final FeishuBotClient feishuBotClient;
    private final BotProperties botProperties;

    public BotNotificationService(BotActionCodeService botActionCodeService,
                                  NotificationTemplateFactory notificationTemplateFactory,
                                  FeishuBotClient feishuBotClient,
                                  BotProperties botProperties) {
        this.botActionCodeService = botActionCodeService;
        this.notificationTemplateFactory = notificationTemplateFactory;
        this.feishuBotClient = feishuBotClient;
        this.botProperties = botProperties;
    }

    public boolean newIssue(UserRepoWatch watch, GitHubIssueEvent event) {
        String chatId = botProperties.getFeishu().getDefaultChatId();
        if (chatId == null || chatId.isBlank()) {
            return false;
        }
        NotificationMessage message = notificationTemplateFactory.newIssue(watch, event);
        try {
            feishuBotClient.sendTextToChat(chatId, message.title() + "\n\n" + message.content());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public void patchReady(UserRepoWatch watch, GitHubIssueEvent event, PatchRecord patchRecord) {
        if (patchRecord == null) {
            return;
        }
        botActionCodeService.markPatchReady(patchRecord.getTaskId(), patchRecord.getId());
        BotActionCode code = botActionCodeService.findByTaskId(patchRecord.getTaskId());
        if (code == null || code.getChatId() == null || code.getChatId().isBlank()) {
            return;
        }
        NotificationMessage message = notificationTemplateFactory.patchReady(watch, event, patchRecord);
        try {
            feishuBotClient.sendTextToChat(code.getChatId(), message.title() + "\n\n" + message.content());
        } catch (RuntimeException ignored) {
            // Existing notification channels still provide the fallback delivery path.
        }
    }

    public void repairFailed(UserRepoWatch watch, GitHubIssueEvent event, Long taskId, String reason) {
        botActionCodeService.markFailed(taskId);
        BotActionCode code = botActionCodeService.findByTaskId(taskId);
        if (code == null || code.getChatId() == null || code.getChatId().isBlank()) {
            return;
        }
        NotificationMessage message = notificationTemplateFactory.repairFailed(watch, event, taskId, reason);
        try {
            feishuBotClient.sendTextToChat(code.getChatId(), message.title() + "\n\n" + message.content());
        } catch (RuntimeException ignored) {
            // Existing notification channels still provide the fallback delivery path.
        }
    }
}
