package com.codepliot.agent.tool.impl;

import com.codepliot.agent.context.AgentContext;
import com.codepliot.agent.tool.AgentTool;
import com.codepliot.agent.tool.ToolResult;
import com.codepliot.index.dto.RetrievedCodeChunk;
import com.codepliot.index.lucene.LuceneCodeSearchService;
import com.codepliot.index.rank.CodeRanker;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.trace.entity.AgentStepType;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 基于 Lucene + 重排规则的相关代码检索工具。
 */
@Component
@Order(30)
public class SearchRelevantCodeTool implements AgentTool {

    private static final int DEFAULT_TOP_K = 10;

    private final LuceneCodeSearchService luceneCodeSearchService;
    private final CodeRanker codeRanker;

    public SearchRelevantCodeTool(LuceneCodeSearchService luceneCodeSearchService, CodeRanker codeRanker) {
        this.luceneCodeSearchService = luceneCodeSearchService;
        this.codeRanker = codeRanker;
    }

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.RETRIEVING;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.SEARCH_RELEVANT_CODE;
    }

    @Override
    public String stepName() {
        return "检索相关代码";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        String query = buildQuery(context.issueTitle(), context.issueDescription());
        List<RetrievedCodeChunk> luceneHits = luceneCodeSearchService.search(context.projectId(), query, DEFAULT_TOP_K);
        List<RetrievedCodeChunk> rerankedHits = codeRanker.rerank(query, luceneHits, DEFAULT_TOP_K);
        context.updateRetrievedChunks(rerankedHits);

        List<Map<String, Object>> chunkSummaries = rerankedHits.stream()
                .map(chunk -> Map.<String, Object>of(
                        "filePath", chunk.filePath() == null ? "" : chunk.filePath(),
                        "language", chunk.language() == null ? "" : chunk.language(),
                        "symbolName", chunk.symbolName() == null ? "" : chunk.symbolName(),
                        "score", chunk.score(),
                        "finalScore", chunk.finalScore()
                ))
                .toList();

        return ToolResult.success("relevant code search completed", Map.of(
                "query", query,
                "topK", DEFAULT_TOP_K,
                "hitCount", rerankedHits.size(),
                "chunks", chunkSummaries
        ));
    }

    private String buildQuery(String issueTitle, String issueDescription) {
        String title = issueTitle == null ? "" : issueTitle.trim();
        String description = issueDescription == null ? "" : issueDescription.trim();
        return (title + " " + description).trim();
    }
}
