package com.codepliot.model;

import java.util.List;

/**
 * LLM工具对话响应对象。
 * <p>封装大语言模型一次对话交互的返回结果，包含文本回复内容、
 * 结束原因以及可能的工具调用请求列表。</p>
 */
public record LlmToolChatResponse(
        /** 模型回复的文本内容 */
        String content,
        /** 对话结束原因（如 stop、tool_calls 等） */
        String finishReason,
        /** 模型请求的工具调用列表（可能为空） */
        List<LlmToolCall> toolCalls
) {

    public LlmToolChatResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
