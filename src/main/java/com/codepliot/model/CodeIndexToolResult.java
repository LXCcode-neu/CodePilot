package com.codepliot.model;

import com.codepliot.service.LanguageType;
import java.util.Map;
/**
 * CodeIndexToolResult 模型类，用于承载流程中的数据结构。
 */
public record CodeIndexToolResult(
        int fileCount,
        int symbolCount,
        int indexDocCount,
        int warningCount,
        Map<LanguageType, Integer> languageStats
) {
}

