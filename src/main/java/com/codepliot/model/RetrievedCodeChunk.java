package com.codepliot.model;
/**
 * RetrievedCodeChunk 模型类，用于承载流程中的数据结构。
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
/**
 * 执行 withFinalScore 相关逻辑。
 */
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

