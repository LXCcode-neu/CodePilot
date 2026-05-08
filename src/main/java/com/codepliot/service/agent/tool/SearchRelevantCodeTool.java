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

@Component
@Order(30)
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

    @Override
    public ToolResult execute(AgentContext context) {
        String issueText = buildIssueText(context);
        List<CodeSearchResult> searchResults;
        try {
            searchResults = agenticCodeSearchService.search(context.localPath(), issueText);
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
