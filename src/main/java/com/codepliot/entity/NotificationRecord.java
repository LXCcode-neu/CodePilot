package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("notification_record")
@EqualsAndHashCode(callSuper = true)
public class NotificationRecord extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("channel_id")
    private Long channelId;

    @TableField("event_type")
    private String eventType;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("sent_at")
    private LocalDateTime sentAt;
}
