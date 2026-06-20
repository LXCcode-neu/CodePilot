package com.codepliot.model;

/**
 * 补丁审核发现项。
 * <p>表示AI审核补丁时发现的一个具体问题或风险点。</p>
 */
public record PatchReviewFinding(
        /** 严重程度（如 HIGH、MEDIUM、LOW） */
        String severity,
        /** 问题所在的文件路径 */
        String filePath,
        /** 问题描述信息 */
        String message
) {
}
