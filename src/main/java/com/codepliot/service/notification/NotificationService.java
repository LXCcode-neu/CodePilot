package com.codepliot.service.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.NotificationChannel;
import com.codepliot.entity.NotificationRecord;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.NotificationChannelCreateRequest;
import com.codepliot.model.NotificationChannelType;
import com.codepliot.model.NotificationChannelVO;
import com.codepliot.model.NotificationEventType;
import com.codepliot.model.NotificationMessage;
import com.codepliot.model.NotificationSendResult;
import com.codepliot.repository.NotificationChannelMapper;
import com.codepliot.repository.NotificationRecordMapper;
import com.codepliot.utils.ApiKeyCryptoUtils;
import com.codepliot.utils.SecurityUtils;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationChannelMapper notificationChannelMapper;
    private final NotificationRecordMapper notificationRecordMapper;
    private final ApiKeyCryptoUtils apiKeyCryptoUtils;
    private final Map<NotificationChannelType, NotificationSender> senderMap;

    public NotificationService(NotificationChannelMapper notificationChannelMapper,
                               NotificationRecordMapper notificationRecordMapper,
                               ApiKeyCryptoUtils apiKeyCryptoUtils,
                               List<NotificationSender> senders) {
        this.notificationChannelMapper = notificationChannelMapper;
        this.notificationRecordMapper = notificationRecordMapper;
        this.apiKeyCryptoUtils = apiKeyCryptoUtils;
        this.senderMap = new EnumMap<>(NotificationChannelType.class);
        senders.forEach(sender -> senderMap.put(sender.type(), sender));
    }

    @Transactional
    public NotificationChannelVO create(NotificationChannelCreateRequest request) {
        NotificationChannelType channelType = parseChannelType(request.channelType());
        String webhookUrl = requireText(request.webhookUrl(), "webhookUrl");

        NotificationChannel channel = new NotificationChannel();
        channel.setUserId(SecurityUtils.getCurrentUserId());
        channel.setChannelType(channelType.name());
        channel.setChannelName(defaultChannelName(request.channelName(), channelType));
        channel.setWebhookUrlEncrypted(apiKeyCryptoUtils.encrypt(webhookUrl.trim()));
        channel.setEnabled(true);
        notificationChannelMapper.insert(channel);
        return NotificationChannelVO.from(channel, maskWebhook(webhookUrl));
    }

    public List<NotificationChannelVO> listCurrentUserChannels() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationChannelMapper.selectList(new LambdaQueryWrapper<NotificationChannel>()
                        .eq(NotificationChannel::getUserId, userId)
                        .orderByDesc(NotificationChannel::getCreatedAt))
                .stream()
                .map(channel -> NotificationChannelVO.from(channel, maskedEncryptedWebhook(channel)))
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        NotificationChannel channel = requireOwnedChannel(id);
        notificationChannelMapper.deleteById(channel.getId());
    }

    @Transactional
    public NotificationSendResult test(Long id) {
        NotificationChannel channel = requireOwnedChannel(id);
        NotificationMessage message = new NotificationMessage(
                "CodePilot 通知测试",
                "如果你收到了这条消息，说明当前通知渠道可用。",
                NotificationEventType.TEST,
                null
        );
        boolean success = sendToChannel(channel, message);
        return new NotificationSendResult(success, success ? "Notification sent" : "Notification failed");
    }

    @Transactional
    public boolean sendToUser(Long userId, NotificationMessage message) {
        List<NotificationChannel> channels = notificationChannelMapper.selectList(
                new LambdaQueryWrapper<NotificationChannel>()
                        .eq(NotificationChannel::getUserId, userId)
                        .eq(NotificationChannel::getEnabled, true)
                        .orderByAsc(NotificationChannel::getCreatedAt));
        boolean success = false;
        for (NotificationChannel channel : channels) {
            success = sendToChannel(channel, message) || success;
        }
        return success;
    }

    private boolean sendToChannel(NotificationChannel channel, NotificationMessage message) {
        NotificationRecord record = new NotificationRecord();
        record.setUserId(channel.getUserId());
        record.setChannelId(channel.getId());
        record.setEventType(message.eventType().name());
        record.setTitle(message.title());
        record.setContent(message.content());
        record.setSentAt(LocalDateTime.now());

        try {
            NotificationSender sender = senderMap.get(parseChannelType(channel.getChannelType()));
            if (sender == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Notification channel sender is not supported");
            }
            sender.send(apiKeyCryptoUtils.decrypt(channel.getWebhookUrlEncrypted()), message);
            record.setStatus("SUCCESS");
            notificationRecordMapper.insert(record);
            return true;
        } catch (RuntimeException exception) {
            record.setStatus("FAILED");
            record.setErrorMessage(safeErrorMessage(exception));
            notificationRecordMapper.insert(record);
            return false;
        }
    }

    private NotificationChannel requireOwnedChannel(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        NotificationChannel channel = notificationChannelMapper.selectOne(new LambdaQueryWrapper<NotificationChannel>()
                .eq(NotificationChannel::getId, id)
                .eq(NotificationChannel::getUserId, userId)
                .last("limit 1"));
        if (channel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Notification channel not found");
        }
        return channel;
    }

    private NotificationChannelType parseChannelType(String value) {
        try {
            return NotificationChannelType.valueOf(requireText(value, "channelType").trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Notification channel type is not supported");
        }
    }

    private String defaultChannelName(String channelName, NotificationChannelType channelType) {
        if (channelName != null && !channelName.isBlank()) {
            return channelName.trim();
        }
        return channelType == NotificationChannelType.FEISHU ? "飞书通知" : "企业微信通知";
    }

    private String maskedEncryptedWebhook(NotificationChannel channel) {
        try {
            return maskWebhook(apiKeyCryptoUtils.decrypt(channel.getWebhookUrlEncrypted()));
        } catch (RuntimeException exception) {
            return "****";
        }
    }

    private String maskWebhook(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return "****";
        }
        String normalized = webhookUrl.trim();
        if (normalized.length() <= 16) {
            return "****";
        }
        return normalized.substring(0, Math.min(32, normalized.length())) + "****"
                + normalized.substring(normalized.length() - 4);
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, name + " is required");
        }
        return value;
    }

    private String safeErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
