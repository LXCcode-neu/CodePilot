package com.codepliot.service.agent.tool;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentContext;
import com.codepliot.model.RetrievedCodeChunk;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.ToolResult;
import com.codepliot.service.index.KeywordExtractor;
import com.codepliot.service.index.RepositoryCodeSearchService;
import com.codepliot.service.index.lucene.LuceneCodeSearchService;
import com.codepliot.service.index.rank.CodeRanker;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private final RepositoryCodeSearchService repositoryCodeSearchService;
    private final CodeRanker codeRanker;

    public SearchRelevantCodeTool(LuceneCodeSearchService luceneCodeSearchService,
                                  RepositoryCodeSearchService repositoryCodeSearchService,
                                  CodeRanker codeRanker) {
        this.luceneCodeSearchService = luceneCodeSearchService;
        this.repositoryCodeSearchService = repositoryCodeSearchService;
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
        return "Search Relevant Code";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        String query = buildQuery(context.issueTitle(), context.issueDescription());
        List<RetrievedCodeChunk> luceneHits = repositoryCodeSearchService.hydrate(
                context.localPath(),
                query,
                luceneCodeSearchService.search(context.projectId(), query, SEARCH_CANDIDATE_LIMIT)
        );
        List<RetrievedCodeChunk> fallbackHits = shouldUseRepositoryFallback(luceneHits)
                ? repositoryCodeSearchService.search(context.localPath(), query, SEARCH_CANDIDATE_LIMIT)
                : List.of();
        List<RetrievedCodeChunk> mergedCandidates = mergeCandidates(luceneHits, fallbackHits);
        List<RetrievedCodeChunk> rerankedHits = codeRanker.rerank(query, mergedCandidates, DEFAULT_TOP_K);
        context.updateRetrievedChunks(rerankedHits);

        List<Map<String, Object>> chunkSummaries = rerankedHits.stream()
                .map(chunk -> Map.<String, Object>of(
                        "filePath", chunk.filePath() == null ? "" : chunk.filePath(),
                        "language", chunk.language() == null ? "" : chunk.language(),
                        "symbolName", chunk.symbolName() == null ? "" : chunk.symbolName(),
                        "score", chunk.score(),
                        "finalScore", chunk.finalScore(),
                        "startLine", chunk.startLine() == null ? "" : chunk.startLine(),
                        "endLine", chunk.endLine() == null ? "" : chunk.endLine()
                ))
                .toList();

        return ToolResult.success("relevant code search completed", Map.of(
                "query", query,
                "topK", DEFAULT_TOP_K,
                "candidateCount", mergedCandidates.size(),
                "luceneCandidateCount", luceneHits.size(),
                "fallbackCandidateCount", fallbackHits.size(),
                "hitCount", rerankedHits.size(),
                "chunks", chunkSummaries
        ));
    }

    private String buildQuery(String issueTitle, String issueDescription) {
        String query = KeywordExtractor.buildQuery(issueTitle, issueDescription);
        if (!query.isBlank()) {
            return query;
        }
        String title = issueTitle == null ? "" : issueTitle.trim();
        String description = issueDescription == null ? "" : issueDescription.trim();
        return (title + " " + description).trim();
    }

    private boolean shouldUseRepositoryFallback(List<RetrievedCodeChunk> luceneHits) {
        return luceneHits == null || luceneHits.size() < 3;
    }

    private List<RetrievedCodeChunk> mergeCandidates(List<RetrievedCodeChunk> luceneHits, List<RetrievedCodeChunk> fallbackHits) {
        LinkedHashMap<String, RetrievedCodeChunk> merged = new LinkedHashMap<>();
        addCandidates(merged, luceneHits);
        addCandidates(merged, fallbackHits);
        return merged.values().stream()
                .sorted(Comparator.comparingDouble(RetrievedCodeChunk::score).reversed()
                        .thenComparing(RetrievedCodeChunk::filePath, Comparator.nullsLast(String::compareTo)))
                .limit(SEARCH_CANDIDATE_LIMIT)
                .toList();
    }

    private void addCandidates(Map<String, RetrievedCodeChunk> merged, List<RetrievedCodeChunk> candidates) {
        if (candidates == null) {
            return;
        }
        for (RetrievedCodeChunk chunk : candidates) {
            if (chunk == null) {
                continue;
            }
            String key = chunk.filePath() == null ? "" : chunk.filePath();
            RetrievedCodeChunk existing = merged.get(key);
            if (existing == null || chunk.score() > existing.score()) {
                merged.put(key, chunk);
            }
        }
    }
}
