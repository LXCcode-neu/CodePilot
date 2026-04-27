package com.codepliot.service;

import com.codepliot.service.LanguageType;
import com.codepliot.model.ParsedCodeSymbol;
import com.codepliot.model.TreeSitterParseResult;
import java.util.List;
/**
 * LanguageSymbolExtractor 服务类，负责封装业务流程和领域能力。
 */
public interface LanguageSymbolExtractor {
boolean support(LanguageType languageType);
List<ParsedCodeSymbol> extract(TreeSitterParseResult parseResult);
}

