package com.codepliot.service.agent.tool;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentContext;
import com.codepliot.model.RetrievedCodeChunk;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.ToolResult;
import com.codepliot.service.index.lucene.LuceneCodeSearchService;
import com.codepliot.service.index.rank.CodeRanker;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class SearchRelevantCodeTool implements AgentTool {

    private static final int DEFAULT_TOP_K = 10;
    private static final int SEARCH_CANDIDATE_LIMIT = 40;

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "from", "into", "that", "this", "when", "then", "than",
            "does", "doesnt", "cant", "cannot", "will", "should", "need", "after", "before",
            "have", "has", "had", "there", "their", "them", "they", "user", "users", "please",
            "check", "ensure", "keep", "show", "move", "issue"
    );

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
        List<String> titleKeywords = extractKeywords(issueTitle);
        List<String> descriptionKeywords = extractKeywords(issueDescription);

        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(titleKeywords);
        merged.addAll(descriptionKeywords);

        if (merged.isEmpty()) {
            String title = issueTitle == null ? "" : issueTitle.trim();
            String description = issueDescription == null ? "" : issueDescription.trim();
            return (title + " " + description).trim();
        }

        return merged.stream()
                .limit(12)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private List<String> extractKeywords(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        String[] tokens = value.toLowerCase().split("[^a-z0-9_./-]+");
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (token.length() <= 1 || STOPWORDS.contains(token)) {
                continue;
            }
            ordered.add(token);
        }
        return new ArrayList<>(ordered);
    }
}
