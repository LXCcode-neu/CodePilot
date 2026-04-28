package com.codepliot.service.index;

import ch.usi.si.seart.treesitter.Node;
import com.codepliot.service.index.LanguageType;
import com.codepliot.model.ParsedCodeSymbol;
import com.codepliot.model.TreeSitterParseResult;
import com.codepliot.model.CodeSymbolType;
import com.codepliot.service.index.TreeSitterNodeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
/**
 * GoSymbolExtractor 服务类，负责封装业务流程和领域能力。
 */
@Component
public class GoSymbolExtractor extends AbstractTreeSitterSymbolExtractor {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern HTTP_ROUTE_PATTERN =
            Pattern.compile("http\\.HandleFunc\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern GIN_ROUTE_PATTERN =
            Pattern.compile("router\\.(GET|POST)\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern RECEIVER_PATTERN =
            Pattern.compile("\\((.*?)\\)\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");
/**
 * 创建 GoSymbolExtractor 实例。
 */
public GoSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        super(nodeUtils);
    }
    /**
     * 执行 support 相关逻辑。
     */
@Override
    public boolean support(LanguageType languageType) {
        return LanguageType.GO == languageType;
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
                case "package_clause" -> symbols.add(buildPackageSymbol(parseResult, node));
                case "import_spec" -> symbols.add(buildImportSymbol(parseResult, node));
                case "import_declaration" -> {
                    if (findFirstChild(node, "import_spec") == null) {
                        symbols.add(buildImportSymbol(parseResult, node));
                    }
                }
                case "type_spec" -> {
                    ParsedCodeSymbol structSymbol = buildStructSymbol(parseResult, node);
                    if (structSymbol != null) {
                        symbols.add(structSymbol);
                    }
                }
                case "function_declaration" -> symbols.add(buildFunctionSymbol(parseResult, node));
                case "method_declaration" -> symbols.add(buildMethodSymbol(parseResult, node));
                case "call_expression" -> {
                    ParsedCodeSymbol routeSymbol = buildRouteSymbol(parseResult, node);
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
 * 构建Package Symbol相关逻辑。
 */
private ParsedCodeSymbol buildPackageSymbol(TreeSitterParseResult parseResult, Node node) {
        Matcher matcher = PACKAGE_PATTERN.matcher(getNodeText(node));
        String packageName = matcher.find() ? matcher.group(1) : cleanText(getNodeText(node));
        return createSymbol(parseResult, CodeSymbolType.CONFIG, packageName, null, packageName, null, null, null, node);
    }
/**
 * 构建Import Symbol相关逻辑。
 */
private ParsedCodeSymbol buildImportSymbol(TreeSitterParseResult parseResult, Node node) {
        String importText = cleanText(getNodeText(node));
        Matcher matcher = IMPORT_PATTERN.matcher(importText == null ? "" : importText);
        String importName = matcher.find() ? matcher.group(1) : importText;
        return createSymbol(parseResult, CodeSymbolType.CONFIG, importName, null, importText, null, null, importText, node);
    }
/**
 * 构建Struct Symbol相关逻辑。
 */
private ParsedCodeSymbol buildStructSymbol(TreeSitterParseResult parseResult, Node node) {
        Node typeNode = getChildByFieldName(node, "type");
        if (typeNode == null) {
            typeNode = findFirstChild(node, "struct_type");
        }
        if (!isType(typeNode, "struct_type")) {
            return null;
        }
        return createSymbol(
                parseResult,
                CodeSymbolType.CLASS,
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
 * 构建Method Symbol相关逻辑。
 */
private ParsedCodeSymbol buildMethodSymbol(TreeSitterParseResult parseResult, Node node) {
        String signature = extractSignature(node);
        return createSymbol(
                parseResult,
                CodeSymbolType.METHOD,
                extractIdentifier(node),
                extractReceiverType(signature),
                signature,
                null,
                null,
                null,
                node
        );
    }
/**
 * 构建Route Symbol相关逻辑。
 */
private ParsedCodeSymbol buildRouteSymbol(TreeSitterParseResult parseResult, Node node) {
        String text = cleanText(getNodeText(node));
        if (text == null) {
            return null;
        }

        Matcher httpMatcher = HTTP_ROUTE_PATTERN.matcher(text);
        if (httpMatcher.find()) {
            return createSymbol(
                    parseResult,
                    CodeSymbolType.ROUTE,
                    "http.HandleFunc",
                    null,
                    text,
                    null,
                    extractFirstQuotedText(httpMatcher.group(1)),
                    null,
                    node
            );
        }

        Matcher ginMatcher = GIN_ROUTE_PATTERN.matcher(text);
        if (ginMatcher.find()) {
            return createSymbol(
                    parseResult,
                    CodeSymbolType.ROUTE,
                    "router." + ginMatcher.group(1),
                    null,
                    text,
                    null,
                    extractFirstQuotedText(ginMatcher.group(2)),
                    null,
                    node
            );
        }

        return null;
    }
/**
 * 提取Receiver Type相关逻辑。
 */
private String extractReceiverType(String signature) {
        if (signature == null) {
            return null;
        }
        Matcher matcher = RECEIVER_PATTERN.matcher(signature);
        if (!matcher.find()) {
            return null;
        }
        String receiver = matcher.group(1);
        Matcher identifierMatcher = Pattern.compile("\\*?([A-Za-z_][A-Za-z0-9_]*)").matcher(receiver);
        String lastMatch = null;
        while (identifierMatcher.find()) {
            lastMatch = identifierMatcher.group(1);
        }
        return lastMatch;
    }
}
