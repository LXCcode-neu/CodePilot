package com.codepliot.search;

import com.codepliot.search.config.CodeSearchProperties;
import com.codepliot.search.dto.CodeSearchResult;
import com.codepliot.search.dto.CodeSnippet;
import com.codepliot.search.dto.GrepMatch;
import com.codepliot.search.dto.SearchRequest;
import com.codepliot.search.glob.FileGlobService;
import com.codepliot.search.grep.GrepSearchService;
import com.codepliot.search.grep.GrepSearchService.GrepSearchResponse;
import com.codepliot.search.read.CodeReadService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Claude Code style search facade that coordinates query planning, grep, and read.
 */
@Service
public class GrepCodeSearchFacade implements CodeSearchFacade {

    private static final Logger log = LoggerFactory.getLogger(GrepCodeSearchFacade.class);

    private static final List<String> SPRING_BOOT_FALLBACK_QUERIES = List.of(
            "@RestController",
            "@Controller",
            "@Service",
            "@Repository",
            "@Mapper",
            "@Configuration",
            "@Component"
    );

    private static final Set<String> EXCLUDED_PATH_SEGMENTS = Set.of(
            "/target/",
            "/build/",
            "/dist/",
            "/node_modules/",
            "/.git/",
            "/.idea/",
            "/.vscode/",
            "/logs/",
            "/tmp/"
    );

    private final SearchQueryPlanner searchQueryPlanner;
    private final LlmSearchQueryPlanner llmSearchQueryPlanner;
    private final FileGlobService fileGlobService;
    private final GrepSearchService grepSearchService;
    private final CodeReadService codeReadService;
    private final CodeSearchProperties properties;

    @Autowired
    public GrepCodeSearchFacade(SearchQueryPlanner searchQueryPlanner,
                                LlmSearchQueryPlanner llmSearchQueryPlanner,
                                FileGlobService fileGlobService,
                                GrepSearchService grepSearchService,
                                CodeReadService codeReadService,
                                CodeSearchProperties properties) {
        this.searchQueryPlanner = searchQueryPlanner;
        this.llmSearchQueryPlanner = llmSearchQueryPlanner;
        this.fileGlobService = fileGlobService;
        this.grepSearchService = grepSearchService;
        this.codeReadService = codeReadService;
        this.properties = properties;
    }

    public GrepCodeSearchFacade(SearchQueryPlanner searchQueryPlanner,
                                GrepSearchService grepSearchService,
                                CodeReadService codeReadService,
                                CodeSearchProperties properties) {
        this(searchQueryPlanner, null, null, grepSearchService, codeReadService, properties);
    }

    @Override
    public List<CodeSearchResult> search(SearchRequest request) {
        if (request == null) {
            return List.of(buildReasonResult("SearchRequest must not be null"));
        }

        int maxResults = resolveMaxResults(request);
        if (maxResults <= 0) {
            return List.of();
        }

        SearchPlan searchPlan = planSearch(request);
        List<SearchPlanQuery> plannedQueries = searchPlan.queries();

        Map<String, MatchAccumulator> accumulated = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        if (searchPlan.warning() != null && !searchPlan.warning().isBlank()) {
            warnings.add(searchPlan.warning());
        }

        collectGlobFileCandidates(request, searchPlan, accumulated, warnings);
        for (SearchPlanQuery query : plannedQueries) {
            collectMatches(request, query, accumulated, warnings);
        }

        if (accumulated.isEmpty()) {
            for (String fallbackQuery : SPRING_BOOT_FALLBACK_QUERIES) {
                collectMatches(request, new SearchPlanQuery(fallbackQuery, false, List.of("**/*.java"), "Spring structure fallback"), accumulated, warnings);
                if (accumulated.size() >= maxResults) {
                    break;
                }
            }
        }

        List<MatchAccumulator> rankedMatches = accumulated.values().stream()
                .map(match -> match.withScore(score(match)))
                .sorted(Comparator.comparingDouble(MatchAccumulator::score).reversed()
                        .thenComparing(match -> normalize(match.match().getFilePath()))
                        .thenComparing(match -> match.match().getLineNumber() == null ? Integer.MAX_VALUE : match.match().getLineNumber()))
                .limit(maxResults)
                .toList();

        List<CodeSearchResult> results = new ArrayList<>();
        for (MatchAccumulator match : rankedMatches) {
            CodeSearchResult result = toSearchResult(request, match);
            if (result != null) {
                results.add(result);
            }
        }

        if (results.isEmpty() && !warnings.isEmpty()) {
            return List.of(buildReasonResult(String.join("; ", warnings)));
        }
        return results;
    }

