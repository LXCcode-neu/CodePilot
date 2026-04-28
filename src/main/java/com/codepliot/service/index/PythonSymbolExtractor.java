package com.codepliot.service.index;

import ch.usi.si.seart.treesitter.Node;
import com.codepliot.service.index.LanguageType;
import com.codepliot.model.ParsedCodeSymbol;
import com.codepliot.model.TreeSitterParseResult;
import com.codepliot.model.CodeSymbolType;
import com.codepliot.service.index.TreeSitterNodeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
/**
 * PythonSymbolExtractor 服务类，负责封装业务流程和领域能力。
 */
@Component
public class PythonSymbolExtractor extends AbstractTreeSitterSymbolExtractor {

    private static final Set<String> CLASS_TYPES = Set.of("class_definition");
    private static final Pattern ROUTE_DECORATOR_PATTERN =
            Pattern.compile("@(?:app|router)\\.(get|post)\\s*\\((.*?)\\)", Pattern.DOTALL);
/**
 * 创建 PythonSymbolExtractor 实例。
 */
public PythonSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        super(nodeUtils);
    }
    /**
     * 执行 support 相关逻辑。
     */
@Override
    public boolean support(LanguageType languageType) {
        return LanguageType.PYTHON == languageType;
    }
    /**
     * 执行 extract 相关逻辑。
     */
@Override
    public List<ParsedCodeSymbol> extract(TreeSitterParseResult parseResult) {
        List<ParsedCodeSymbol> symbols = new ArrayList<>();
        Node rootNode = getRootNode(parseResult);
        if (rootNode == null) {
            return symbols;
        }

        walk(rootNode, node -> {
            String nodeType = getNodeType(node);
            switch (nodeType) {
                case "import_statement", "import_from_statement" -> symbols.add(buildImportSymbol(parseResult, node));
                case "decorated_definition" -> symbols.addAll(buildDecoratedSymbols(parseResult, node));
                case "class_definition" -> {
                    if (!isType(getParent(node), "decorated_definition")) {
                        symbols.add(buildClassSymbol(parseResult, node, null));
                    }
                }
                case "function_definition" -> {
                    if (!isType(getParent(node), "decorated_definition")) {
                        symbols.add(buildFunctionSymbol(parseResult, node, null));
                    }
                }
                default -> {
                }
            }
        });

        return symbols.stream().filter(symbol -> symbol != null && symbol.symbolName() != null).toList();
    }
/**
 * 构建Import Symbol相关逻辑。
 */
private ParsedCodeSymbol buildImportSymbol(TreeSitterParseResult parseResult, Node node) {
        String importText = cleanText(getNodeText(node));
        return createSymbol(parseResult, CodeSymbolType.CONFIG, importText, null, importText, null, null, importText, node);
    }
/**
 * 构建Decorated Symbols相关逻辑。
 */
private List<ParsedCodeSymbol> buildDecoratedSymbols(TreeSitterParseResult parseResult, Node node) {
        List<ParsedCodeSymbol> symbols = new ArrayList<>();
        String decorators = joinTexts(collectChildTexts(node, "decorator"));
        Node decoratedTarget = findFirstChild(node, "class_definition", "function_definition");
        if (decoratedTarget == null) {
            return symbols;
        }

        if (isType(decoratedTarget, "class_definition")) {
            symbols.add(buildClassSymbol(parseResult, decoratedTarget, decorators));
            return symbols;
        }

        ParsedCodeSymbol functionSymbol = buildFunctionSymbol(parseResult, decoratedTarget, decorators);
        if (functionSymbol != null) {
            symbols.add(functionSymbol);
        }

        if (containsPythonRouteDecorator(decorators)) {
            String routePath = extractPythonRoute(decorators);
            symbols.add(createSymbol(
                    parseResult,
                    CodeSymbolType.ROUTE,
                    extractIdentifier(decoratedTarget),
                    findNearestAncestorName(decoratedTarget, CLASS_TYPES),
                    extractSignature(decoratedTarget),
                    decorators,
                    routePath,
                    null,
                    decoratedTarget
            ));
        }
        return symbols;
    }
/**
 * 构建Class Symbol相关逻辑。
 */
private ParsedCodeSymbol buildClassSymbol(TreeSitterParseResult parseResult, Node node, String decorators) {
        return createSymbol(
                parseResult,
                CodeSymbolType.CLASS,
                extractIdentifier(node),
                null,
                extractSignature(node),
                decorators,
                null,
                null,
                node
        );
    }
/**
 * 构建Function Symbol相关逻辑。
 */
private ParsedCodeSymbol buildFunctionSymbol(TreeSitterParseResult parseResult, Node node, String decorators) {
        String parentSymbol = findNearestAncestorName(node, CLASS_TYPES);
        CodeSymbolType symbolType = parentSymbol == null ? CodeSymbolType.FUNCTION : CodeSymbolType.METHOD;
        return createSymbol(
                parseResult,
                symbolType,
                extractIdentifier(node),
                parentSymbol,
                extractSignature(node),
                decorators,
                null,
                null,
                node
        );
    }
/**
 * 提取Python Route相关逻辑。
 */
private String extractPythonRoute(String decorators) {
        if (decorators == null) {
            return null;
        }
        Matcher matcher = ROUTE_DECORATOR_PATTERN.matcher(decorators);
        if (!matcher.find()) {
            return null;
        }
        return extractFirstQuotedText(matcher.group(2));
    }
/**
 * 执行 containsPythonRouteDecorator 相关逻辑。
 */
private boolean containsPythonRouteDecorator(String decorators) {
        return decorators != null && ROUTE_DECORATOR_PATTERN.matcher(decorators).find();
    }
}
