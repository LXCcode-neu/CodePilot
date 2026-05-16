package com.codepliot.service.llm;

import com.codepliot.model.PatchGenerateResult;
import com.codepliot.model.PatchVerificationCommandResult;
import com.codepliot.model.PatchVerificationResult;
import com.codepliot.model.RetrievedCodeChunk;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PatchReviewPromptBuilder {

    private static final int TEXT_LIMIT = 20000;

    public String buildSystemPrompt() {
        return """
                You are a senior code reviewer inside CodePilot.
                Review the generated patch after deterministic verification has passed.
                Focus on correctness, minimality, maintainability, security, and repository constraints.
                Return only a JSON object. Do not wrap it in markdown.
                """;
    }

    public String buildUserPrompt(String issueTitle,
                                  String issueDescription,
                                  String analysis,
                                  List<RetrievedCodeChunk> retrievedChunks,
                                  PatchGenerateResult patchGenerateResult,
                                  PatchVerificationResult verificationResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("Review this patch as an AI code review gate.\n\n")
                .append("Decision rules:\n")
                .append("- passed must be false if the patch likely fails to fix the issue, introduces high-risk behavior, leaks secrets, violates repository architecture, or makes unrelated changes.\n")
                .append("- score must be 0-100.\n")
                .append("- riskLevel must be LOW, MEDIUM, or HIGH.\n")
                .append("- Return MEDIUM risk for issues that need human attention but do not block confirmation.\n")
                .append("- Return HIGH risk for issues that should block PR confirmation.\n\n")
                .append("Check these criteria:\n")
                .append("1. Fixes the original issue or Sentry alert root cause.\n")
                .append("2. Keeps the patch minimal and focused.\n")
                .append("3. Avoids unrelated refactors or broad behavior changes.\n")
                .append("4. Preserves package layout, layering, and existing style.\n")
                .append("5. Does not introduce unsupported dependencies, frameworks, middleware, or frontend stacks.\n")
                .append("6. Does not expose tokens, API keys, webhook secrets, OAuth tokens, or plaintext credentials.\n")
                .append("7. Does not weaken auth, encryption, notification approval, verification, or PR confirmation gates.\n")
                .append("8. Handles obvious null, concurrency, transaction, and boundary risks.\n")
                .append("9. Names and code structure are clear enough for maintainers.\n\n")
                .append("Required JSON schema:\n")
                .append("{\n")
                .append("  \"passed\": true,\n")
                .append("  \"score\": 86,\n")
                .append("  \"riskLevel\": \"LOW\",\n")
                .append("  \"summary\": \"Concise review conclusion.\",\n")
                .append("  \"findings\": [{\"severity\":\"LOW\",\"filePath\":\"path/or/null\",\"message\":\"finding\"}],\n")
                .append("  \"recommendations\": [\"manual follow-up if any\"]\n")
                .append("}\n\n")
                .append("Issue title:\n").append(nullToEmpty(issueTitle)).append("\n\n")
                .append("Issue description:\n").append(truncate(issueDescription)).append("\n\n")
                .append("Prior analysis:\n").append(truncate(analysis)).append("\n\n")
                .append("Patch generation analysis:\n")
                .append(truncate(patchGenerateResult == null ? null : patchGenerateResult.analysis())).append("\n\n")
                .append("Patch solution:\n")
                .append(truncate(patchGenerateResult == null ? null : patchGenerateResult.solution())).append("\n\n")
                .append("Patch risk from generator:\n")
                .append(truncate(patchGenerateResult == null ? null : patchGenerateResult.risk())).append("\n\n")
                .append("Patch diff:\n```diff\n")
                .append(truncate(patchGenerateResult == null ? null : patchGenerateResult.patch()))
                .append("\n```\n\n")
                .append("Verification summary:\n")
                .append(verificationResult == null ? "" : nullToEmpty(verificationResult.summary())).append("\n\n")
                .append("Verification commands:\n");
        if (verificationResult != null) {
            for (PatchVerificationCommandResult command : verificationResult.commands()) {
                builder.append("- ").append(command.name())
                        .append(" passed=").append(command.passed())
                        .append(" exitCode=").append(command.exitCode())
                        .append(" timedOut=").append(command.timedOut())
                        .append("\n");
            }
        }
        builder.append("\nRelevant code snippets:\n");
        for (RetrievedCodeChunk chunk : safeChunks(retrievedChunks)) {
            builder.append("File: ").append(chunk.filePath()).append("\n")
                    .append("Lines: ").append(chunk.startLine()).append("-").append(chunk.endLine()).append("\n")
                    .append("```").append(chunk.language() == null ? "" : chunk.language().toLowerCase()).append("\n")
                    .append(truncate(chunk.content(), 5000))
                    .append("\n```\n\n");
        }
        return truncate(builder.toString(), 60000);
    }

    private List<RetrievedCodeChunk> safeChunks(List<RetrievedCodeChunk> chunks) {
        return chunks == null ? List.of() : chunks.stream().limit(12).toList();
    }

    private String truncate(String value) {
        return truncate(value, TEXT_LIMIT);
    }

    private String truncate(String value, int limit) {
        if (value == null) {
            return "";
        }
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
