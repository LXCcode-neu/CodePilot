package com.codepliot.index.dto;

import com.codepliot.index.detect.LanguageType;

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
