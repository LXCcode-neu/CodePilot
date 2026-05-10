package com.codepliot.search.grep;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 为 ProcessBuilder 构建 ripgrep 命令参数列表。
 */
@Component
public class RipgrepCommandBuilder {

    public List<String> build(RipgrepCommand request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.query() == null || request.query().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }

        List<String> command = new ArrayList<>();
        command.add(request.rgPath() == null || request.rgPath().isBlank() ? "rg" : request.rgPath());
        command.add("--line-number");
        command.add("--column");
        command.add("--no-heading");
        command.add("--color");
        command.add("never");

        if (!request.regexEnabled()) {
            command.add("--fixed-strings");
        }
        if (request.maxResults() != null && request.maxResults() > 0) {
            command.add("--max-count");
            command.add(String.valueOf(request.maxResults()));
        }

        addGlobArguments(command, request.includeGlobs(), false);
        addGlobArguments(command, request.excludeGlobs(), true);

        command.add(request.query());
        command.add(".");
        return command;
    }

    private void addGlobArguments(List<String> command, List<String> patterns, boolean exclude) {
        if (patterns == null) {
            return;
        }
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            command.add("--glob");
            command.add(exclude ? "!" + normalizeGlob(pattern) : normalizeGlob(pattern));
        }
    }

    private String normalizeGlob(String pattern) {
        String normalized = pattern.replace('\\', '/');
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }

    public record RipgrepCommand(
            String rgPath,
            String query,
            boolean regexEnabled,
            List<String> includeGlobs,
            List<String> excludeGlobs,
            Integer maxResults
    ) {
    }
}
