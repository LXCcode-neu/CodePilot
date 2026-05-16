package com.codepliot.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public record PatchReviewResult(
        boolean skipped,
        boolean passed,
        int score,
        String riskLevel,
        String summary,
        List<PatchReviewFinding> findings,
        List<String> recommendations,
        String rawResponse
) {
    public PatchReviewResult {
        riskLevel = riskLevel == null || riskLevel.isBlank() ? "MEDIUM" : riskLevel.trim().toUpperCase();
        summary = summary == null ? "" : summary.trim();
        findings = findings == null ? List.of() : List.copyOf(findings);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }

    public static PatchReviewResult skipped(String summary) {
        return new PatchReviewResult(true, true, 100, "LOW", summary, List.of(), List.of(), null);
    }

    public static PatchReviewResult failed(String summary, String rawResponse) {
        return new PatchReviewResult(false, false, 0, "HIGH", summary, List.of(), List.of(), rawResponse);
    }

    public static PatchReviewResult fromRawOutput(ObjectMapper objectMapper, String rawOutput) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(rawOutput));
            List<PatchReviewFinding> findings = new ArrayList<>();
            JsonNode findingsNode = root.path("findings");
            if (findingsNode.isArray()) {
                for (JsonNode finding : findingsNode) {
                    findings.add(new PatchReviewFinding(
                            text(finding, "severity"),
                            text(finding, "filePath"),
                            text(finding, "message")
                    ));
                }
            }
            List<String> recommendations = new ArrayList<>();
            JsonNode recommendationsNode = root.path("recommendations");
            if (recommendationsNode.isArray()) {
                for (JsonNode recommendation : recommendationsNode) {
                    if (recommendation.isTextual() && !recommendation.asText().isBlank()) {
                        recommendations.add(recommendation.asText());
                    }
                }
            }
            return new PatchReviewResult(
                    false,
                    root.path("passed").asBoolean(false),
                    Math.max(0, Math.min(100, root.path("score").asInt(0))),
                    text(root, "riskLevel"),
                    text(root, "summary"),
                    findings,
                    recommendations,
                    rawOutput
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse patch review JSON output", exception);
        }
    }

    private static String extractJson(String rawOutput) {
        if (rawOutput == null) {
            throw new IllegalArgumentException("Patch review output is empty");
        }
        String trimmed = rawOutput.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Patch review output does not contain JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
