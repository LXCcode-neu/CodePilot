package com.codepliot.service;

import com.codepliot.service.LanguageType;
import com.codepliot.service.TreeSitterNodeUtils;
import org.springframework.stereotype.Component;
/**
 * JavaScriptSymbolExtractor 服务类，负责封装业务流程和领域能力。
 */
@Component
public class JavaScriptSymbolExtractor extends AbstractJavaScriptLikeSymbolExtractor {
/**
 * 创建 JavaScriptSymbolExtractor 实例。
 */
public JavaScriptSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        super(nodeUtils);
    }
    /**
     * 执行 support 相关逻辑。
     */
@Override
    public boolean support(LanguageType languageType) {
        return LanguageType.JAVASCRIPT == languageType;
    }
}

