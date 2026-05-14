package com.codepliot.service;

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

@Service
public class BotActionCodeService {

    private static final char[] CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private final BotActionCodeMapper botActionCodeMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public BotActionCodeService(BotActionCodeMapper botActionCodeMapper) {
        this.botActionCodeMapper = botActionCodeMapper;
    }

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
    public void markPrCreated(BotActionCode code) {
        code.setStatus(BotActionStatus.PR_CREATED.name());
        botActionCodeMapper.updateById(code);
    }

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
