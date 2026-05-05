package com.codepliot.search.read;

import com.codepliot.search.dto.CodeSnippet;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

/**
 * Reads line-numbered snippets from repository files with path traversal protection.
 */
@Service
public class CodeReadService {

    public CodeSnippet readSnippet(String repoPath,
                                   String filePath,
                                   int lineNumber,
                                   int contextBeforeLines,
                                   int contextAfterLines) {
        Path repoRoot = resolveRepositoryRoot(repoPath);
        Path targetFile = resolveRepositoryFile(repoRoot, filePath);
        int safeLineNumber = Math.max(1, lineNumber);
        int startLine = Math.max(1, safeLineNumber - Math.max(0, contextBeforeLines));
        int endLine = safeLineNumber + Math.max(0, contextAfterLines);

        StringBuilder content = new StringBuilder();
        int actualEndLine = startLine;
        try (BufferedReader reader = Files.newBufferedReader(targetFile, StandardCharsets.UTF_8)) {
            String line;
            int currentLine = 0;
            while ((line = reader.readLine()) != null) {
                currentLine++;
                if (currentLine < startLine) {
                    continue;
                }
                if (currentLine > endLine) {
                    break;
                }
                if (content.length() > 0) {
                    content.append('\n');
                }
                content.append(String.format("%6d | %s", currentLine, line));
                actualEndLine = currentLine;
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read repository file: " + buildErrorMessage(exception), exception);
        }

        CodeSnippet snippet = new CodeSnippet();
        snippet.setFilePath(toRepositoryRelativePath(repoRoot, targetFile));
        snippet.setStartLine(startLine);
        snippet.setEndLine(Math.max(startLine, actualEndLine));
        snippet.setContentWithLineNumbers(content.toString());
        return snippet;
    }

    private Path resolveRepositoryRoot(String repoPath) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new IllegalArgumentException("repoPath must not be blank");
        }
        Path repoRoot = Path.of(repoPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(repoRoot)) {
            throw new IllegalArgumentException("repoPath must be an existing directory: " + repoRoot);
        }
        return repoRoot;
    }

    private Path resolveRepositoryFile(Path repoRoot, String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath must not be blank");
        }

        Path candidate = Path.of(filePath);
        Path targetFile = candidate.isAbsolute()
                ? candidate.toAbsolutePath().normalize()
                : repoRoot.resolve(candidate).toAbsolutePath().normalize();
        if (!targetFile.startsWith(repoRoot)) {
            throw new IllegalArgumentException("filePath must stay inside repoPath");
        }
        if (!Files.isRegularFile(targetFile)) {
            throw new IllegalArgumentException("filePath must be an existing file inside repoPath: " + filePath);
        }
        return targetFile;
    }

    private String toRepositoryRelativePath(Path repoRoot, Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(repoRoot)) {
            throw new IllegalArgumentException("Path is outside repository: " + normalizedPath);
        }
        return repoRoot.relativize(normalizedPath).toString().replace('\\', '/');
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
