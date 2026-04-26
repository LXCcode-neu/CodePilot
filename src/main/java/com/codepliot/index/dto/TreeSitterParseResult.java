package com.codepliot.index.dto;

import com.codepliot.index.detect.LanguageType;

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
