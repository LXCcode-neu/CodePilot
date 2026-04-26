package com.codepliot.index.tree;

import ch.usi.si.seart.treesitter.Parser;
import ch.usi.si.seart.treesitter.Tree;
import com.codepliot.index.detect.LanguageDetector;
import com.codepliot.index.detect.LanguageType;
import com.codepliot.index.dto.TreeSitterParseResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

/**
 * Tree-sitter 统一解析服务。
 * 负责读取源码、按语言创建 Parser，并将解析结果封装为统一的 TreeSitterParseResult。
 */
@Service
public class TreeSitterParserService {

    private final LanguageDetector languageDetector;
    private final TreeSitterLanguageRegistry languageRegistry;

    public TreeSitterParserService(LanguageDetector languageDetector,
                                   TreeSitterLanguageRegistry languageRegistry) {
        this.languageDetector = languageDetector;
        this.languageRegistry = languageRegistry;
    }

    /**
     * 根据文件路径自动探测语言并执行解析。
     */
    public TreeSitterParseResult parse(Path filePath) {
        LanguageType languageType = languageDetector.detect(filePath);
        return parse(filePath, languageType);
    }

    /**
     * 使用指定语言执行解析。
     * 单文件失败时不会抛出导致整个项目失败，而是返回 success=false 和 errorMessage。
     */
    public TreeSitterParseResult parse(Path filePath, LanguageType languageType) {
        String normalizedPath = normalizePath(filePath);
        String sourceCode;
        try {
            sourceCode = readSourceCode(filePath);
        } catch (IOException exception) {
            return new TreeSitterParseResult(
                    languageType,
                    normalizedPath,
                    normalizedPath,
                    null,
                    false,
                    "Failed to read source file: " + buildErrorMessage(exception),
                    null
            );
        }

        TreeSitterLanguageRegistry.TreeSitterLanguageHandle handle = languageRegistry.get(languageType);
        if (!handle.available()) {
            return new TreeSitterParseResult(
                    languageType,
                    normalizedPath,
                    normalizedPath,
                    sourceCode,
                    false,
                    handle.unavailableReason(),
                    null
            );
        }

        try {
            Tree tree;
            try (Parser parser = handle.createParser()) {
                tree = parser.parse(sourceCode);
            }
            if (tree == null) {
                return new TreeSitterParseResult(
                        languageType,
                        normalizedPath,
                        normalizedPath,
                        sourceCode,
                        false,
                        "Tree-sitter returned null AST",
                        null
                );
            }
            return new TreeSitterParseResult(
                    languageType,
                    normalizedPath,
                    normalizedPath,
                    sourceCode,
                    true,
                    null,
                    tree
            );
        } catch (Exception exception) {
            return new TreeSitterParseResult(
                    languageType,
                    normalizedPath,
                    normalizedPath,
                    sourceCode,
                    false,
                    "Tree-sitter parse failed: " + buildErrorMessage(exception),
                    null
            );
        }
    }

    private String readSourceCode(Path filePath) throws IOException {
        return java.nio.file.Files.readString(filePath, StandardCharsets.UTF_8);
    }

    private String normalizePath(Path filePath) {
        return filePath == null ? "" : filePath.toString().replace('\\', '/');
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
