package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知发送记录实体，对应数据库表 notification_record。
 * <p>记录每次通知的发送详情，包括发送状态和错误信息，便于排查问题。</p>
 */
@Data
@TableName("notification_record")
@EqualsAndHashCode(callSuper = true)
public class NotificationRecord extends BaseEntity {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 关联的通知渠道ID */
    @TableField("channel_id")
    private Long channelId;

    /** 事件类型（如 issue_opened、patch_generated 等） */
    @TableField("event_type")
    private String eventType;

    /** 通知标题 */
    @TableField("title")
    private String title;

    /** 通知正文内容 */
    @TableField("content")
    private String content;

    /** 发送状态（如 success、failed） */
    @TableField("status")
    private String status;

    /** 发送失败时的错误信息 */
    @TableField("error_message")
    private String errorMessage;

    /** 发送时间 */
    @TableField("sent_at")
    private LocalDateTime sentAt;
}
