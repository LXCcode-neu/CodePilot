package com.codepliot.model;

import com.codepliot.service.LanguageType;
import com.codepliot.model.CodeSymbolType;
/**
 * ParsedCodeSymbol 模型类，用于承载流程中的数据结构。
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

