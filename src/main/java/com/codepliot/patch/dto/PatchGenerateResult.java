package com.codepliot.patch.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * Patch 生成结构化结果。
 */
public record PatchGenerateResult(
        String analysis,
        String solution,
        String patch,
        String risk
) {

    public static PatchGenerateResult fromRawOutput(ObjectMapper objectMapper, String rawOutput) {
        try {
            JsonNode root = objectMapper.readTree(stripMarkdownFence(rawOutput));
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
        return node.isMissingNode() || node.isNull() ? "" : node.asText("");
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
