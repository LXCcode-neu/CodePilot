package com.codepliot.model;

import java.util.List;

/**
 * Patch 自动验证结果。
 */
public record PatchVerificationResult(
        boolean skipped,
        boolean patchApplicable,
        boolean passed,
        String summary,
        List<String> detectedTypes,
        List<PatchVerificationCommandResult> commands
) {

    public PatchVerificationResult {
        detectedTypes = detectedTypes == null ? List.of() : List.copyOf(detectedTypes);
        commands = commands == null ? List.of() : List.copyOf(commands);
    }
}
