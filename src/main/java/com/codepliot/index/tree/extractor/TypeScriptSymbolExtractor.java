package com.codepliot.index.tree.extractor;

import com.codepliot.index.detect.LanguageType;
import com.codepliot.index.tree.TreeSitterNodeUtils;
import org.springframework.stereotype.Component;

/**
 * TypeScript 符号提取器。
 */
@Component
public class TypeScriptSymbolExtractor extends AbstractJavaScriptLikeSymbolExtractor {

    public TypeScriptSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        super(nodeUtils);
    }

    @Override
    public boolean support(LanguageType languageType) {
        return LanguageType.TYPESCRIPT == languageType;
    }
}
