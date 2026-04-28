package com.codepliot.service.index;

import com.codepliot.service.index.LanguageType;
import com.codepliot.service.index.TreeSitterNodeUtils;
import org.springframework.stereotype.Component;
/**
 * TypeScriptSymbolExtractor 服务类，负责封装业务流程和领域能力。
 */
@Component
public class TypeScriptSymbolExtractor extends AbstractJavaScriptLikeSymbolExtractor {
/**
 * 创建 TypeScriptSymbolExtractor 实例。
 */
public TypeScriptSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        super(nodeUtils);
    }
    /**
     * 执行 support 相关逻辑。
     */
@Override
    public boolean support(LanguageType languageType) {
        return LanguageType.TYPESCRIPT == languageType;
    }
}