    private void collectMatches(SearchRequest originalRequest,
                                SearchPlanQuery query,
                                Map<String, MatchAccumulator> accumulated,
                                List<String> warnings) {
        try {
            SearchRequest grepRequest = copyForQuery(originalRequest, query);
            GrepSearchResponse response = grepSearchService.searchWithStatus(grepRequest);
            if (!response.success()) {
                warnings.add("Query '" + query.pattern() + "' failed: " + response.message());
                return;
            }
            for (GrepMatch match : response.matches()) {
                if (match == null || shouldExclude(match.getFilePath())) {
                    continue;
                }
                String key = key(match);
                MatchAccumulator existing = accumulated.get(key);
                if (existing == null) {
                    accumulated.put(key, new MatchAccumulator(match, query.pattern(), query.reason(), 1, 0.0d));
                } else {
                    boolean useCandidate = candidateScore(match, query.pattern()) > candidateScore(existing.match(), existing.query());
                    accumulated.put(key, existing.incrementHitCount(match, query.pattern(), query.reason(), useCandidate));
                }
            }
        } catch (RuntimeException exception) {
            log.warn("Search query failed, query={}", query.pattern(), exception);
            warnings.add("Query '" + query.pattern() + "' failed: " + buildErrorMessage(exception));
        }
    }

    private CodeSearchResult toSearchResult(SearchRequest request, MatchAccumulator accumulator) {
        GrepMatch match = accumulator.match();
        try {
            CodeSnippet snippet = codeReadService.readSnippet(
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
            result.setScore(accumulator.score());
            result.setReason(buildReason(accumulator));
            result.setContentWithLineNumbers(snippet.getContentWithLineNumbers());
            return result;
        } catch (RuntimeException exception) {
            log.warn("Failed to read code snippet, filePath={}", match.getFilePath(), exception);
            return null;
        }
    }

    private SearchPlan planSearch(SearchRequest request) {
        LinkedHashSet<String> deterministicTerms = new LinkedHashSet<>();
        String text = joinNonBlank(request.getIssueText(), request.getQuery());
        deterministicTerms.addAll(searchQueryPlanner.plan(text, Math.max(10, resolveMaxResults(request))));
        if (isFocusedQuery(request.getQuery())) {
            deterministicTerms.add(request.getQuery().trim());
        }

        int maxQueries = Math.max(10, Math.min(resolveMaxResults(request), 30));
        SearchPlan planned = llmSearchQueryPlanner == null
                ? SearchPlan.deterministic(toPlanQueries(deterministicTerms, maxQueries), List.of())
                : llmSearchQueryPlanner.plan(request, List.copyOf(deterministicTerms), maxQueries);
        if (!planned.queries().isEmpty() || !planned.fileGlobs().isEmpty()) {
            return planned;
        }

        // Last-resort probes keep the agentic flow alive instead of failing before grep/read can run.
        return SearchPlan.deterministic(List.of(
                new SearchPlanQuery("class ", false, List.of("**/*.java"), "last resort Java class probe"),
                new SearchPlanQuery("@Service", false, List.of("**/*.java"), "last resort Spring service probe"),
                new SearchPlanQuery("@RestController", false, List.of("**/*.java"), "last resort Spring controller probe")
        ), List.of("**/*.java"));
    }

    private List<SearchPlanQuery> toPlanQueries(Set<String> terms, int maxQueries) {
        return terms.stream()
                .filter(query -> query != null && !query.isBlank())
                .limit(maxQueries)
                .map(query -> new SearchPlanQuery(query, false, List.of(), "deterministic issue term"))
                .toList();
    }

    private void collectGlobFileCandidates(SearchRequest request,
                                           SearchPlan searchPlan,
                                           Map<String, MatchAccumulator> accumulated,
                                           List<String> warnings) {
        if (fileGlobService == null) {
            return;
        }
        LinkedHashSet<String> globs = new LinkedHashSet<>();
        globs.addAll(searchPlan.fileGlobs());
        for (SearchPlanQuery query : searchPlan.queries()) {
            globs.addAll(query.globPatterns());
            String classLikeGlob = toClassLikeGlob(query.pattern());
            if (!classLikeGlob.isBlank()) {
                globs.add(classLikeGlob);
            }
        }
        if (globs.isEmpty()) {
            return;
        }

        try {
            int limit = Math.max(resolveMaxResults(request) * 2, 20);
            for (String filePath : fileGlobService.findFiles(request.getRepoPath(), List.copyOf(globs))) {
                if (accumulated.size() >= limit) {
                    break;
                }
                if (shouldExclude(filePath)) {
                    continue;
                }
                GrepMatch match = new GrepMatch();
                match.setFilePath(filePath);
                match.setLineNumber(1);
                match.setColumn(1);
                match.setLineText(extractFileName(filePath));
                match.setQuery("glob");
                accumulated.putIfAbsent(key(match), new MatchAccumulator(match, "glob", "matched file name/path glob", 1, 0.0d));
            }
        } catch (RuntimeException exception) {
            warnings.add("Glob file candidate search failed: " + buildErrorMessage(exception));
        }
    }

    private String toClassLikeGlob(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return "";
        }
        String normalized = pattern.trim();
        if (!normalized.matches("[A-Za-z_][A-Za-z0-9_]{2,80}")) {
            return "";
        }
        if (Character.isUpperCase(normalized.charAt(0)) || normalized.endsWith("Service") || normalized.endsWith("Controller")
                || normalized.endsWith("Mapper") || normalized.endsWith("Repository") || normalized.endsWith("Util")
                || normalized.endsWith("Utils")) {
            return "**/*" + normalized + "*.java";
        }
        return "";
    }

