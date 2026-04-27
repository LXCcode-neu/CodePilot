package com.codepliot.model;

import com.codepliot.service.LanguageType;
/**
 * TreeSitterParseResult 模型类，用于承载流程中的数据结构。
 */
public record TreeSitterParseResult(
        LanguageType language,
        String filePath,
        String relativePath,
        String sourceCode,
        boolean success,
        String errorMessage,
        Object astObject
) {
}

