package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机器人操作码实体，对应数据库表 bot_action_code。
 * <p>用于存储通知消息中的交互操作码，用户通过回复操作码来触发对应的任务动作。</p>
 */
@Data
@TableName("bot_action_code")
@EqualsAndHashCode(callSuper = true)
public class BotActionCode extends BaseEntity {

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

    /** 通知渠道类型（如 telegram、discord 等） */
    @TableField("channel_type")
    private String channelType;

    /** 聊天会话ID */
    @TableField("chat_id")
    private String chatId;

    /** 操作码，用户回复此码触发对应动作 */
    @TableField("action_code")
    private String actionCode;

    /** 状态（如 pending、used、expired） */
    @TableField("status")
    private String status;

    /** 最后一条消息ID，用于消息回复定位 */
    @TableField("last_message_id")
    private String lastMessageId;

    /** 过期时间 */
    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
