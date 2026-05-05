package com.codepliot.search;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Builds prioritized grep query terms from issue descriptions.
 */
@Component
public class SearchQueryPlanner {

    private static final int DEFAULT_MAX_TERMS = 10;

    private static final Pattern API_PATH_PATTERN = Pattern.compile("(?<![\\w.-])/[A-Za-z0-9_{}:$.-]+(?:/[A-Za-z0-9_{}:$.-]+)+");
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("\\b[A-Z][A-Za-z0-9_]*Exception\\b");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\b[A-Z][A-Za-z0-9_]*(?:Controller|Service|Mapper|Repository|Executor|Client|Config|Policy|Handler|Filter|Tool|Builder|Record)\\b");
    private static final Pattern METHOD_NAME_PATTERN = Pattern.compile("\\b[a-z][A-Za-z0-9_]*(?:Task|Patch|Run|Confirm|Create|Update|Delete|Search|Index|Login|Execute|Generate|Apply|Build)[A-Za-z0-9_]*\\b");

    private static final Set<String> TECH_KEYWORDS = Set.of(
            "token", "login", "patch", "diff", "task", "agent", "redis", "jwt", "security",
            "controller", "service", "mapper", "repository"
    );

    private static final Map<String, List<String>> CHINESE_MAPPINGS = Map.ofEntries(
            Map.entry("登录", List.of("login")),
            Map.entry("用户", List.of("user")),
            Map.entry("任务", List.of("task")),
            Map.entry("补丁", List.of("patch")),
            Map.entry("确认", List.of("confirm")),
            Map.entry("安全检查", List.of("safety", "check")),
            Map.entry("仓库", List.of("repo", "repository")),
            Map.entry("检索", List.of("search", "grep")),
            Map.entry("索引", List.of("index", "search")),
            Map.entry("报错", List.of("error", "exception")),
            Map.entry("权限", List.of("auth", "security")),
            Map.entry("接口", List.of("api", "controller")),
            Map.entry("异步", List.of("async")),
            Map.entry("执行", List.of("execute", "run")),
            Map.entry("状态", List.of("status", "state"))
    );

    private static final List<String> SPRING_ANNOTATION_FALLBACKS = List.of(
            "@RestController",
            "@RequestMapping",
            "@PostMapping",
            "@GetMapping"
    );

    public List<String> plan(String issueText) {
        return plan(issueText, DEFAULT_MAX_TERMS);
    }

    public List<String> plan(String issueText, int maxTerms) {
        if (issueText == null || issueText.isBlank() || maxTerms <= 0) {
            return List.of();
        }

        LinkedHashMap<String, Boolean> terms = new LinkedHashMap<>();
        addPatternMatches(terms, issueText, API_PATH_PATTERN);
        addPatternMatches(terms, issueText, EXCEPTION_PATTERN);
        addPatternMatches(terms, issueText, CLASS_NAME_PATTERN);
        addPatternMatches(terms, issueText, METHOD_NAME_PATTERN);
        addEnglishKeywords(terms, issueText);
        addChineseMappings(terms, issueText);
        addSpringAnnotationFallbacks(terms);

        return terms.keySet().stream()
                .limit(maxTerms)
                .toList();
    }

    public String buildQuery(String issueText) {
        return String.join(" ", plan(issueText));
    }

    private void addPatternMatches(LinkedHashMap<String, Boolean> terms, String issueText, Pattern pattern) {
        Matcher matcher = pattern.matcher(issueText);
        while (matcher.find()) {
            addTerm(terms, matcher.group());
        }
    }

    private void addEnglishKeywords(LinkedHashMap<String, Boolean> terms, String issueText) {
        String normalized = issueText.toLowerCase();
        for (String keyword : TECH_KEYWORDS) {
            if (containsWord(normalized, keyword)) {
                addTerm(terms, keyword);
            }
        }
    }

    private void addChineseMappings(LinkedHashMap<String, Boolean> terms, String issueText) {
        for (Map.Entry<String, List<String>> entry : CHINESE_MAPPINGS.entrySet()) {
            if (!issueText.contains(entry.getKey())) {
                continue;
            }
            for (String term : entry.getValue()) {
                addTerm(terms, term);
            }
        }
    }

    private void addSpringAnnotationFallbacks(LinkedHashMap<String, Boolean> terms) {
        for (String fallback : SPRING_ANNOTATION_FALLBACKS) {
            addTerm(terms, fallback);
        }
    }

    private boolean containsWord(String text, String word) {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).find();
    }

    private void addTerm(LinkedHashMap<String, Boolean> terms, String term) {
        if (term == null || term.isBlank()) {
            return;
        }
        terms.putIfAbsent(term.trim(), Boolean.TRUE);
    }
}
