package com.codepliot.service.agent.tool;

import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.AgentStepType;
import com.codepliot.model.AgentContext;
import com.codepliot.model.RetrievedCodeChunk;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.ToolResult;
import com.codepliot.service.index.lucene.LuceneCodeSearchService;
import com.codepliot.service.index.rank.CodeRanker;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
/**
 * SearchRelevantCodeTool 服务类，负责封装业务流程和领域能力。
 */
@Component
@Order(30)
public class SearchRelevantCodeTool implements AgentTool {

    private static final int DEFAULT_TOP_K = 10;

    private final LuceneCodeSearchService luceneCodeSearchService;
    private final CodeRanker codeRanker;
/**
 * 创建 SearchRelevantCodeTool 实例。
 */
public SearchRelevantCodeTool(LuceneCodeSearchService luceneCodeSearchService, CodeRanker codeRanker) {
        this.luceneCodeSearchService = luceneCodeSearchService;
        this.codeRanker = codeRanker;
    }
    /**
     * 执行 taskStatus 相关逻辑。
     */
@Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.RETRIEVING;
    }
    /**
     * 执行 stepType 相关逻辑。
     */
@Override
    public AgentStepType stepType() {
        return AgentStepType.SEARCH_RELEVANT_CODE;
    }
    /**
     * 执行 stepName 相关逻辑。
     */
@Override
    public String stepName() {
        return "ش";
    }
    /**
     * 执行 execute 相关逻辑。
     */
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
/**
 * 构建Query相关逻辑。
 */
private String buildQuery(String issueTitle, String issueDescription) {
        String title = issueTitle == null ? "" : issueTitle.trim();
        String description = issueDescription == null ? "" : issueDescription.trim();
        return (title + " " + description).trim();
    }
}