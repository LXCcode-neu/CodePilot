package com.codepliot.service.notification;

import com.codepliot.client.FeishuBotClient;
import com.codepliot.config.BotProperties;
import com.codepliot.entity.BotActionCode;
import com.codepliot.entity.GitHubIssueEvent;
import com.codepliot.entity.PatchRecord;
import com.codepliot.entity.UserRepoWatch;
import com.codepliot.model.NotificationMessage;
import com.codepliot.service.bot.BotActionCodeService;
import org.springframework.stereotype.Service;

/**
 * 机器人通知服务。
 * <p>
 * 通过飞书机器人向群组发送通知消息，支持以下场景：
 * <ul>
 *   <li>新 Issue 发现通知</li>
 *   <li>补丁就绪通知（包含操作码供用户确认）</li>
 *   <li>修复失败通知</li>
 * </ul>
 * <p>
 * 当群组聊天 ID 未配置时，通知将被静默跳过。
 */
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

    /**
     * 向飞书群组发送新 Issue 发现通知。
     *
     * @param watch 仓库监听配置
     * @param event GitHub Issue 事件
     * @return 是否发送成功
     */
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

    /**
     * 向飞书群组发送补丁就绪通知。
     *
     * @param watch       仓库监听配置
     * @param event       GitHub Issue 事件
     * @param patchRecord 补丁记录
     */
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

    /**
     * 向飞书群组发送修复失败通知。
     *
     * @param watch   仓库监听配置
     * @param event   GitHub Issue 事件
     * @param taskId  Agent 任务 ID
     * @param reason  失败原因
     */
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
