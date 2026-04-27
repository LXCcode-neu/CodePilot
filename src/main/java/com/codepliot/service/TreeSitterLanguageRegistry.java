package com.codepliot.service;

import ch.usi.si.seart.treesitter.Language;
import ch.usi.si.seart.treesitter.LibraryLoader;
import ch.usi.si.seart.treesitter.Parser;
import com.codepliot.service.LanguageType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
/**
 * TreeSitterLanguageRegistry 服务类，负责封装业务流程和领域能力。
 */
@Component
public class TreeSitterLanguageRegistry {

    private final Map<LanguageType, TreeSitterLanguageHandle> registry;
    private final boolean nativeLibraryLoaded;
    private final String nativeLibraryErrorMessage;
/**
 * 创建 TreeSitterLanguageRegistry 实例。
 */
public TreeSitterLanguageRegistry() {
        String loadError = null;
        boolean libraryLoaded = false;
        try {
            LibraryLoader.load();
            libraryLoaded = true;
        } catch (Throwable throwable) {
            loadError = buildErrorMessage(throwable);
        }

        this.nativeLibraryLoaded = libraryLoaded;
        this.nativeLibraryErrorMessage = loadError;
        this.registry = buildRegistry();
    }
/**
 * 执行 get 相关逻辑。
 */
public TreeSitterLanguageHandle get(LanguageType languageType) {
        return registry.getOrDefault(languageType, unavailable(languageType, "Unsupported language mapping"));
    }
/**
 * 执行 isNativeLibraryLoaded 相关逻辑。
 */
public boolean isNativeLibraryLoaded() {
        return nativeLibraryLoaded;
    }
/**
 * 获取Native Library Error Message相关逻辑。
 */
public Optional<String> getNativeLibraryErrorMessage() {
        return Optional.ofNullable(nativeLibraryErrorMessage);
    }
/**
 * 构建Registry相关逻辑。
 */
private Map<LanguageType, TreeSitterLanguageHandle> buildRegistry() {
        Map<LanguageType, TreeSitterLanguageHandle> handles = new EnumMap<>(LanguageType.class);
        if (!nativeLibraryLoaded) {
            String reason = "Tree-sitter native library is unavailable: " + nativeLibraryErrorMessage;
            handles.put(LanguageType.JAVA, unavailable(LanguageType.JAVA, reason));
            handles.put(LanguageType.PYTHON, unavailable(LanguageType.PYTHON, reason));
            handles.put(LanguageType.JAVASCRIPT, unavailable(LanguageType.JAVASCRIPT, reason));
            handles.put(LanguageType.TYPESCRIPT, unavailable(LanguageType.TYPESCRIPT, reason));
            handles.put(LanguageType.GO, unavailable(LanguageType.GO, reason));
            handles.put(LanguageType.UNKNOWN, unavailable(LanguageType.UNKNOWN, "Tree-sitter parser is not defined for UNKNOWN"));
            return handles;
        }

        handles.put(LanguageType.JAVA, create(LanguageType.JAVA, Language.JAVA));
        handles.put(LanguageType.PYTHON, create(LanguageType.PYTHON, Language.PYTHON));
        handles.put(LanguageType.JAVASCRIPT, create(LanguageType.JAVASCRIPT, Language.JAVASCRIPT));
        handles.put(LanguageType.TYPESCRIPT, create(LanguageType.TYPESCRIPT, Language.TYPESCRIPT));
        handles.put(LanguageType.GO, create(LanguageType.GO, Language.GO));
        handles.put(LanguageType.UNKNOWN, unavailable(LanguageType.UNKNOWN, "Tree-sitter parser is not defined for UNKNOWN"));
        return handles;
    }
/**
 * 执行 create 相关逻辑。
 */
private TreeSitterLanguageHandle create(LanguageType languageType, Language treeSitterLanguage) {
        if (!nativeLibraryLoaded) {
            return unavailable(languageType, "Tree-sitter native library is unavailable: " + nativeLibraryErrorMessage);
        }
        return new TreeSitterLanguageHandle(languageType, treeSitterLanguage, true, null);
    }
/**
 * 执行 unavailable 相关逻辑。
 */
private TreeSitterLanguageHandle unavailable(LanguageType languageType, String reason) {
        return new TreeSitterLanguageHandle(languageType, null, false, reason);
    }
/**
 * 构建Error Message相关逻辑。
 */
private String buildErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }
/**
 * TreeSitterLanguageHandle 服务类，负责封装业务流程和领域能力。
 */
public record TreeSitterLanguageHandle(
            LanguageType languageType,
            Language treeSitterLanguage,
            boolean available,
            String unavailableReason
    ) {
/**
 * 创建Parser相关逻辑。
 */
public Parser createParser() {
            if (!available || treeSitterLanguage == null) {
                throw new IllegalStateException(unavailableReason == null ? "Parser is unavailable" : unavailableReason);
            }
            return Parser.getFor(treeSitterLanguage);
        }
    }
}

