package com.codepliot.index.tree.extractor;

import com.codepliot.index.detect.LanguageType;
import com.codepliot.index.dto.ParsedCodeSymbol;
import com.codepliot.index.dto.TreeSitterParseResult;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 多语言符号提取器注册表。
 * 负责按语言分发到具体提取器，并在无提取器或单文件提取失败时回退到文本兜底提取。
 */
@Component
public class LanguageSymbolExtractorRegistry {

    private final Map<LanguageType, LanguageSymbolExtractor> registry;
    private final PlainTextFallbackExtractor fallbackExtractor;

    public LanguageSymbolExtractorRegistry(List<LanguageSymbolExtractor> extractors,
                                           PlainTextFallbackExtractor fallbackExtractor) {
        this.fallbackExtractor = fallbackExtractor;
        this.registry = new EnumMap<>(LanguageType.class);
        for (LanguageSymbolExtractor extractor : extractors) {
            if (extractor == fallbackExtractor) {
                continue;
            }
            for (LanguageType languageType : LanguageType.values()) {
                if (extractor.support(languageType)) {
                    registry.put(languageType, extractor);
                }
            }
        }
    }

    /**
     * 根据语言获取提取器，不存在时返回文本兜底提取器。
     */
    public LanguageSymbolExtractor get(LanguageType languageType) {
        if (languageType == null) {
            return fallbackExtractor;
        }
        return registry.getOrDefault(languageType, fallbackExtractor);
    }

    /**
     * 对单文件执行安全提取。
     */
    public List<ParsedCodeSymbol> extract(TreeSitterParseResult parseResult) {
        if (parseResult == null) {
            return List.of();
        }
        if (!parseResult.success()) {
            return fallbackExtractor.extract(parseResult);
        }

        LanguageSymbolExtractor extractor = get(parseResult.language());
        try {
            return extractor.extract(parseResult);
        } catch (Exception exception) {
            return fallbackExtractor.extract(parseResult);
        }
    }
}
