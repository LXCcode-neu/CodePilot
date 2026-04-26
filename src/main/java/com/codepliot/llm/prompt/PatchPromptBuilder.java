package com.codepliot.llm.prompt;

import com.codepliot.index.dto.RetrievedCodeChunk;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 多语言 Patch 生成 Prompt 构建器。
 */
@Component
public class PatchPromptBuilder {

    public String buildSystemPrompt() {
        return """
                You are a careful senior software engineer preparing a patch suggestion for a multi-language repository.
                You must only use the provided issue, retrieved code chunks, and prior analysis.
                Do not fabricate files, code locations, or repository structure outside the provided snippets.
                Return only valid JSON with the exact required keys.
                Prefer unified diff format in the patch field when you can do so reliably.
                If a reliable patch cannot be produced, set patch to an empty string and explain why in risk.
                """;
    }

    public String buildUserPrompt(String issueTitle,
                                  String issueDescription,
                                  String analysis,
                                  List<RetrievedCodeChunk> retrievedChunks) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                Generate a patch suggestion for the following issue.

                Output requirements:
                1. Return ONLY valid JSON.
                2. The JSON must have exactly these keys:
                   {
                     "analysis": "...",
                     "solution": "...",
                     "patch": "...",
                     "risk": "..."
                   }
                3. The patch field should use unified diff when possible.
                4. If you cannot reliably generate a patch from the provided snippets, return an empty string for patch and explain the reason in risk.
                5. Do not mention files or code that are not present in the provided snippets.

                Issue Title:
                """).append(nullToEmpty(issueTitle)).append('\n').append('\n')
                .append("Issue Description:\n")
                .append(nullToEmpty(issueDescription)).append('\n').append('\n')
                .append("Prior Analysis:\n")
                .append(nullToEmpty(analysis)).append('\n').append('\n')
                .append("Retrieved Code Chunks:\n");

        appendChunks(builder, retrievedChunks);
        return builder.toString();
    }

    private void appendChunks(StringBuilder builder, List<RetrievedCodeChunk> retrievedChunks) {
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            builder.append("- No retrieved code chunks were provided.\n");
            return;
        }

        int index = 1;
        for (RetrievedCodeChunk chunk : retrievedChunks) {
            builder.append('\n')
                    .append("[Chunk ").append(index++).append("]\n")
                    .append("language: ").append(nullToEmpty(chunk.language())).append('\n')
                    .append("filePath: ").append(nullToEmpty(chunk.filePath())).append('\n')
                    .append("symbolType: ").append(nullToEmpty(chunk.symbolType())).append('\n')
                    .append("symbolName: ").append(nullToEmpty(chunk.symbolName())).append('\n')
                    .append("parentSymbol: ").append(nullToEmpty(chunk.parentSymbol())).append('\n')
                    .append("routePath: ").append(nullToEmpty(chunk.routePath())).append('\n')
                    .append("startLine: ").append(chunk.startLine() == null ? "" : chunk.startLine()).append('\n')
                    .append("endLine: ").append(chunk.endLine() == null ? "" : chunk.endLine()).append('\n')
                    .append("content:\n")
                    .append(nullToEmpty(chunk.content())).append('\n');
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
