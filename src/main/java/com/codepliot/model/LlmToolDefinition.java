package com.codepliot.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * LLM工具定义对象。
 * <p>描述一个可供大语言模型调用的工具（Function）的元信息，
 * 包括名称、功能描述和参数Schema。</p>
 */
public record LlmToolDefinition(
        /** 工具/函数名称 */
        String name,
        /** 工具功能描述（供模型理解何时调用） */
        String description,
        /** 工具参数的JSON Schema定义 */
        JsonNode parameters
) {
}
