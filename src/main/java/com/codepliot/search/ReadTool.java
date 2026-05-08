package com.codepliot.search;

import com.codepliot.search.dto.CodeSnippet;
import com.codepliot.search.read.CodeReadService;
import org.springframework.stereotype.Component;

/**
 * Reads line-numbered code from a repository file.
 */
@Component
public class ReadTool {

    private static final int DEFAULT_CONTEXT_AFTER_LINES = 120;

    private final CodeReadService codeReadService;

    public ReadTool(CodeReadService codeReadService) {
        this.codeReadService = codeReadService;
    }

    public CodeSnippet execute(String repoPath, String filePath, Integer startLine, Integer endLine) {
        int safeStartLine = startLine == null || startLine <= 0 ? 1 : startLine;
        int contextAfterLines = endLine == null || endLine < safeStartLine
                ? DEFAULT_CONTEXT_AFTER_LINES
                : endLine - safeStartLine;
        return codeReadService.readSnippet(repoPath, filePath, safeStartLine, 0, contextAfterLines);
    }
}
