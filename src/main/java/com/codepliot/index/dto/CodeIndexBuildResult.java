package com.codepliot.index.dto;

import com.codepliot.index.detect.LanguageType;
import java.util.Map;

public record CodeIndexBuildResult(
        int fileCount,
        int symbolCount,
        int warningCount,
        Map<LanguageType, Integer> languageStats
) {
}
