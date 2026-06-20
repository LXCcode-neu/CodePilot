package com.codepliot.model;

import java.util.List;

/**
 * 补丁差异片段（Hunk）。
 * <p>表示文件diff中的一个变更块，包含变更位置的行号范围和具体的差异行。</p>
 */
public record PatchDiffHunk(
        /** 片段头部信息（如 @@ -10,5 +10,7 @@） */
        String header,
        /** 原文件起始行号 */
        Integer oldStart,
        /** 原文件涉及行数 */
        Integer oldLineCount,
        /** 新文件起始行号 */
        Integer newStart,
        /** 新文件涉及行数 */
        Integer newLineCount,
        /** 该片段内的差异行列表 */
        List<PatchDiffLine> lines
) {
}
