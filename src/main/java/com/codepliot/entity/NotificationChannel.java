package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("notification_channel")
@EqualsAndHashCode(callSuper = true)
public class NotificationChannel extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("channel_type")
    private String channelType;

    @TableField("channel_name")
    private String channelName;

    @TableField("webhook_url_encrypted")
    private String webhookUrlEncrypted;

    @TableField("enabled")
    private Boolean enabled;
}
