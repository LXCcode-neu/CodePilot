package com.codepliot.search;

import com.codepliot.search.dto.CodeSnippet;
import com.codepliot.search.read.CodeReadService;
import org.springframework.stereotype.Component;

/**
 * 从仓库文件中读取带行号的代码。
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

    public CodeSnippet executeAround(String repoPath,
                                     String filePath,
                                     Integer lineNumber,
                                     int contextBeforeLines,
                                     int contextAfterLines) {
        int safeLineNumber = lineNumber == null || lineNumber <= 0 ? 1 : lineNumber;
        return codeReadService.readSnippet(
                repoPath,
                filePath,
                safeLineNumber,
                Math.max(0, contextBeforeLines),
                Math.max(0, contextAfterLines)
        );
    }
}
