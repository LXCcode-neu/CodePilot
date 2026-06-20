package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知操作令牌实体，对应数据库表 notification_action_token。
 * <p>用于通知消息中的安全操作验证，用户通过携带令牌的链接执行确认、拒绝等操作。</p>
 */
@Data
@TableName("notification_action_token")
@EqualsAndHashCode(callSuper = true)
public class NotificationActionToken extends BaseEntity {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 关联的 Issue 事件ID */
    @TableField("issue_event_id")
    private Long issueEventId;

    /** 关联的代理任务ID */
    @TableField("task_id")
    private Long taskId;

    /** 关联的补丁记录ID */
    @TableField("patch_id")
    private Long patchId;

    /** 操作类型（如 confirm、reject、review 等） */
    @TableField("action_type")
    private String actionType;

    /** 令牌哈希值，用于安全校验 */
    @TableField("token_hash")
    private String tokenHash;

    /** 状态（如 pending、used、expired） */
    @TableField("status")
    private String status;

    /** 过期时间 */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /** 使用时间 */
    @TableField("used_at")
    private LocalDateTime usedAt;
}
