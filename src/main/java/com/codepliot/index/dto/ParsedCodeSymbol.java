package com.codepliot.index.dto;

import com.codepliot.index.detect.LanguageType;
import com.codepliot.index.enums.CodeSymbolType;

/**
 * 符号解析结果 DTO。
 * 用于承接“解析完成但尚未落库”的符号级结果。
 */
public record ParsedCodeSymbol(
        Long projectId,
        LanguageType language,
        String filePath,
        CodeSymbolType symbolType,
        String symbolName,
        String parentSymbol,
        String signature,
        String annotations,
        String routePath,
        String importText,
        Integer startLine,
        Integer endLine,
        String content
) {
}
