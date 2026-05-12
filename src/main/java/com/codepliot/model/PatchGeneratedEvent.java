package com.codepliot.model;

public record PatchGeneratedEvent(
        Long taskId,
        Long patchId,
        boolean success,
        String reason
) {
}
