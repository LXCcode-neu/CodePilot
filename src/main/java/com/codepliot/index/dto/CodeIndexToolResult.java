package com.codepliot.index.dto;

import com.codepliot.index.detect.LanguageType;
import java.util.Map;

/**
 * Agent 工具层使用的统一索引构建结果。
 * 汇总数据库索引结果与 Lucene 本地索引结果，便于直接写入执行步骤输出。
 */
public record CodeIndexToolResult(
        int fileCount,
        int symbolCount,
        int indexDocCount,
        int warningCount,
        Map<LanguageType, Integer> languageStats
) {
}
