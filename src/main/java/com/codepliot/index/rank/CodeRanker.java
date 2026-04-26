package com.codepliot.index.rank;

import com.codepliot.index.dto.RetrievedCodeChunk;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 多语言代码结果重排器。
 * 基于 Issue 查询特征、路径命名习惯和不同语言约定对 Lucene 结果做二次加权。
 */
@Component
public class CodeRanker {

    private static final Set<String> CONTENT_SIGNAL_KEYWORDS = Set.of(
            "exception", "null", "token", "login", "auth", "permission", "unauthorized", "forbidden"
    );

    private static final Set<String> FRONTEND_HINTS = Set.of(
            "frontend", "react", "component", "ui", "page", "css", "vite", "tailwind"
    );

    public List<RetrievedCodeChunk> rerank(String query, List<RetrievedCodeChunk> chunks, int topK) {
        if (chunks == null || chunks.isEmpty() || topK <= 0) {
            return List.of();
        }

        Set<String> keywords = extractKeywords(query);
        boolean frontendIssue = containsAny(keywords, FRONTEND_HINTS);

        return chunks.stream()
                .map(chunk -> chunk.withFinalScore(calculateFinalScore(keywords, chunk, frontendIssue)))
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

        score += keywordHitBonus(filePath, keywords, 1.2d);
        score += keywordHitBonus(symbolName, keywords, 1.8d);
        score += keywordHitBonus(routePath, keywords, 1.5d);
        score += contentSignalBonus(content, keywords);

        if (containsAny(filePath, Set.of("/test/", "/tests/", "test_", "_test.", ".spec.", ".test."))) {
            score -= 1.6d;
        }
        if (containsAny(filePath, Set.of("/generated/", "/build/", "/dist/", "/target/", ".gen.", "generated-sources"))) {
            score -= 2.0d;
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
}
