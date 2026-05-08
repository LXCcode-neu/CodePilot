package com.codepliot.search;

import com.codepliot.search.config.CodeSearchProperties;
import com.codepliot.search.dto.CodeSearchResult;
import com.codepliot.search.dto.GrepMatch;
import com.codepliot.search.dto.SearchRequest;
import com.codepliot.search.grep.GrepSearchService;
import com.codepliot.search.grep.GrepSearchService.GrepSearchResponse;
import com.codepliot.search.read.CodeReadService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Simple one-shot grep facade.
 *
 * <p>Multi-round search decisions now live in AgenticCodeSearchService. This
 * facade only executes the provided request and turns raw grep matches into
 * readable snippets.
 */
@Service
public class GrepCodeSearchFacade implements CodeSearchFacade {

    private final GrepSearchService grepSearchService;
    private final CodeReadService codeReadService;
    private final CodeSearchProperties properties;

    public GrepCodeSearchFacade(GrepSearchService grepSearchService,
                                CodeReadService codeReadService,
                                CodeSearchProperties properties) {
        this.grepSearchService = grepSearchService;
        this.codeReadService = codeReadService;
        this.properties = properties;
    }

    @Override
    public List<CodeSearchResult> search(SearchRequest request) {
        if (request == null) {
            return List.of();
        }

        GrepSearchResponse response = grepSearchService.searchWithStatus(request);
        if (!response.success()) {
            return List.of(buildReasonResult(response.message()));
        }

        return response.matches().stream()
                .map(match -> toSearchResult(request, match))
                .filter(result -> result != null)
                .limit(resolveMaxResults(request))
                .toList();
    }

    private CodeSearchResult toSearchResult(SearchRequest request, GrepMatch match) {
        try {
            var snippet = codeReadService.readSnippet(
                    request.getRepoPath(),
                    match.getFilePath(),
                    match.getLineNumber() == null ? 1 : match.getLineNumber(),
                    resolveContextBeforeLines(request),
                    resolveContextAfterLines(request)
            );

            CodeSearchResult result = new CodeSearchResult();
            result.setFilePath(snippet.getFilePath());
            result.setStartLine(snippet.getStartLine());
            result.setEndLine(snippet.getEndLine());
            result.setScore(0.0d);
            result.setReason("grep matched '" + nullToEmpty(match.getQuery()) + "' at line " + nullToEmpty(match.getLineNumber()));
            result.setContentWithLineNumbers(snippet.getContentWithLineNumbers());
            return result;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private CodeSearchResult buildReasonResult(String reason) {
        CodeSearchResult result = new CodeSearchResult();
        result.setScore(0.0d);
        result.setReason(reason == null ? "" : reason);
        result.setContentWithLineNumbers("");
        return result;
    }

    private int resolveMaxResults(SearchRequest request) {
        return request.getMaxResults() == null || request.getMaxResults() <= 0
                ? properties.getMaxResults()
                : request.getMaxResults();
    }

    private int resolveContextBeforeLines(SearchRequest request) {
        return request.getContextBeforeLines() == null || request.getContextBeforeLines() < 0
                ? properties.getContextBeforeLines()
                : request.getContextBeforeLines();
    }

    private int resolveContextAfterLines(SearchRequest request) {
        return request.getContextAfterLines() == null || request.getContextAfterLines() < 0
                ? properties.getContextAfterLines()
                : request.getContextAfterLines();
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
