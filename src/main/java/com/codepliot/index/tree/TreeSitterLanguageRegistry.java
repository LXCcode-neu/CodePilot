package com.codepliot.index.tree;

import ch.usi.si.seart.treesitter.Language;
import ch.usi.si.seart.treesitter.LibraryLoader;
import ch.usi.si.seart.treesitter.Parser;
import com.codepliot.index.detect.LanguageType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Tree-sitter 语言注册表。
 * 负责维护 CodePilot 语言枚举到 Tree-sitter 语言映射、可用性以及 Parser 创建入口。
 */
@Component
public class TreeSitterLanguageRegistry {

    private final Map<LanguageType, TreeSitterLanguageHandle> registry;
    private final boolean nativeLibraryLoaded;
    private final String nativeLibraryErrorMessage;

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
     * 根据业务语言类型查询对应的 Tree-sitter 配置。
     */
    public TreeSitterLanguageHandle get(LanguageType languageType) {
        return registry.getOrDefault(languageType, unavailable(languageType, "Unsupported language mapping"));
    }

    /**
     * 获取当前是否已成功加载 Tree-sitter native 库。
     */
    public boolean isNativeLibraryLoaded() {
        return nativeLibraryLoaded;
    }

    /**
     * 获取 native 库加载失败原因。
     */
    public Optional<String> getNativeLibraryErrorMessage() {
        return Optional.ofNullable(nativeLibraryErrorMessage);
    }

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

    private TreeSitterLanguageHandle create(LanguageType languageType, Language treeSitterLanguage) {
        if (!nativeLibraryLoaded) {
            return unavailable(languageType, "Tree-sitter native library is unavailable: " + nativeLibraryErrorMessage);
        }
        return new TreeSitterLanguageHandle(languageType, treeSitterLanguage, true, null);
    }

    private TreeSitterLanguageHandle unavailable(LanguageType languageType, String reason) {
        return new TreeSitterLanguageHandle(languageType, null, false, reason);
    }

    private String buildErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }

    /**
     * 单个语言注册信息。
     * 包含语言映射、可用性状态以及创建 Parser 的能力。
     */
    public record TreeSitterLanguageHandle(
            LanguageType languageType,
            Language treeSitterLanguage,
            boolean available,
            String unavailableReason
    ) {

        /**
         * 为当前语言创建一个新的 Parser。
         */
        public Parser createParser() {
            if (!available || treeSitterLanguage == null) {
                throw new IllegalStateException(unavailableReason == null ? "Parser is unavailable" : unavailableReason);
            }
            return Parser.getFor(treeSitterLanguage);
        }
    }
}
