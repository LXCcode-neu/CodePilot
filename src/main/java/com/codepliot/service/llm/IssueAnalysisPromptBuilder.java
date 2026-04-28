package com.codepliot.service.llm;

import com.codepliot.model.RetrievedCodeChunk;
import java.util.List;
import org.springframework.stereotype.Component;
/**
 * IssueAnalysisPromptBuilder 服务类，负责封装业务流程和领域能力。
 */
@Component
public class IssueAnalysisPromptBuilder {
/**
 * 构建System Prompt相关逻辑。
 */
public String buildSystemPrompt() {
        return """
                你是丢名谨慎的软件工程师，负责分析丢个多语言仓库中的 Issue?                你只能基于调用方提供的代码片段进行分析?                不要编不存在的文件函数路由依赖或调用关系?                如果现有信息不足，请明确指出缺少仢么信息?                如果代码片段中能看出不同语言文件之间的关联，请明确说明?                """;
    }
/**
 * 构建User Prompt相关逻辑。
 */
public String buildUserPrompt(String issueTitle, String issueDescription, List<RetrievedCodeChunk> retrievedChunks) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                请仅基于下面提供的代码检索结果分析这?Issue?
                要求?                1. 不要编提供片段之外的文件或代码?                2. 只能根据下方棢索到的代码片段进行推断?                3. 说明不同语言文件之间可见的关联关系?                4. 如果证据不足，请明确说明不足之处?                5. 给出箢洁但可执行的分析结论，供后续 patch 生成使用?
                Issue 标题?                """).append(nullToEmpty(issueTitle)).append('\n').append('\n')
                .append("Issue 描述：\n")
                .append(nullToEmpty(issueDescription)).append('\n').append('\n')
                .append("棢索到的代码片段：\n");

        appendChunks(builder, retrievedChunks);
        return builder.toString();
    }
/**
 * 执行 appendChunks 相关逻辑。
 */
private void appendChunks(StringBuilder builder, List<RetrievedCodeChunk> retrievedChunks) {
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            builder.append("- 当前没有可用于分析的代码片段。\n");
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
/**
 * 执行 nullToEmpty 相关逻辑。
 */
private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
