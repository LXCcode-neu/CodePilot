package com.codepliot.model;

import com.codepliot.service.index.LanguageType;
/**
 * ParsedCodeFile 模型类，用于承载流程中的数据结构。
 */
public record ParsedCodeFile(
        Long projectId,
        String filePath,
        LanguageType language,
        String packageName,
        String moduleName,
        String className,
        String contentHash,
        Long size,
        String parseStatus,
        String parseError
) {
}