    private SearchRequest copyForQuery(SearchRequest request, SearchPlanQuery query) {
        SearchRequest copy = new SearchRequest();
        copy.setRepoPath(request.getRepoPath());
        copy.setIssueText(request.getIssueText());
        copy.setQuery(query.pattern());
        copy.setMaxResults(Math.max(resolveMaxResults(request) * 4, 40));
        copy.setContextBeforeLines(resolveContextBeforeLines(request));
        copy.setContextAfterLines(resolveContextAfterLines(request));
        copy.setGlobPatterns(mergeGlobPatterns(request.getGlobPatterns(), query.globPatterns()));
        copy.setRegexEnabled(request.isRegexEnabled() || query.regexEnabled());
        return copy;
    }

    private List<String> mergeGlobPatterns(List<String> requestGlobs, List<String> queryGlobs) {
        LinkedHashSet<String> globs = new LinkedHashSet<>();
        if (requestGlobs != null) {
            requestGlobs.stream()
                    .filter(glob -> glob != null && !glob.isBlank())
                    .forEach(globs::add);
        }
        if (queryGlobs != null) {
            queryGlobs.stream()
                    .filter(glob -> glob != null && !glob.isBlank())
                    .forEach(globs::add);
        }
        return List.copyOf(globs);
    }

    private double score(MatchAccumulator accumulator) {
        GrepMatch match = accumulator.match();
        String filePath = normalize(match.getFilePath());
        String fileName = extractFileName(filePath);
        String lineText = normalize(match.getLineText());
        String query = normalize(accumulator.query());
        double score = accumulator.hitCount();

        if (!query.isBlank() && fileName.contains(query)) {
            score += 8.0d;
        }
        if (!query.isBlank() && lineText.contains(query)) {
            score += 5.0d;
        }
        if (isNumericQuery(query) && lineText.contains(query)) {
            score += 15.0d;
        }
        if (filePath.contains("src/main/java/")) {
            score += 4.0d;
        }
        if (filePath.contains("src/test/java/")) {
            score -= 2.0d;
        }
        if (containsAny(filePath, Set.of("/service/", "/mapper/", "/repository/", "/util/", "/utils/"))) {
            score += 4.0d;
        }
        if (containsAny(filePath, Set.of("/controller/"))) {
            score += isApiLikeQuery(query) ? 2.0d : 0.5d;
        }
        if (containsAny(fileName, Set.of("service", "mapper", "repository", "util", "utils"))) {
            score += 3.0d;
        }
        if (fileName.contains("controller")) {
            score += isApiLikeQuery(query) ? 2.0d : 0.5d;
        }
        if (match.getLineNumber() != null && match.getLineNumber() > 0) {
            score += 1.0d / Math.max(1, match.getLineNumber());
        }
        return score;
    }

