package com.codepliot.service.index;

import com.codepliot.service.index.LanguageType;
import com.codepliot.model.ParsedCodeSymbol;
import com.codepliot.model.TreeSitterParseResult;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
/**
 * LanguageSymbolExtractorRegistry 服务类，负责封装业务流程和领域能力。
 */
@Component
public class LanguageSymbolExtractorRegistry {

    private final Map<LanguageType, LanguageSymbolExtractor> registry;
    private final PlainTextFallbackExtractor fallbackExtractor;
/**
 * 创建 LanguageSymbolExtractorRegistry 实例。
 */
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
 * 执行 get 相关逻辑。
 */
public LanguageSymbolExtractor get(LanguageType languageType) {
        if (languageType == null) {
            return fallbackExtractor;
        }
        return registry.getOrDefault(languageType, fallbackExtractor);
    }
/**
 * 执行 extract 相关逻辑。
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
