package com.codepliot.model;

import java.util.List;

public record PatchDiffHunk(
        String header,
        Integer oldStart,
        Integer oldLineCount,
        Integer newStart,
        Integer newLineCount,
        List<PatchDiffLine> lines
) {
}
