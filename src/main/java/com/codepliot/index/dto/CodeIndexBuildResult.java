package com.codepliot.index.dto;

import com.codepliot.index.detect.LanguageType;
import java.util.Map;

/**
 * 索引构建结果 DTO。
 * 保存一次索引构建执行后的统计摘要。
 */
public record CodeIndexBuildResult(
        int fileCount,
        int symbolCount,
        int warningCount,
        Map<LanguageType, Integer> languageStats
) {
}
