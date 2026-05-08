package com.codepliot.search;

import com.codepliot.search.dto.GrepMatch;
import com.codepliot.search.dto.SearchRequest;
import com.codepliot.search.grep.GrepSearchService;
import com.codepliot.search.grep.GrepSearchService.GrepSearchResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Executes raw text search against a repository.
 */
@Component
public class GrepTool {

    private final GrepSearchService grepSearchService;

    public GrepTool(GrepSearchService grepSearchService) {
        this.grepSearchService = grepSearchService;
    }

    public GrepSearchResponse execute(String repoPath,
                                      String query,
                                      List<String> globPatterns,
                                      boolean regexEnabled,
                                      int maxResults) {
        SearchRequest request = new SearchRequest();
        request.setRepoPath(repoPath);
        request.setQuery(query);
        request.setGlobPatterns(globPatterns == null ? List.of() : globPatterns);
        request.setRegexEnabled(regexEnabled);
        request.setMaxResults(maxResults);
        return grepSearchService.searchWithStatus(request);
    }

    public record GrepObservation(boolean success, String message, List<GrepMatch> matches) {
    }
}
