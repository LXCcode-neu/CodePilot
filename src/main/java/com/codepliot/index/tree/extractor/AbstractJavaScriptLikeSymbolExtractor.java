package com.codepliot.index.tree.extractor;

import ch.usi.si.seart.treesitter.Node;
import com.codepliot.index.dto.ParsedCodeSymbol;
import com.codepliot.index.dto.TreeSitterParseResult;
import com.codepliot.index.enums.CodeSymbolType;
import com.codepliot.index.tree.TreeSitterNodeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JavaScript / TypeScript 公共提取基类。
 */
public abstract class AbstractJavaScriptLikeSymbolExtractor extends AbstractTreeSitterSymbolExtractor {

    private static final Set<String> CLASS_TYPES = Set.of("class_declaration");
    private static final Pattern EXPRESS_ROUTE_PATTERN =
            Pattern.compile("(app|router)\\.(get|post)\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern NEST_CONTROLLER_PATTERN =
            Pattern.compile("@Controller\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern NEST_ROUTE_PATTERN =
            Pattern.compile("@(Get|Post|Put|Delete)\\s*\\((.*?)\\)", Pattern.DOTALL);

    protected AbstractJavaScriptLikeSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        super(nodeUtils);
    }

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

    private ParsedCodeSymbol buildImportSymbol(TreeSitterParseResult parseResult, Node node) {
        String importText = cleanText(getNodeText(node));
        return createSymbol(parseResult, CodeSymbolType.CONFIG, importText, null, importText, null, null, importText, node);
    }

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

    private boolean containsNestRouteDecorator(String decorators) {
        return decorators != null && NEST_ROUTE_PATTERN.matcher(decorators).find();
    }
}
