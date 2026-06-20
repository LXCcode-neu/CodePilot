package com.codepliot.service.agent.tool;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.model.AgentContext;
import com.codepliot.model.RetrievedCodeChunk;
import com.codepliot.search.AgenticCodeSearchService;
import com.codepliot.search.config.CodeSearchProperties;
import com.codepliot.search.dto.CodeSearchResult;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.ToolResult;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 搜索相关代码工具。
 * <p>
 * Agent 工具链中的代码检索步骤（优先级 20）。根据 Issue 的标题和描述，
 * 使用代码搜索引擎在项目仓库中查找相关代码片段。支持 grep 等多种搜索模式，
 * 检索结果会被转换为 {@link RetrievedCodeChunk} 并更新到 Agent 上下文中，
 * 供后续的补丁生成步骤使用。
 * </p>
 */
@Component
@Order(20)
public class SearchRelevantCodeTool implements AgentTool {

    private static final String SEARCH_MODE_GREP = "grep";

    private final AgenticCodeSearchService agenticCodeSearchService;
    private final CodeSearchProperties codeSearchProperties;

    public SearchRelevantCodeTool(AgenticCodeSearchService agenticCodeSearchService,
                                  CodeSearchProperties codeSearchProperties) {
        this.agenticCodeSearchService = agenticCodeSearchService;
        this.codeSearchProperties = codeSearchProperties;
    }

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.RETRIEVING;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.SEARCH_RELEVANT_CODE;
    }

    @Override
    public String stepName() {
        return "Search Relevant Code";
    }

    /**
     * 执行代码搜索。
     * <p>
     * 根据 Issue 文本在项目仓库中搜索相关代码，将搜索结果转换为
     * RetrievedCodeChunk 列表并更新到上下文中。如果搜索失败或未找到结果，
     * 返回包含警告信息的失败结果。
     * </p>
     *
     * @param context Agent 执行上下文
     * @return 包含搜索结果摘要（命中数、代码片段列表、警告等）的工具执行结果
     */
    @Override
    public ToolResult execute(AgentContext context) {
        String issueText = buildIssueText(context);
        List<CodeSearchResult> searchResults;
        try {
            searchResults = agenticCodeSearchService.search(context.localPath(), issueText, context.llmRuntimeConfig());
        } catch (RuntimeException exception) {
            return ToolResult.failure("code search failed: " + buildErrorMessage(exception), Map.of(
                    "mode", codeSearchProperties.getMode(),
                    "query", issueText,
                    "error", buildErrorMessage(exception)
            ));
        }

        List<RetrievedCodeChunk> retrievedChunks = searchResults.stream()
                .filter(result -> result.getFilePath() != null && !result.getFilePath().isBlank())
                .map(this::toRetrievedCodeChunk)
                .toList();
        context.updateRetrievedChunks(retrievedChunks);

        List<String> warnings = searchResults.stream()
                .filter(result -> result.getFilePath() == null || result.getFilePath().isBlank())
                .map(CodeSearchResult::getReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .toList();

        if (retrievedChunks.isEmpty() && !warnings.isEmpty()) {
            return ToolResult.failure("code search failed: " + String.join("; ", warnings), Map.of(
                    "mode", normalizeMode(codeSearchProperties.getMode()),
                    "query", issueText,
                    "warnings", warnings
            ));
        }

        List<Map<String, Object>> chunkSummaries = retrievedChunks.stream()
                .map(chunk -> Map.<String, Object>of(
                        "filePath", chunk.filePath() == null ? "" : chunk.filePath(),
                        "language", chunk.language() == null ? "" : chunk.language(),
                        "symbolName", chunk.symbolName() == null ? "" : chunk.symbolName(),
                        "score", chunk.score(),
                        "finalScore", chunk.finalScore(),
                        "startLine", chunk.startLine() == null ? "" : chunk.startLine(),
                        "endLine", chunk.endLine() == null ? "" : chunk.endLine()
                ))
                .toList();

        return ToolResult.success("relevant code search completed", Map.of(
                "mode", normalizeMode(codeSearchProperties.getMode()),
                "query", issueText,
                "maxResults", codeSearchProperties.getMaxResults(),
                "hitCount", retrievedChunks.size(),
                "warnings", warnings,
                "chunks", chunkSummaries
        ));
    }

    private String buildIssueText(AgentContext context) {
        String title = context.issueTitle() == null ? "" : context.issueTitle().trim();
        String description = context.issueDescription() == null ? "" : context.issueDescription().trim();
        return (title + "\n" + description).trim();
    }

    private RetrievedCodeChunk toRetrievedCodeChunk(CodeSearchResult result) {
        double score = result.getScore() == null ? 0.0d : result.getScore();
        return new RetrievedCodeChunk(
                result.getFilePath(),
                detectLanguage(result.getFilePath()),
                "FILE",
                extractFileName(result.getFilePath()),
                null,
                null,
                result.getStartLine(),
                result.getEndLine(),
                result.getContentWithLineNumbers(),
                score,
                score
        );
    }

    private String detectLanguage(String filePath) {
        if (filePath == null) {
            return "";
        }
        String normalized = filePath.toLowerCase();
        if (normalized.endsWith(".java")) {
            return "JAVA";
        }
        if (normalized.endsWith(".ts") || normalized.endsWith(".tsx")) {
            return "TYPESCRIPT";
        }
        if (normalized.endsWith(".js") || normalized.endsWith(".jsx")) {
            return "JAVASCRIPT";
        }
        if (normalized.endsWith(".py")) {
            return "PYTHON";
        }
        if (normalized.endsWith(".go")) {
            return "GO";
        }
        return "UNKNOWN";
    }

    private String extractFileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }
        String normalized = filePath.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? SEARCH_MODE_GREP : mode.trim();
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
