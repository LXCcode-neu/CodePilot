package com.codepliot.agent.tool;

/**
 * Agent 工具执行结果。
 */
public record ToolResult(
        boolean success,
        String message,
        Object data
) {

    public static ToolResult success(String message, Object data) {
        return new ToolResult(true, message, data);
    }

    public static ToolResult failure(String message, Object data) {
        return new ToolResult(false, message, data);
    }
}
