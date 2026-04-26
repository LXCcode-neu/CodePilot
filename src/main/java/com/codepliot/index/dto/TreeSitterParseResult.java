package com.codepliot.index.dto;

import com.codepliot.index.detect.LanguageType;

/**
 * Tree-sitter 解析结果 DTO。
 * 当前阶段只定义统一结果模型，预留 astObject 供后续真实解析器接入。
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
