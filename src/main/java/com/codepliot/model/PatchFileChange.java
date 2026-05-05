package com.codepliot.model;

import java.util.List;

public record PatchFileChange(
        String oldPath,
        String newPath,
        String filePath,
        Integer addedLines,
        Integer removedLines,
        List<PatchDiffHunk> hunks
) {
}
