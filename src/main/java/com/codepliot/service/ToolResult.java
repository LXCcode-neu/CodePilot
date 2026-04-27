package com.codepliot.service;
/**
 * ToolResult 服务类，负责封装业务流程和领域能力。
 */
public record ToolResult(
        boolean success,
        String message,
        Object data
) {
/**
 * 执行 success 相关逻辑。
 */
public static ToolResult success(String message, Object data) {
        return new ToolResult(true, message, data);
    }
/**
 * 执行 failure 相关逻辑。
 */
public static ToolResult failure(String message, Object data) {
        return new ToolResult(false, message, data);
    }
}

