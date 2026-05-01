package com.codepliot.service.agent.tool;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentContext;
import com.codepliot.model.RetrievedCodeChunk;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.ToolResult;
import com.codepliot.service.index.lucene.LuceneCodeSearchService;
import com.codepliot.service.index.rank.CodeRanker;
import com.codepliot.service.index.QueryRewriter;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class SearchRelevantCodeTool implements AgentTool {

    private static final int DEFAULT_TOP_K = 10;
    private static final int SEARCH_CANDIDATE_LIMIT = 40;

    private final LuceneCodeSearchService luceneCodeSearchService;
    private final CodeRanker codeRanker;
    private final QueryRewriter queryRewriter;

    public SearchRelevantCodeTool(LuceneCodeSearchService luceneCodeSearchService,
                                  CodeRanker codeRanker,
                                  QueryRewriter queryRewriter) {
        this.luceneCodeSearchService = luceneCodeSearchService;
        this.codeRanker = codeRanker;
        this.queryRewriter = queryRewriter;
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
        return "Search Relevant Code";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        String query = buildQuery(context.issueTitle(), context.issueDescription());
        List<RetrievedCodeChunk> luceneHits = luceneCodeSearchService.search(
                context.projectId(),
                query,
                SEARCH_CANDIDATE_LIMIT
        );
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
                "candidateCount", luceneHits.size(),
                "hitCount", rerankedHits.size(),
                "chunks", chunkSummaries
        ));
    }

    private String buildQuery(String issueTitle, String issueDescription) {
        return queryRewriter.rewrite(issueTitle, issueDescription);
    }
}
