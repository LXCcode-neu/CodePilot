package com.codepliot.model;

/**
 * 补丁差异行。
 * <p>表示文件diff中的一行变更内容，包含行类型和对应的行号。</p>
 */
public record PatchDiffLine(
        /** 行类型：CONTEXT（上下文/未变更）、ADDED（新增）、REMOVED（删除） */
        String type,
        /** 原文件行号（删除行或上下文行使用） */
        Integer oldLineNumber,
        /** 新文件行号（新增行或上下文行使用） */
        Integer newLineNumber,
        /** 该行的文本内容 */
        String content
) {
}
