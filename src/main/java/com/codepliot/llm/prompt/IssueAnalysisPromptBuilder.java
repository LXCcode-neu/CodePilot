package com.codepliot.llm.prompt;

import com.codepliot.index.dto.RetrievedCodeChunk;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 多语言 Issue 分析 Prompt 构建器。
 */
@Component
public class IssueAnalysisPromptBuilder {

    public String buildSystemPrompt() {
        return """
                You are a careful senior software engineer analyzing a multi-language repository issue.
                Only use the code snippets provided by the caller.
                Do not invent missing files, functions, routes, or dependencies.
                If the provided snippets are insufficient, explicitly say what is missing.
                Explain cross-language relationships when they are visible in the snippets.
                """;
    }

    public String buildUserPrompt(String issueTitle, String issueDescription, List<RetrievedCodeChunk> retrievedChunks) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                Analyze the following issue using only the provided retrieved code chunks.

                Requirements:
                1. Do not fabricate files or code outside the provided snippets.
                2. Only reason from the retrieved snippets below.
                3. Explain any visible relationships across different language files.
                4. If the evidence is insufficient, say so clearly.
                5. Provide a concise but actionable analysis for a follow-up patch step.

                Issue Title:
                """).append(nullToEmpty(issueTitle)).append('\n').append('\n')
                .append("Issue Description:\n")
                .append(nullToEmpty(issueDescription)).append('\n').append('\n')
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
