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
                你是一名谨慎的软件工程师，负责为一个多语言仓库生成 patch 建议。
                你只能使用给定的 Issue、检索代码片段和已有分析结果。
                不要编造提供片段之外的文件、代码位置或仓库结构。
                返回结果必须是合法 JSON，且字段必须完全符合要求。
                如果可以可靠生成 patch，优先使用 unified diff 格式。
                如果无法可靠生成 patch，请将 patch 置为空字符串，并在 risk 中说明原因。
                """;
    }

    public String buildUserPrompt(String issueTitle,
                                  String issueDescription,
                                  String analysis,
                                  List<RetrievedCodeChunk> retrievedChunks) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                请为下面的 Issue 生成 patch 建议。

                输出要求：
                1. 只能返回合法 JSON。
                2. JSON 必须且只能包含以下字段：
                   {
                     "analysis": "...",
                     "solution": "...",
                     "patch": "...",
                     "risk": "..."
                   }
                3. patch 字段尽量使用 unified diff。
                4. 如果无法仅基于提供片段可靠生成 patch，请返回空字符串，并在 risk 中说明原因。
                5. 不要提及提供片段中不存在的文件或代码。

                Issue 标题：
                """).append(nullToEmpty(issueTitle)).append('\n').append('\n')
                .append("Issue 描述：\n")
                .append(nullToEmpty(issueDescription)).append('\n').append('\n')
                .append("已有分析：\n")
                .append(nullToEmpty(analysis)).append('\n').append('\n')
                .append("检索到的代码片段：\n");

        appendChunks(builder, retrievedChunks);
        return builder.toString();
    }

    private void appendChunks(StringBuilder builder, List<RetrievedCodeChunk> retrievedChunks) {
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            builder.append("- 当前没有可用于生成 patch 的代码片段。\n");
            return;
        }

        int index = 1;
        for (RetrievedCodeChunk chunk : retrievedChunks) {
            builder.append('\n')
                    .append("[代码片段 ").append(index++).append("]\n")
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
