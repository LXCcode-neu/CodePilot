package com.codepliot.index.dto;

import com.codepliot.index.detect.LanguageType;

/**
 * 文件解析结果 DTO。
 * 用于承接“解析完成但尚未落库”的文件级结果。
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
