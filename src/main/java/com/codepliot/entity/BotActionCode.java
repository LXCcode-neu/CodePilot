package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("bot_action_code")
@EqualsAndHashCode(callSuper = true)
public class BotActionCode extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("issue_event_id")
    private Long issueEventId;

    @TableField("task_id")
    private Long taskId;

    @TableField("patch_id")
    private Long patchId;

    @TableField("channel_type")
    private String channelType;

    @TableField("chat_id")
    private String chatId;

    @TableField("action_code")
    private String actionCode;

    @TableField("status")
    private String status;

    @TableField("last_message_id")
    private String lastMessageId;

    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
