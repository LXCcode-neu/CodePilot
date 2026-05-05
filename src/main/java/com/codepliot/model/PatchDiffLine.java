package com.codepliot.model;

public record PatchDiffLine(
        String type,
        Integer oldLineNumber,
        Integer newLineNumber,
        String content
) {
}
