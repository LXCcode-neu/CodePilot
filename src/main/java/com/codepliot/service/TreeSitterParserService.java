package com.codepliot.service;

import ch.usi.si.seart.treesitter.Parser;
import ch.usi.si.seart.treesitter.Tree;
import com.codepliot.service.LanguageDetector;
import com.codepliot.service.LanguageType;
import com.codepliot.model.TreeSitterParseResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
/**
 * TreeSitterParserService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class TreeSitterParserService {

    private final LanguageDetector languageDetector;
    private final TreeSitterLanguageRegistry languageRegistry;
/**
 * 创建 TreeSitterParserService 实例。
 */
public TreeSitterParserService(LanguageDetector languageDetector,
                                   TreeSitterLanguageRegistry languageRegistry) {
        this.languageDetector = languageDetector;
        this.languageRegistry = languageRegistry;
    }
/**
 * 执行 parse 相关逻辑。
 */
public TreeSitterParseResult parse(Path filePath) {
        LanguageType languageType = languageDetector.detect(filePath);
        return parse(filePath, languageType);
    }
/**
 * 执行 parse 相关逻辑。
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
/**
 * 读取Source Code相关逻辑。
 */
private String readSourceCode(Path filePath) throws IOException {
        return java.nio.file.Files.readString(filePath, StandardCharsets.UTF_8);
    }
/**
 * 规范化Path相关逻辑。
 */
private String normalizePath(Path filePath) {
        return filePath == null ? "" : filePath.toString().replace('\\', '/');
    }
/**
 * 构建Error Message相关逻辑。
 */
private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}

