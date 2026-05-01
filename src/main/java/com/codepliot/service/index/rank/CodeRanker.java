package com.codepliot.service.index.rank;

import com.codepliot.model.RetrievedCodeChunk;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CodeRanker {

    private static final Set<String> CONTENT_SIGNAL_KEYWORDS = Set.of(
            "exception", "null", "token", "login", "auth", "permission", "unauthorized", "forbidden"
    );

    private static final Set<String> FRONTEND_HINTS = Set.of(
            "frontend", "react", "component", "ui", "page", "css", "vite", "tailwind", "tsx", "browser"
    );

    private static final Set<String> BACKEND_HINTS = Set.of(
            "backend", "api", "controller", "service", "repository", "redis", "lock", "database", "mysql",
            "mapper", "task", "status", "confirm", "patch"
    );

    private static final Set<String> CONFIG_HINTS = Set.of(
            "config", "configuration", "yaml", "yml", "properties", "sql", "schema"
    );

    private static final Set<String> STRONGLY_PENALIZED_PATHS = Set.of(
            "/.m2repo/", "/node_modules/", "/target/", "/dist/", "/coverage/", "/storybook-static/", "/static/assets/"
    );

    private static final Set<String> DOC_PATH_HINTS = Set.of(
            "/docs/", "readme", "agent-runtime-spec"
    );

    public List<RetrievedCodeChunk> rerank(String query, List<RetrievedCodeChunk> chunks, int topK) {
        if (chunks == null || chunks.isEmpty() || topK <= 0) {
            return List.of();
        }

        Set<String> keywords = extractKeywords(query);
        boolean frontendIssue = containsAny(keywords, FRONTEND_HINTS);

        Map<String, RetrievedCodeChunk> bestByFile = chunks.stream()
                .map(chunk -> chunk.withFinalScore(calculateFinalScore(keywords, chunk, frontendIssue)))
                .collect(Collectors.toMap(
                        chunk -> normalize(chunk.filePath()),
                        chunk -> chunk,
                        (left, right) -> left.finalScore() >= right.finalScore() ? left : right,
                        LinkedHashMap::new
                ));

        return bestByFile.values().stream()
                .sorted(Comparator.comparingDouble(RetrievedCodeChunk::finalScore).reversed()
                        .thenComparing(Comparator.comparingDouble(RetrievedCodeChunk::score).reversed())
                        .thenComparing(RetrievedCodeChunk::filePath, Comparator.nullsLast(String::compareTo)))
                .limit(topK)
                .collect(Collectors.toList());
    }

    private double calculateFinalScore(Set<String> keywords, RetrievedCodeChunk chunk, boolean frontendIssue) {
        double score = chunk.score();
        String filePath = normalize(chunk.filePath());
        String symbolName = normalize(chunk.symbolName());
        String routePath = normalize(chunk.routePath());
        String content = normalize(chunk.content());
        String language = normalize(chunk.language());
        String fileName = extractFileName(filePath);
        boolean backendIssue = containsAny(keywords, BACKEND_HINTS);
        boolean configIssue = containsAny(keywords, CONFIG_HINTS);

        score += keywordHitBonus(filePath, keywords, 1.2d);
        score += keywordHitBonus(symbolName, keywords, 1.8d);
        score += keywordHitBonus(routePath, keywords, 1.5d);
        score += contentSignalBonus(content, keywords);
        score += fileNameBonus(fileName, keywords);

        if (containsAny(filePath, STRONGLY_PENALIZED_PATHS) || containsAny(fileName, Set.of(".pom", ".jar", ".sha1", ".md5"))) {
            score -= 12.0d;
        }
        if (containsAny(filePath, DOC_PATH_HINTS) && !configIssue) {
            score -= 4.0d;
        }
        if (containsAny(filePath, Set.of("/test/", "/tests/", "test_", "_test.", ".spec.", ".test."))) {
            score -= 1.6d;
        }
        if (containsAny(filePath, Set.of("/generated/", "/build/", "/dist/", "/target/", ".gen.", "generated-sources"))) {
            score -= 2.0d;
        }
        if ("unknown".equals(language) && !isConfigLikeFile(filePath)) {
            score -= 3.5d;
        }

        if (frontendIssue && containsAny(filePath, Set.of("frontend/src/", "frontend\\src\\"))) {
            score += 2.0d;
        }
        if (backendIssue && containsAny(filePath, Set.of("src/main/java/", "src\\main\\java\\"))) {
            score += 2.0d;
        }
        if (configIssue && isConfigLikeFile(filePath)) {
            score += 2.5d;
        }

        score += switch (language) {
            case "java" -> javaBonus(filePath, symbolName);
            case "python" -> pythonBonus(filePath, content);
            case "javascript", "typescript" -> javaScriptLikeBonus(filePath, symbolName, content, frontendIssue);
            case "go" -> goBonus(filePath, symbolName);
            default -> 0.0d;
        };

        return score;
    }

    private double keywordHitBonus(String target, Set<String> keywords, double weight) {
        double bonus = 0.0d;
        for (String keyword : keywords) {
            if (target.contains(keyword)) {
                bonus += weight;
            }
        }
        return bonus;
    }

    private double contentSignalBonus(String content, Set<String> keywords) {
        double bonus = 0.0d;
        for (String signal : CONTENT_SIGNAL_KEYWORDS) {
            if (keywords.contains(signal) && content.contains(signal)) {
                bonus += 1.0d;
            }
        }
        return bonus;
    }

    private double fileNameBonus(String fileName, Set<String> keywords) {
        double bonus = 0.0d;
        for (String keyword : keywords) {
            if (fileName.equals(keyword) || fileName.startsWith(keyword + ".")) {
                bonus += 2.8d;
                continue;
            }
            if (fileName.contains(keyword)) {
                bonus += 1.8d;
            }
        }
        return bonus;
    }

    private double javaBonus(String filePath, String symbolName) {
        return containsWeighted(filePath + " " + symbolName, Set.of("controller", "service", "mapper", "filter", "config"), 1.3d);
    }

    private double pythonBonus(String filePath, String content) {
        double bonus = containsWeighted(filePath, Set.of("router", "views", "service", "models", "schemas"), 1.2d);
        if (containsAny(content, Set.of("@app.", "@router.", "@bp.route", "@blueprint.route", "@api."))) {
            bonus += 1.3d;
        }
        return bonus;
    }

    private double javaScriptLikeBonus(String filePath, String symbolName, String content, boolean frontendIssue) {
        double bonus = containsWeighted(filePath + " " + symbolName, Set.of("controller", "service", "route", "middleware", "api"), 1.2d);
        if (!frontendIssue && containsAny(filePath + " " + content, Set.of("react", ".tsx", ".jsx", "function component", "useeffect", "usestate"))) {
            bonus -= 0.8d;
        }
        return bonus;
    }

    private double goBonus(String filePath, String symbolName) {
        return containsWeighted(filePath + " " + symbolName, Set.of("handler", "service", "repository", "middleware", "router"), 1.3d);
    }

    private double containsWeighted(String target, Set<String> values, double weight) {
        double bonus = 0.0d;
        for (String value : values) {
            if (target.contains(value)) {
                bonus += weight;
            }
        }
        return bonus;
    }

    private boolean containsAny(Set<String> source, Set<String> candidates) {
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String source, Set<String> candidates) {
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) {
            return Set.of();
        }
        String[] tokens = query.toLowerCase(Locale.ROOT).split("[^a-z0-9_./-]+");
        Set<String> keywords = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token == null || token.isBlank() || token.length() <= 1) {
                continue;
            }
            keywords.add(token);
        }
        return keywords;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String extractFileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }
        int index = filePath.lastIndexOf('/');
        return index >= 0 ? filePath.substring(index + 1) : filePath;
    }

    private boolean isConfigLikeFile(String filePath) {
        return containsAny(filePath, Set.of(
                ".yml", ".yaml", ".properties", ".json", ".sql", ".xml", "dockerfile", "makefile", ".env"
        ));
    }
}
