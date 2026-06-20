package com.codepliot.service.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codepliot.entity.NotificationActionToken;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.NotificationActionType;
import com.codepliot.repository.NotificationActionTokenMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知操作令牌服务。
 * <p>
 * 管理通知中可执行操作的安全令牌，支持：
 * <ul>
 *   <li>生成一次性操作令牌（如通知中的"忽略"或"执行修复"按钮）</li>
 *   <li>验证并领取（claim）令牌，确保每个令牌只能使用一次</li>
 *   <li>令牌使用 SHA-256 哈希存储，原始令牌仅返回给调用方</li>
 * </ul>
 * <p>
 * 令牌默认有效期为 24 小时，过期后不可使用。
 */
@Service
public class NotificationActionTokenService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_USED = "USED";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final NotificationActionTokenMapper notificationActionTokenMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public NotificationActionTokenService(NotificationActionTokenMapper notificationActionTokenMapper) {
        this.notificationActionTokenMapper = notificationActionTokenMapper;
    }

    /**
     * 创建 Issue 操作令牌。
     * <p>
     * 生成安全随机令牌并存储其哈希值，原始令牌仅通过返回值传递给调用方。
     *
     * @param userId       用户 ID
     * @param issueEventId Issue 事件 ID
     * @param actionType   操作类型（如 IGNORE、RUN_REPAIR）
     * @return 原始令牌字符串
     */
    @Transactional
    public String createIssueActionToken(Long userId, Long issueEventId, NotificationActionType actionType) {
        NotificationActionToken token = new NotificationActionToken();
        String rawToken = generateRawToken();
        token.setUserId(userId);
        token.setIssueEventId(issueEventId);
        token.setActionType(actionType.name());
        token.setTokenHash(hash(rawToken));
        token.setStatus(STATUS_PENDING);
        token.setExpiresAt(LocalDateTime.now().plus(DEFAULT_TTL));
        token.setUsedAt(null);
        notificationActionTokenMapper.insert(token);
        return rawToken;
    }

    /**
     * 验证并领取操作令牌。
     * <p>
     * 使用乐观锁确保令牌只能被领取一次，已使用或已过期的令牌将被拒绝。
     *
     * @param rawToken   原始令牌字符串
     * @param actionType 期望的操作类型
     * @return 已领取的令牌实体
     * @throws BusinessException 令牌不存在、已使用或已过期时抛出异常
     */
    @Transactional
    public NotificationActionToken claim(String rawToken, NotificationActionType actionType) {
        String tokenHash = hash(requireToken(rawToken));
        NotificationActionToken token = notificationActionTokenMapper.selectOne(new LambdaQueryWrapper<NotificationActionToken>()
                .eq(NotificationActionToken::getTokenHash, tokenHash)
                .eq(NotificationActionToken::getActionType, actionType.name())
                .last("limit 1"));
        if (token == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Notification action is not found");
        }
        if (!STATUS_PENDING.equals(token.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Notification action has already been handled");
        }
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Notification action has expired");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = notificationActionTokenMapper.update(null, new LambdaUpdateWrapper<NotificationActionToken>()
                .eq(NotificationActionToken::getId, token.getId())
                .eq(NotificationActionToken::getStatus, STATUS_PENDING)
                .set(NotificationActionToken::getStatus, STATUS_USED)
                .set(NotificationActionToken::getUsedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Notification action has already been handled");
        }
        token.setStatus(STATUS_USED);
        token.setUsedAt(now);
        return token;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String requireToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Notification action token is required");
        }
        return rawToken.trim();
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to hash notification action token");
        }
    }
}
