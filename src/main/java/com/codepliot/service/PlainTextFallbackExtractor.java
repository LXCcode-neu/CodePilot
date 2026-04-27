package com.codepliot.service;

import com.codepliot.service.LanguageType;
import com.codepliot.model.ParsedCodeFile;
import com.codepliot.model.ParsedCodeSymbol;
import com.codepliot.model.TreeSitterParseResult;
import com.codepliot.model.CodeSymbolType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Component;
/**
 * PlainTextFallbackExtractor 服务类，负责封装业务流程和领域能力。
 */
@Component
public class PlainTextFallbackExtractor implements LanguageSymbolExtractor {

    private static final int CONTENT_PREVIEW_LIMIT = 2000;
    private static final String PARSE_STATUS_FALLBACK = "FALLBACK";
    private static final String PARSE_STATUS_PARSED = "PARSED";
    /**
     * 执行 support 相关逻辑。
     */
@Override
    public boolean support(LanguageType languageType) {
        return LanguageType.UNKNOWN == languageType;
    }
/**
 * 构建Parsed Code File相关逻辑。
 */
public ParsedCodeFile buildParsedCodeFile(Long projectId, TreeSitterParseResult parseResult) {
        String relativePath = normalizePath(parseResult.relativePath(), parseResult.filePath());
        String moduleName = extractModuleName(relativePath);
        String className = extractFileName(relativePath);
        String sourceCode = parseResult.sourceCode() == null ? "" : parseResult.sourceCode();
        String parseStatus = parseResult.success() ? PARSE_STATUS_PARSED : PARSE_STATUS_FALLBACK;

        return new ParsedCodeFile(
                projectId,
                relativePath,
                parseResult.language(),
                null,
                moduleName,
                className,
                sha256(sourceCode),
                (long) sourceCode.length(),
                parseStatus,
                parseResult.errorMessage()
        );
    }
    /**
     * 执行 extract 相关逻辑。
     */
@Override
    public List<ParsedCodeSymbol> extract(TreeSitterParseResult parseResult) {
        String relativePath = normalizePath(parseResult.relativePath(), parseResult.filePath());
        String sourceCode = parseResult.sourceCode() == null ? "" : parseResult.sourceCode();

        return List.of(new ParsedCodeSymbol(
                null,
                parseResult.language() == null ? LanguageType.UNKNOWN : parseResult.language(),
                relativePath,
                CodeSymbolType.UNKNOWN,
                extractFileName(relativePath),
                null,
                null,
                null,
                null,
                null,
                1,
                estimateEndLine(sourceCode),
                truncateContent(sourceCode)
        ));
    }
/**
 * 规范化Path相关逻辑。
 */
private String normalizePath(String relativePath, String filePath) {
        if (relativePath != null && !relativePath.isBlank()) {
            return relativePath;
        }
        return filePath == null ? "" : filePath;
    }
/**
 * 提取Module Name相关逻辑。
 */
private String extractModuleName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        String normalized = filePath.replace('\\', '/');
        int index = normalized.indexOf('/');
        if (index <= 0) {
            return null;
        }
        return normalized.substring(0, index);
    }
/**
 * 提取File Name相关逻辑。
 */
private String extractFileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "unknown";
        }
        String normalized = filePath.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }
/**
 * 执行 truncateContent 相关逻辑。
 */
private String truncateContent(String sourceCode) {
        if (sourceCode.length() <= CONTENT_PREVIEW_LIMIT) {
            return sourceCode;
        }
        return sourceCode.substring(0, CONTENT_PREVIEW_LIMIT);
    }
/**
 * 执行 estimateEndLine 相关逻辑。
 */
private int estimateEndLine(String sourceCode) {
        if (sourceCode.isEmpty()) {
            return 1;
        }
        return sourceCode.split("\\R", -1).length;
    }
/**
 * 执行 sha256 相关逻辑。
 */
private String sha256(String sourceCode) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(sourceCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}

