package com.codepliot.service.bot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.BotActionCode;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.BotActionStatus;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.NotificationChannelType;
import com.codepliot.repository.BotActionCodeMapper;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 机器人操作码服务。
 * <p>
 * 管理飞书等 Bot 渠道的操作码（Action Code）生命周期。操作码是用户通过 Bot
 * 发起操作时的唯一标识，用于关联 Issue 事件与 Agent 任务。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>为 Issue 事件生成唯一操作码（格式：CP-XXXXX，24 小时有效）</li>
 *   <li>验证操作码的可用性（检查渠道类型、过期时间、聊天绑定）</li>
 *   <li>管理操作码的状态流转（PENDING -> RUNNING -> PATCH_READY/FAILED/PR_CREATED/IGNORED/CANCELLED）</li>
 *   <li>绑定聊天 ID 和消息 ID 用于后续消息更新</li>
 * </ul>
 * </p>
 */
@Service
public class BotActionCodeService {

    private static final char[] CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private final BotActionCodeMapper botActionCodeMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public BotActionCodeService(BotActionCodeMapper botActionCodeMapper) {
        this.botActionCodeMapper = botActionCodeMapper;
    }

    /**
     * 查找或创建指定 Issue 事件的操作码。
     * <p>
     * 如果该 Issue 事件已有操作码则直接返回，否则生成一个新的操作码。
     * 新操作码默认状态为 PENDING，有效期 24 小时。
     * </p>
     *
     * @param userId      用户 ID
     * @param issueEventId Issue 事件 ID
     * @return 操作码实体
     */
    @Transactional
    public BotActionCode findOrCreateForIssue(Long userId, Long issueEventId) {
        BotActionCode existing = botActionCodeMapper.selectOne(new LambdaQueryWrapper<BotActionCode>()
                .eq(BotActionCode::getIssueEventId, issueEventId)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }

        BotActionCode code = new BotActionCode();
        code.setUserId(userId);
        code.setIssueEventId(issueEventId);
        code.setChannelType(NotificationChannelType.FEISHU.name());
        code.setActionCode(nextUniqueCode());
        code.setStatus(BotActionStatus.PENDING.name());
        code.setExpiresAt(LocalDateTime.now().plusHours(24));
        botActionCodeMapper.insert(code);
        return code;
    }

    /**
     * 获取并验证操作码的可用性。
     * <p>
     * 检查操作码是否存在、渠道类型是否匹配、是否已过期、是否属于当前聊天。
     * </p>
     *
     * @param actionCode  操作码字符串
     * @param channelType 通知渠道类型（可为 null，表示不检查渠道）
     * @param chatId      聊天 ID（可为 null，表示不检查聊天绑定）
     * @return 验证通过的操作码实体
     * @throws BusinessException 如果操作码不存在、渠道不匹配、已过期或不属于当前聊天
     */
    public BotActionCode requireUsableCode(String actionCode, NotificationChannelType channelType, String chatId) {
        BotActionCode code = requireByCode(actionCode);
        if (channelType != null && !channelType.name().equals(code.getChannelType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Action code does not belong to this bot channel");
        }
        if (code.getExpiresAt() == null || code.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Action code has expired");
        }
        if (code.getChatId() != null && chatId != null && !code.getChatId().equals(chatId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Action code belongs to another chat");
        }
        return code;
    }

    /**
     * 绑定聊天 ID 和消息 ID 到操作码。
     * <p>
     * 用于后续通过 Bot 更新消息内容。聊天 ID 仅在首次绑定时设置，
     * 消息 ID 每次都会更新为最新的消息 ID。
     * </p>
     *
     * @param code      操作码实体
     * @param chatId    聊天 ID
     * @param messageId 消息 ID
     * @return 更新后的操作码实体
     */
    @Transactional
    public BotActionCode bindChatAndMessage(BotActionCode code, String chatId, String messageId) {
        if (chatId != null && !chatId.isBlank() && (code.getChatId() == null || code.getChatId().isBlank())) {
            code.setChatId(chatId);
        }
        if (messageId != null && !messageId.isBlank()) {
            code.setLastMessageId(messageId);
        }
        botActionCodeMapper.updateById(code);
        return code;
    }

    @Transactional
    public void markRunning(BotActionCode code, Long taskId) {
        code.setTaskId(taskId);
        code.setStatus(BotActionStatus.RUNNING.name());
        botActionCodeMapper.updateById(code);
    }

    @Transactional
    public void markIgnored(BotActionCode code) {
        code.setStatus(BotActionStatus.IGNORED.name());
        botActionCodeMapper.updateById(code);
    }

    @Transactional
    public void markCancelled(BotActionCode code) {
        code.setStatus(BotActionStatus.CANCELLED.name());
        botActionCodeMapper.updateById(code);
    }

    /**
     * 标记操作码对应的补丁已就绪。
     *
     * @param taskId  任务 ID
     * @param patchId 补丁记录 ID
     */
    @Transactional
    public void markPatchReady(Long taskId, Long patchId) {
        BotActionCode code = findByTaskId(taskId);
        if (code == null) {
            return;
        }
        code.setPatchId(patchId);
        code.setStatus(BotActionStatus.PATCH_READY.name());
        botActionCodeMapper.updateById(code);
    }

    @Transactional
    public void markFailed(Long taskId) {
        BotActionCode code = findByTaskId(taskId);
        if (code == null) {
            return;
        }
        code.setStatus(BotActionStatus.FAILED.name());
        botActionCodeMapper.updateById(code);
    }

    /**
     * 标记操作码对应的 Pull Request 已创建。
     *
     * @param code 操作码实体
     */
    @Transactional
    public void markPrCreated(BotActionCode code) {
        code.setStatus(BotActionStatus.PR_CREATED.name());
        botActionCodeMapper.updateById(code);
    }

    /**
     * 根据任务 ID 查找对应的操作码。
     *
     * @param taskId 任务 ID
     * @return 操作码实体，如果不存在则返回 null
     */
    public BotActionCode findByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return botActionCodeMapper.selectOne(new LambdaQueryWrapper<BotActionCode>()
                .eq(BotActionCode::getTaskId, taskId)
                .last("limit 1"));
    }

    private BotActionCode requireByCode(String actionCode) {
        String normalized = normalizeActionCode(actionCode);
        BotActionCode code = botActionCodeMapper.selectOne(new LambdaQueryWrapper<BotActionCode>()
                .eq(BotActionCode::getActionCode, normalized)
                .last("limit 1"));
        if (code == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Action code not found");
        }
        return code;
    }

    private String nextUniqueCode() {
        for (int i = 0; i < 20; i++) {
            String code = nextCode();
            Long count = botActionCodeMapper.selectCount(new LambdaQueryWrapper<BotActionCode>()
                    .eq(BotActionCode::getActionCode, code));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to generate action code");
    }

    private String nextCode() {
        StringBuilder code = new StringBuilder("CP-");
        for (int i = 0; i < 5; i++) {
            code.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)]);
        }
        return code.toString();
    }

    private String normalizeActionCode(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Action code is required");
        }
        return actionCode.trim().toUpperCase();
    }
}
