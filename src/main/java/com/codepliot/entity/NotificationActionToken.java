package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("notification_action_token")
@EqualsAndHashCode(callSuper = true)
public class NotificationActionToken extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("issue_event_id")
    private Long issueEventId;

    @TableField("task_id")
    private Long taskId;

    @TableField("patch_id")
    private Long patchId;

    @TableField("action_type")
    private String actionType;

    @TableField("token_hash")
    private String tokenHash;

    @TableField("status")
    private String status;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("used_at")
    private LocalDateTime usedAt;
}
