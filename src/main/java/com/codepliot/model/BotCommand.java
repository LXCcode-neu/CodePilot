package com.codepliot.model;

/**
 * 机器人命令记录。
 * <p>表示用户通过消息通道发送给机器人的指令信息。</p>
 *
 * @param type       命令类型
 * @param actionCode 动作编码，用于关联具体的执行上下文
 * @param rawText    原始文本内容
 */
public record BotCommand(
        BotCommandType type,
        String actionCode,
        String rawText
) {
}
