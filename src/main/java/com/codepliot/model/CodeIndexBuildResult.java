package com.codepliot.model;

import com.codepliot.service.index.LanguageType;
import java.util.Map;
/**
 * CodeIndexBuildResult 模型类，用于承载流程中的数据结构。
 */
public record CodeIndexBuildResult(
        int fileCount,
        int symbolCount,
        int warningCount,
        Map<LanguageType, Integer> languageStats
) {
}

