package com.codepliot.search;

import com.codepliot.search.dto.SearchRequest;
import com.codepliot.service.llm.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Uses the configured LLM to decide the first grep/glob probes, then safely falls back to deterministic terms.
 */
@Component
public class LlmSearchQueryPlanner {

    private static final int MAX_LLM_QUERIES = 8;
    private static final int MAX_PATTERN_LENGTH = 120;
    private static final Set<String> DEFAULT_CODE_GLOBS = Set.of(
            "**/*.java",
            "**/*.xml",
            "**/*.yml",
            "**/*.yaml",
            "**/*.properties",
            "**/*.sql",
            "**/*.ts",
            "**/*.tsx",
            "**/*.js",
            "**/*.jsx"
    );

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Autowired
    public LlmSearchQueryPlanner(ObjectProvider<LlmService> llmServiceProvider, ObjectMapper objectMapper) {
        this(llmServiceProvider.getIfAvailable(), objectMapper);
    }

    public LlmSearchQueryPlanner(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    public SearchPlan plan(SearchRequest request, List<String> deterministicTerms, int maxQueries) {
        SearchPlan fallback = buildDeterministicPlan(deterministicTerms, maxQueries);
        if (llmService == null || request == null || isBlank(joinIssueText(request))) {
            return fallback;
        }

        try {
            String raw = llmService.generate(buildSystemPrompt(), buildUserPrompt(request, deterministicTerms, maxQueries));
            SearchPlan llmPlan = parsePlan(raw, maxQueries);
            if (!llmPlan.queries().isEmpty() || !llmPlan.fileGlobs().isEmpty()) {
                return mergePlans(llmPlan, fallback);
            }
            return fallback;
        } catch (RuntimeException exception) {
            return new SearchPlan(fallback.queries(), fallback.fileGlobs(), fallback.source(),
                    "LLM search planning failed; deterministic fallback used: " + buildErrorMessage(exception));
        }
    }

    private SearchPlan buildDeterministicPlan(List<String> deterministicTerms, int maxQueries) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        if (deterministicTerms != null) {
            for (String term : deterministicTerms) {
                if (isSafePattern(term)) {
                    terms.add(term.trim());
                }
                if (terms.size() >= maxQueries) {
                    break;
                }
            }
        }

        List<SearchPlanQuery> queries = terms.stream()
                .map(term -> new SearchPlanQuery(term, false, List.of(), "deterministic issue term"))
                .toList();
        return SearchPlan.deterministic(queries, List.of());
    }

    private SearchPlan parsePlan(String raw, int maxQueries) {
        String json = extractJson(raw);
        if (json.isBlank()) {
            return new SearchPlan(List.of(), List.of(), "llm", "LLM did not return JSON search plan");
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            List<SearchPlanQuery> queries = new ArrayList<>();
            JsonNode queryNodes = root.path("queries");
            if (queryNodes.isArray()) {
                for (JsonNode queryNode : queryNodes) {
                    if (queries.size() >= Math.min(MAX_LLM_QUERIES, maxQueries)) {
                        break;
                    }
                    String pattern = queryNode.path("pattern").asText("");
                    if (!isSafePattern(pattern)) {
                        continue;
                    }
                    queries.add(new SearchPlanQuery(
                            pattern.trim(),
                            queryNode.path("regexEnabled").asBoolean(false),
                            parseStringArray(queryNode.path("globPatterns")),
                            queryNode.path("reason").asText("llm planned query")
                    ));
                }
            }
            return new SearchPlan(queries, parseStringArray(root.path("fileGlobs")), "llm", "");
        } catch (Exception exception) {
            return new SearchPlan(List.of(), List.of(), "llm",
                    "LLM search plan JSON could not be parsed: " + buildErrorMessage(exception));
        }
    }

    private SearchPlan mergePlans(SearchPlan primary, SearchPlan fallback) {
        List<SearchPlanQuery> queries = new ArrayList<>();
        LinkedHashSet<String> seenPatterns = new LinkedHashSet<>();
        addQueries(queries, seenPatterns, primary.queries());
        addQueries(queries, seenPatterns, fallback.queries());

        LinkedHashSet<String> fileGlobs = new LinkedHashSet<>();
        fileGlobs.addAll(primary.fileGlobs());
        fileGlobs.addAll(fallback.fileGlobs());
        return new SearchPlan(queries, List.copyOf(fileGlobs), primary.source(), primary.warning());
    }

    private void addQueries(List<SearchPlanQuery> target, Set<String> seenPatterns, List<SearchPlanQuery> source) {
        for (SearchPlanQuery query : source) {
            if (query == null || !isSafePattern(query.pattern())) {
                continue;
            }
            String key = query.pattern().trim() + ":" + query.regexEnabled();
            if (seenPatterns.add(key)) {
                target.add(query);
            }
        }
    }

    private List<String> parseStringArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (JsonNode item : node) {
            String value = item.asText("");
            if (value == null || value.isBlank()) {
                continue;
            }
            String normalized = value.trim().replace('\\', '/');
            if (normalized.length() <= MAX_PATTERN_LENGTH && isLikelyCodeGlob(normalized)) {
                values.add(normalized);
            }
        }
        return List.copyOf(values);
    }

    private boolean isLikelyCodeGlob(String glob) {
        return DEFAULT_CODE_GLOBS.contains(glob)
                || glob.startsWith("**/*")
                || glob.startsWith("src/")
                || glob.endsWith(".java")
                || glob.endsWith(".xml")
                || glob.endsWith(".yml")
                || glob.endsWith(".yaml")
                || glob.endsWith(".properties")
                || glob.endsWith(".sql")
                || glob.endsWith(".ts")
                || glob.endsWith(".tsx")
                || glob.endsWith(".js")
                || glob.endsWith(".jsx");
    }

    private boolean isSafePattern(String pattern) {
        return pattern != null
                && !pattern.isBlank()
                && pattern.length() <= MAX_PATTERN_LENGTH
                && !pattern.contains("\n")
                && !pattern.contains("\r");
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return "";
    }

    private String buildSystemPrompt() {
        return """
                You are a read-only code search planner, similar to Claude Code's Grep/Glob/Read workflow.
                Return JSON only. Do not explain.
                Prefer exact grep terms that are likely to appear in source code: class names, method names,
                constants, annotations, API paths, exception names, English identifiers, and translated domain terms.
                Use broad-to-specific probes. Avoid generic words like fix, bug, problem.
                Schema:
                {
                  "queries": [
                    {"pattern": "RandomUtil.randomNumbers", "regexEnabled": false, "globPatterns": ["**/*.java"], "reason": "likely generator call"}
                  ],
                  "fileGlobs": ["**/*Service*.java"]
                }
                """;
    }

    private String buildUserPrompt(SearchRequest request, List<String> deterministicTerms, int maxQueries) {
        return """
                Issue:
                %s

                Existing deterministic hints:
                %s

                Generate at most %d grep queries and optional file globs.
                For Chinese issues, translate domain words into likely Java identifiers.
                Keep patterns short and exact. Prefer business/service/util code before controller-only probes.
                """.formatted(joinIssueText(request), deterministicTerms == null ? List.of() : deterministicTerms, Math.min(MAX_LLM_QUERIES, maxQueries));
    }

    private String joinIssueText(SearchRequest request) {
        String issueText = request.getIssueText() == null ? "" : request.getIssueText().trim();
        String query = request.getQuery() == null ? "" : request.getQuery().trim();
        if (issueText.isBlank()) {
            return query;
        }
        if (query.isBlank() || issueText.equals(query)) {
            return issueText;
        }
        return issueText + "\n" + query;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
