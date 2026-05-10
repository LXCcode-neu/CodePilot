package com.codepliot.search;

import com.codepliot.search.dto.CodeSnippet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 将代码片段格式化为稳定、便于 LLM 阅读的检索上下文。
 */
@Component
public class ContextBuilder {

    private static final int DEFAULT_MAX_CONTEXT_CHARS = 24_000;
    private static final int MERGE_DISTANCE_LINES = 8;

    public String build(List<CodeSnippet> snippets) {
        return build(snippets, DEFAULT_MAX_CONTEXT_CHARS);
    }

    public String build(List<CodeSnippet> snippets, int maxContextChars) {
        if (snippets == null || snippets.isEmpty() || maxContextChars <= 0) {
            return "";
        }

        List<CodeSnippet> normalizedSnippets = normalizeAndMerge(snippets);
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (CodeSnippet snippet : normalizedSnippets) {
            String block = formatSnippet(index, snippet);
            if (builder.length() + block.length() > maxContextChars) {
                break;
            }
            builder.append(block);
            index++;
        }
        return builder.toString().trim();
    }

    private List<CodeSnippet> normalizeAndMerge(List<CodeSnippet> snippets) {
        Map<String, CodeSnippet> deduped = new LinkedHashMap<>();
        for (CodeSnippet snippet : snippets) {
            if (!isValid(snippet)) {
                continue;
            }
            CodeSnippet normalized = normalize(snippet);
            deduped.putIfAbsent(key(normalized), normalized);
        }

        List<CodeSnippet> sorted = new ArrayList<>(deduped.values());
        sorted.sort(Comparator.comparing(CodeSnippet::getFilePath)
                .thenComparing(CodeSnippet::getStartLine)
                .thenComparing(CodeSnippet::getEndLine));

        List<CodeSnippet> merged = new ArrayList<>();
        for (CodeSnippet snippet : sorted) {
            if (merged.isEmpty()) {
                merged.add(snippet);
                continue;
            }
            CodeSnippet previous = merged.get(merged.size() - 1);
            if (canMerge(previous, snippet)) {
                merged.set(merged.size() - 1, merge(previous, snippet));
            } else {
                merged.add(snippet);
            }
        }
        return merged;
    }

    private boolean isValid(CodeSnippet snippet) {
        return snippet != null
                && snippet.getFilePath() != null
                && !snippet.getFilePath().isBlank()
                && snippet.getStartLine() != null
                && snippet.getEndLine() != null
                && snippet.getContentWithLineNumbers() != null
                && !snippet.getContentWithLineNumbers().isBlank();
    }

    private CodeSnippet normalize(CodeSnippet snippet) {
        CodeSnippet normalized = new CodeSnippet();
        normalized.setFilePath(snippet.getFilePath().replace('\\', '/'));
        normalized.setStartLine(Math.max(1, snippet.getStartLine()));
        normalized.setEndLine(Math.max(normalized.getStartLine(), snippet.getEndLine()));
        normalized.setContentWithLineNumbers(snippet.getContentWithLineNumbers().strip());
        return normalized;
    }

    private boolean canMerge(CodeSnippet left, CodeSnippet right) {
        return left.getFilePath().equals(right.getFilePath())
                && right.getStartLine() <= left.getEndLine() + MERGE_DISTANCE_LINES;
    }

    private CodeSnippet merge(CodeSnippet left, CodeSnippet right) {
        CodeSnippet merged = new CodeSnippet();
        merged.setFilePath(left.getFilePath());
        merged.setStartLine(Math.min(left.getStartLine(), right.getStartLine()));
        merged.setEndLine(Math.max(left.getEndLine(), right.getEndLine()));
        merged.setContentWithLineNumbers(mergeContent(left.getContentWithLineNumbers(), right.getContentWithLineNumbers()));
        return merged;
    }

    private String mergeContent(String left, String right) {
        LinkedHashMap<String, Boolean> lines = new LinkedHashMap<>();
        addContentLines(lines, left);
        addContentLines(lines, right);
        return String.join("\n", lines.keySet());
    }

    private void addContentLines(LinkedHashMap<String, Boolean> lines, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        for (String line : content.split("\\R")) {
            lines.putIfAbsent(line, Boolean.TRUE);
        }
    }

    private String key(CodeSnippet snippet) {
        return snippet.getFilePath() + ":" + snippet.getStartLine() + ":" + snippet.getEndLine();
    }

    private String formatSnippet(int index, CodeSnippet snippet) {
        return """
                
                [Snippet %d]
                File: %s
                Lines: %d-%d
                Language: %s
                Code:
                %s
                """.formatted(
                index,
                snippet.getFilePath(),
                snippet.getStartLine(),
                snippet.getEndLine(),
                detectLanguage(snippet.getFilePath()),
                snippet.getContentWithLineNumbers()
        );
    }

    private String detectLanguage(String filePath) {
        String normalized = filePath == null ? "" : filePath.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".java")) {
            return "java";
        }
        if (normalized.endsWith(".ts") || normalized.endsWith(".tsx")) {
            return "typescript";
        }
        if (normalized.endsWith(".js") || normalized.endsWith(".jsx")) {
            return "javascript";
        }
        if (normalized.endsWith(".py")) {
            return "python";
        }
        if (normalized.endsWith(".go")) {
            return "go";
        }
        if (normalized.endsWith(".yml") || normalized.endsWith(".yaml")) {
            return "yaml";
        }
        if (normalized.endsWith(".xml")) {
            return "xml";
        }
        if (normalized.endsWith(".sql")) {
            return "sql";
        }
        return "text";
    }
}
