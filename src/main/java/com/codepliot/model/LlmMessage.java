package com.codepliot.model;

import java.util.List;

/**
 * LLM对话消息对象。
 * <p>表示与大语言模型交互过程中的一条消息，支持系统消息、用户消息、
 * 助手消息和工具调用结果消息四种角色。</p>
 */
public record LlmMessage(
        /** 消息角色：system（系统）、user（用户）、assistant（助手）、tool（工具结果） */
        String role,
        /** 消息文本内容 */
        String content,
        /** 工具调用结果ID，仅在角色为 tool 时使用 */
        String toolCallId,
        /** 工具调用列表，仅在角色为 assistant 且触发工具调用时使用 */
        List<LlmToolCall> toolCalls
) {

    /**
     * 创建系统消息。
     *
     * @param content 系统提示内容
     * @return 系统消息对象
     */
    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, List.of());
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, List.of());
    }

    public static LlmMessage assistant(String content, List<LlmToolCall> toolCalls) {
        return new LlmMessage("assistant", content, null, toolCalls == null ? List.of() : List.copyOf(toolCalls));
    }

    public static LlmMessage tool(String toolCallId, String content) {
        return new LlmMessage("tool", content, toolCallId, List.of());
    }
}
