package com.codepliot.patch.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Patch 生成结构化结果。
 */
public record PatchGenerateResult(
        String analysis,
        String solution,
        String patch,
        String risk
) {

    private static final Set<String> REQUIRED_KEYS = Set.of("analysis", "solution", "patch", "risk");

    public static PatchGenerateResult fromRawOutput(ObjectMapper objectMapper, String rawOutput) {
        try {
            JsonNode root = objectMapper.readTree(stripMarkdownFence(rawOutput));
            validateRoot(root);
            return new PatchGenerateResult(
                    text(root, "analysis"),
                    text(root, "solution"),
                    text(root, "patch"),
                    text(root, "risk")
            );
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to parse patch generation JSON output", exception);
        }
    }

    private static String text(JsonNode root, String key) {
        JsonNode node = root.path(key);
        if (node.isMissingNode() || node.isNull() || !node.isTextual()) {
            throw new IllegalArgumentException("Patch generation JSON field must be a string: " + key);
        }
        return node.asText("");
    }

    private static void validateRoot(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Patch generation output must be a JSON object");
        }

        Set<String> actualKeys = new LinkedHashSet<>();
        Iterator<String> fieldNames = root.fieldNames();
        while (fieldNames.hasNext()) {
            actualKeys.add(fieldNames.next());
        }

        if (!actualKeys.equals(REQUIRED_KEYS)) {
            throw new IllegalArgumentException(
                    "Patch generation JSON must contain exactly these keys: " + REQUIRED_KEYS
            );
        }
    }

    private static String stripMarkdownFence(String rawOutput) {
        if (rawOutput == null) {
            return "";
        }
        String normalized = rawOutput.trim();
        if (normalized.startsWith("```")) {
            int firstLineBreak = normalized.indexOf('\n');
            if (firstLineBreak >= 0) {
                normalized = normalized.substring(firstLineBreak + 1);
            }
            if (normalized.endsWith("```")) {
                normalized = normalized.substring(0, normalized.length() - 3);
            }
        }
        return normalized.trim();
    }
}
