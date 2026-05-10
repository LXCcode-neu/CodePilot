package com.codepliot.model;

/**
 * Patch 验证命令执行结果。
 */
public record PatchVerificationCommandResult(
        String name,
        String command,
        String workingDirectory,
        int exitCode,
        boolean passed,
        boolean timedOut,
        String outputSummary
) {
}
