package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知渠道配置实体，对应数据库表 notification_channel。
 * <p>存储用户配置的通知渠道信息，如 Telegram、Discord、企业微信等 Webhook 配置。</p>
 */
@Data
@TableName("notification_channel")
@EqualsAndHashCode(callSuper = true)
public class NotificationChannel extends BaseEntity {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 渠道类型（如 telegram、discord、wecom 等） */
    @TableField("channel_type")
    private String channelType;

    /** 渠道名称，用户自定义的标识 */
    @TableField("channel_name")
    private String channelName;

    /** 加密存储的 Webhook URL */
    @TableField("webhook_url_encrypted")
    private String webhookUrlEncrypted;

    /** 是否启用 */
    @TableField("enabled")
    private Boolean enabled;
}
