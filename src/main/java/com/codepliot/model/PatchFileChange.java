package com.codepliot.model;

import java.util.List;

/**
 * 补丁文件变更对象。
 * <p>表示一次补丁中单个文件的完整变更信息，包含文件路径、增删行数统计和详细的diff片段。</p>
 */
public record PatchFileChange(
        /** 变更前的文件路径（重命名场景使用） */
        String oldPath,
        /** 变更后的文件路径（重命名场景使用） */
        String newPath,
        /** 当前文件路径 */
        String filePath,
        /** 新增行数 */
        Integer addedLines,
        /** 删除行数 */
        Integer removedLines,
        /** 该文件的diff片段（Hunk）列表 */
        List<PatchDiffHunk> hunks
) {
}
