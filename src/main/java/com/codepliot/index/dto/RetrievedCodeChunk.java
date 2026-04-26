package com.codepliot.index.dto;

/**
 * 多语言代码检索结果片段。
 * 保存 Lucene 原始分与重排后的最终分，供后续 Agent Tool 复用。
 */
public record RetrievedCodeChunk(
        String filePath,
        String language,
        String symbolType,
        String symbolName,
        String parentSymbol,
        String routePath,
        Integer startLine,
        Integer endLine,
        String content,
        double score,
        double finalScore
) {

    public RetrievedCodeChunk withFinalScore(double finalScore) {
        return new RetrievedCodeChunk(
                filePath,
                language,
                symbolType,
                symbolName,
                parentSymbol,
                routePath,
                startLine,
                endLine,
                content,
                score,
                finalScore
        );
    }
}
