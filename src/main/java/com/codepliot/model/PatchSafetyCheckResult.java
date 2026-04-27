package com.codepliot.model;

import java.util.List;

/**
 * Patch 安全检查结果。
 *
 * <p>用于承载 patch 安全策略对当前修改建议的扫描结论，便于落库和前端展示。
 */
public record PatchSafetyCheckResult(
        boolean emptyPatch,
        boolean sensitiveFileModified,
        boolean largeDeletionSuspected,
        boolean permissionBypassRisk,
        boolean configFileModified,
        boolean crossLanguageWideModification,
        int touchedFileCount,
        int touchedLanguageCount,
        int addedLineCount,
        int removedLineCount,
        List<String> touchedFiles,
        List<String> touchedLanguages,
        List<String> riskItems,
        String summary
) {

    /**
     * 判断当前 patch 是否存在需要人工关注的风险信号。
     */
    public boolean requiresAttention() {
        return emptyPatch
                || sensitiveFileModified
                || largeDeletionSuspected
                || permissionBypassRisk
                || configFileModified
                || crossLanguageWideModification;
    }
}
