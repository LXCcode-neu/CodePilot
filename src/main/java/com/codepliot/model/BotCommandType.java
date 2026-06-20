package com.codepliot.model;

/**
 * 机器人命令类型枚举。
 * <p>定义用户可以向机器人发送的各类指令。</p>
 */
public enum BotCommandType {

    /** 执行修复操作 */
    RUN_FIX,

    /** 忽略当前事件 */
    IGNORE,

    /** 查询当前状态 */
    STATUS,

    /** 确认创建 Pull Request */
    CONFIRM_PR,

    /** 取消当前操作 */
    CANCEL
}
