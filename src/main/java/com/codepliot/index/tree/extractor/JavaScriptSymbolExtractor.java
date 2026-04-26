package com.codepliot.index.tree.extractor;

import com.codepliot.index.detect.LanguageType;
import com.codepliot.index.tree.TreeSitterNodeUtils;
import org.springframework.stereotype.Component;

/**
 * JavaScript 符号提取器。
 */
@Component
public class JavaScriptSymbolExtractor extends AbstractJavaScriptLikeSymbolExtractor {

    public JavaScriptSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        super(nodeUtils);
    }

    @Override
    public boolean support(LanguageType languageType) {
        return LanguageType.JAVASCRIPT == languageType;
    }
}
