package com.codepliot.model;

/**
 * LLM工具调用对象。
 * <p>表示大语言模型在对话过程中发起的一次工具（Function）调用请求。</p>
 */
public record LlmToolCall(
        /** 本次工具调用的唯一标识 */
        String id,
        /** 被调用的工具/函数名称 */
        String name,
        /** 工具调用的参数（JSON格式字符串） */
        String arguments
) {
}