    private String buildReason(MatchAccumulator accumulator) {
        return "Matched query '" + accumulator.query() + "'"
                + " at line " + nullToEmpty(accumulator.match().getLineNumber())
                + " with " + accumulator.hitCount() + " hit(s) for this file"
                + (accumulator.reason() == null || accumulator.reason().isBlank() ? "" : "; " + accumulator.reason());
    }

    private CodeSearchResult buildReasonResult(String reason) {
        CodeSearchResult result = new CodeSearchResult();
        result.setScore(0.0d);
        result.setReason(reason);
        result.setContentWithLineNumbers("");
        return result;
    }

    private boolean shouldExclude(String filePath) {
        String normalized = "/" + normalize(filePath);
        for (String excludedPath : EXCLUDED_PATH_SEGMENTS) {
            if (normalized.contains(excludedPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String text, Set<String> values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String key(GrepMatch match) {
        return normalize(match.getFilePath());
    }

    private String extractFileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }
        int index = filePath.lastIndexOf('/');
        return index >= 0 ? filePath.substring(index + 1) : filePath;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String joinNonBlank(String left, String right) {
        StringBuilder builder = new StringBuilder();
        if (left != null && !left.isBlank()) {
            builder.append(left.trim());
        }
        if (right != null && !right.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(right.trim());
        }
        return builder.toString();
    }

    private boolean isFocusedQuery(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String trimmed = query.trim();
        return trimmed.length() <= 80 && !trimmed.contains("\n") && !trimmed.matches(".*\\s+.*");
    }

    private boolean isApiLikeQuery(String query) {
        if (query == null) {
            return false;
        }
        String normalized = normalize(query);
        return normalized.startsWith("/")
                || normalized.contains("controller")
                || normalized.contains("requestmapping")
                || normalized.contains("getmapping")
                || normalized.contains("postmapping")
                || normalized.contains("api");
    }

    private double candidateScore(GrepMatch match, String query) {
        String filePath = normalize(match.getFilePath());
        String fileName = extractFileName(filePath);
        String lineText = normalize(match.getLineText());
        String normalizedQuery = normalize(query);
        double score = 0.0d;
        if (!normalizedQuery.isBlank() && lineText.contains(normalizedQuery)) {
            score += 10.0d;
        }
        if (isNumericQuery(normalizedQuery) && lineText.contains(normalizedQuery)) {
            score += 20.0d;
        }
        if (!normalizedQuery.isBlank() && fileName.contains(normalizedQuery)) {
            score += 5.0d;
        }
        if (containsAny(filePath, Set.of("/service/", "/mapper/", "/repository/", "/util/", "/utils/"))) {
            score += 3.0d;
        }
        if (match.getLineNumber() != null && match.getLineNumber() > 0) {
            score += 1.0d / Math.max(1, match.getLineNumber());
        }
        return score;
    }

    private int resolveMaxResults(SearchRequest request) {
        return request.getMaxResults() == null || request.getMaxResults() <= 0
                ? properties.getMaxResults()
                : request.getMaxResults();
    }

    private boolean isNumericQuery(String query) {
        return query != null && query.matches("\\d+");
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

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private record MatchAccumulator(GrepMatch match, String query, String reason, int hitCount, double score) {

        private MatchAccumulator incrementHitCount(GrepMatch candidate, String candidateQuery, String candidateReason, boolean useCandidate) {
            if (useCandidate) {
                return new MatchAccumulator(candidate, candidateQuery, candidateReason, hitCount + 1, score);
            }
            return new MatchAccumulator(match, query, reason, hitCount + 1, score);
        }

        private MatchAccumulator withScore(double score) {
            return new MatchAccumulator(match, query, reason, hitCount, score);
        }
    }
}
