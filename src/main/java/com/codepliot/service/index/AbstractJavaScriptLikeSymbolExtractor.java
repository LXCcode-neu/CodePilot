package com.codepliot.service.index;

import ch.usi.si.seart.treesitter.Node;
import com.codepliot.model.ParsedCodeSymbol;
import com.codepliot.model.TreeSitterParseResult;
import com.codepliot.model.CodeSymbolType;
import com.codepliot.service.index.TreeSitterNodeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * AbstractJavaScriptLikeSymbolExtractor 服务类，负责封装业务流程和领域能力。
 */
public abstract class AbstractJavaScriptLikeSymbolExtractor extends AbstractTreeSitterSymbolExtractor {

    private static final Set<String> CLASS_TYPES = Set.of("class_declaration");
    private static final Pattern EXPRESS_ROUTE_PATTERN =
            Pattern.compile("(app|router)\\.(get|post)\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern NEST_CONTROLLER_PATTERN =
            Pattern.compile("@Controller\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern NEST_ROUTE_PATTERN =
            Pattern.compile("@(Get|Post|Put|Delete)\\s*\\((.*?)\\)", Pattern.DOTALL);
/**
 * 创建 AbstractJavaScriptLikeSymbolExtractor 实例。
 */
protected AbstractJavaScriptLikeSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        super(nodeUtils);
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
                case "import_statement" -> symbols.add(buildImportSymbol(parseResult, node));
                case "lexical_declaration", "variable_declaration" -> symbols.addAll(buildDeclarationSymbols(parseResult, node));
                case "function_declaration" -> symbols.add(buildFunctionSymbol(parseResult, node));
                case "class_declaration" -> symbols.add(buildClassSymbol(parseResult, node));
                case "method_definition" -> symbols.addAll(buildMethodSymbols(parseResult, node));
                case "call_expression" -> {
                    ParsedCodeSymbol routeSymbol = buildExpressRouteSymbol(parseResult, node);
                    if (routeSymbol != null) {
                        symbols.add(routeSymbol);
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
 * 构建Declaration Symbols相关逻辑。
 */
private List<ParsedCodeSymbol> buildDeclarationSymbols(TreeSitterParseResult parseResult, Node declarationNode) {
        List<ParsedCodeSymbol> symbols = new ArrayList<>();
        walkVariableDeclarators(declarationNode, variableDeclarator -> {
            String valueType = getNodeType(getChildByFieldName(variableDeclarator, "value"));
            String text = cleanText(getNodeText(variableDeclarator));
            if (text != null && text.contains("require(")) {
                symbols.add(createSymbol(
                        parseResult,
                        CodeSymbolType.CONFIG,
                        extractIdentifier(variableDeclarator),
                        null,
                        text,
                        null,
                        null,
                        text,
                        variableDeclarator
                ));
                return;
            }

            if ("arrow_function".equals(valueType) || "function_expression".equals(valueType)) {
                symbols.add(createSymbol(
                        parseResult,
                        CodeSymbolType.FUNCTION,
                        extractIdentifier(variableDeclarator),
                        null,
                        text,
                        null,
                        null,
                        null,
                        variableDeclarator
                ));
            }
        });
        return symbols;
    }
/**
 * 构建Function Symbol相关逻辑。
 */
private ParsedCodeSymbol buildFunctionSymbol(TreeSitterParseResult parseResult, Node node) {
        return createSymbol(
                parseResult,
                CodeSymbolType.FUNCTION,
                extractIdentifier(node),
                null,
                extractSignature(node),
                null,
                null,
                null,
                node
        );
    }
/**
 * 构建Class Symbol相关逻辑。
 */
private ParsedCodeSymbol buildClassSymbol(TreeSitterParseResult parseResult, Node node) {
        String decorators = joinTexts(collectChildTexts(node, "decorator"));
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
 * 构建Method Symbols相关逻辑。
 */
private List<ParsedCodeSymbol> buildMethodSymbols(TreeSitterParseResult parseResult, Node node) {
        List<ParsedCodeSymbol> symbols = new ArrayList<>();
        String methodName = extractIdentifier(node);
        String parentSymbol = findNearestAncestorName(node, CLASS_TYPES);
        String decorators = joinTexts(collectChildTexts(node, "decorator"));

        symbols.add(createSymbol(
                parseResult,
                CodeSymbolType.METHOD,
                methodName,
                parentSymbol,
                extractSignature(node),
                decorators,
                null,
                null,
                node
        ));

        if (containsNestRouteDecorator(decorators)) {
            String routePath = extractNestRoutePath(decorators);
            String controllerPath = findNearestControllerRoute(node);
            symbols.add(createSymbol(
                    parseResult,
                    CodeSymbolType.ROUTE,
                    methodName,
                    parentSymbol,
                    extractSignature(node),
                    decorators,
                    combineRoutePath(controllerPath, routePath),
                    null,
                    node
            ));
        }

        return symbols;
    }
/**
 * 构建Express Route Symbol相关逻辑。
 */
private ParsedCodeSymbol buildExpressRouteSymbol(TreeSitterParseResult parseResult, Node node) {
        String text = cleanText(getNodeText(node));
        if (text == null) {
            return null;
        }
        Matcher matcher = EXPRESS_ROUTE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String calleeText = cleanText(matcher.group(1) + "." + matcher.group(2));
        String routePath = extractFirstQuotedText(matcher.group(3));
        return createSymbol(
                parseResult,
                CodeSymbolType.ROUTE,
                calleeText,
                null,
                text,
                null,
                routePath,
                null,
                node
        );
    }
/**
 * 执行 walkVariableDeclarators 相关逻辑。
 */
private void walkVariableDeclarators(Node node, java.util.function.Consumer<Node> consumer) {
        if (node == null || consumer == null) {
            return;
        }
        if ("variable_declarator".equals(getNodeType(node))) {
            consumer.accept(node);
        }
        for (Node child : getChildren(node)) {
            walkVariableDeclarators(child, consumer);
        }
    }
/**
 * 查找Nearest Controller Route相关逻辑。
 */
private String findNearestControllerRoute(Node node) {
        Node current = getParent(node);
        while (current != null) {
            if ("class_declaration".equals(getNodeType(current))) {
                String decorators = joinTexts(collectChildTexts(current, "decorator"));
                Matcher matcher = NEST_CONTROLLER_PATTERN.matcher(decorators == null ? "" : decorators);
                if (matcher.find()) {
                    return extractFirstQuotedText(matcher.group(1));
                }
            }
            current = getParent(current);
        }
        return null;
    }
/**
 * 提取Nest Route Path相关逻辑。
 */
private String extractNestRoutePath(String decorators) {
        if (decorators == null) {
            return null;
        }
        Matcher matcher = NEST_ROUTE_PATTERN.matcher(decorators);
        if (!matcher.find()) {
            return null;
        }
        return extractFirstQuotedText(matcher.group(2));
    }
/**
 * 执行 containsNestRouteDecorator 相关逻辑。
 */
private boolean containsNestRouteDecorator(String decorators) {
        return decorators != null && NEST_ROUTE_PATTERN.matcher(decorators).find();
    }
}
