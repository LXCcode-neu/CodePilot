package com.codepliot.model;

import com.codepliot.entity.NotificationChannel;
import java.time.LocalDateTime;

/**
 * 通知渠道视图对象（VO）。
 * <p>用于向前端展示通知渠道配置信息，其中Webhook地址经过脱敏处理。</p>
 */
public record NotificationChannelVO(
        /** 渠道记录ID */
        Long id,
        /** 渠道类型（如 FEISHU、WE_COM） */
        String channelType,
        /** 渠道名称 */
        String channelName,
        /** Webhook地址脱敏后的掩码字符串 */
        String webhookMasked,
        /** 是否启用 */
        Boolean enabled,
        /** 创建时间 */
        LocalDateTime createdAt,
        /** 更新时间 */
        LocalDateTime updatedAt
) {
    /**
     * 从实体对象转换为视图对象。
     *
     * @param channel        通知渠道实体
     * @param webhookMasked  脱敏后的Webhook地址
     * @return 视图对象
     */
    public static NotificationChannelVO from(NotificationChannel channel, String webhookMasked) {
        return new NotificationChannelVO(
                channel.getId(),
                channel.getChannelType(),
                channel.getChannelName(),
                webhookMasked,
                channel.getEnabled(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}
