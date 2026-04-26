package com.codepliot.index.tree.extractor;

import com.codepliot.index.detect.LanguageType;
import com.codepliot.index.dto.ParsedCodeSymbol;
import com.codepliot.index.dto.TreeSitterParseResult;
import java.util.List;

public interface LanguageSymbolExtractor {

    boolean support(LanguageType languageType);

    List<ParsedCodeSymbol> extract(TreeSitterParseResult parseResult);
}
