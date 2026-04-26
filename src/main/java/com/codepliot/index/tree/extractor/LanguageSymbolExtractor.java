package com.codepliot.index.tree.extractor;

import com.codepliot.index.detect.LanguageType;
import com.codepliot.index.dto.ParsedCodeSymbol;
import com.codepliot.index.dto.TreeSitterParseResult;
import java.util.List;

/**
 * 语言符号提取器抽象。
 * 每种语言单独实现自己的提取逻辑，避免多语言解析堆在一个类中。
 */
public interface LanguageSymbolExtractor {

    /**
     * 判断当前提取器是否支持目标语言。
     */
    boolean support(LanguageType languageType);

    /**
     * 从统一解析结果中抽取符号列表。
     */
    List<ParsedCodeSymbol> extract(TreeSitterParseResult parseResult);
}
