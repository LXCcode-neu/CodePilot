package com.codepliot.index.dto;

import com.codepliot.index.detect.LanguageType;
import com.codepliot.index.enums.CodeSymbolType;

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
