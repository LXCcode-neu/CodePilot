package com.codepliot.index.tree.extractor;

import ch.usi.si.seart.treesitter.Node;
import com.codepliot.index.detect.LanguageType;
import com.codepliot.index.dto.ParsedCodeSymbol;
import com.codepliot.index.dto.TreeSitterParseResult;
import com.codepliot.index.enums.CodeSymbolType;
import com.codepliot.index.tree.TreeSitterNodeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Java 语言符号提取器。
 */
@Component
public class JavaSymbolExtractor extends AbstractTreeSitterSymbolExtractor {

    private static final Set<String> CLASS_LIKE_TYPES = Set.of("class_declaration", "interface_declaration");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([A-Za-z0-9_.$]+)");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+(?:static\\s+)?([A-Za-z0-9_.*$]+)");
    private static final Pattern ROUTE_ANNOTATION_PATTERN =
            Pattern.compile("@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping)\\s*(\\((.*?)\\))?", Pattern.DOTALL);

    public JavaSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        super(nodeUtils);
    }

    @Override
    public boolean support(LanguageType languageType) {
        return LanguageType.JAVA == languageType;
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
                case "package_declaration" -> symbols.add(buildPackageSymbol(parseResult, node));
                case "import_declaration" -> symbols.add(buildImportSymbol(parseResult, node));
                case "class_declaration" -> symbols.add(buildTypeSymbol(parseResult, node, CodeSymbolType.CLASS));
                case "interface_declaration" -> symbols.add(buildTypeSymbol(parseResult, node, CodeSymbolType.INTERFACE));
                case "method_declaration" -> symbols.addAll(buildMethodSymbols(parseResult, node));
                default -> {
                }
            }
        });

        return symbols.stream().filter(symbol -> symbol != null && symbol.symbolName() != null).toList();
    }

    private ParsedCodeSymbol buildPackageSymbol(TreeSitterParseResult parseResult, Node node) {
        Matcher matcher = PACKAGE_PATTERN.matcher(getNodeText(node));
        String packageName = matcher.find() ? matcher.group(1) : cleanText(getNodeText(node));
        return createSymbol(parseResult, CodeSymbolType.CONFIG, packageName, null, packageName, null, null, null, node);
    }

    private ParsedCodeSymbol buildImportSymbol(TreeSitterParseResult parseResult, Node node) {
        Matcher matcher = IMPORT_PATTERN.matcher(getNodeText(node));
        String importName = matcher.find() ? matcher.group(1) : cleanText(getNodeText(node));
        String importText = cleanText(getNodeText(node));
        return createSymbol(parseResult, CodeSymbolType.CONFIG, importName, null, importText, null, null, importText, node);
    }

    private ParsedCodeSymbol buildTypeSymbol(TreeSitterParseResult parseResult, Node node, CodeSymbolType symbolType) {
        String symbolName = extractIdentifier(node);
        String annotations = joinTexts(collectChildTexts(node, "modifiers", "annotation", "marker_annotation"));
        return createSymbol(
                parseResult,
                symbolType,
                symbolName,
                findNearestAncestorName(node, CLASS_LIKE_TYPES),
                extractSignature(node),
                annotations,
                null,
                null,
                node
        );
    }

    private List<ParsedCodeSymbol> buildMethodSymbols(TreeSitterParseResult parseResult, Node node) {
        List<ParsedCodeSymbol> symbols = new ArrayList<>();
        String methodName = extractIdentifier(node);
        String parentSymbol = findNearestAncestorName(node, CLASS_LIKE_TYPES);
        String annotations = collectJavaAnnotations(node);

        symbols.add(createSymbol(
                parseResult,
                CodeSymbolType.METHOD,
                methodName,
                parentSymbol,
                extractSignature(node),
                annotations,
                null,
                null,
                node
        ));

        String classRoute = findNearestClassRoute(node);
        if (containsSpringRouteAnnotation(annotations)) {
            String methodRoute = extractSpringRoute(annotations);
            symbols.add(createSymbol(
                    parseResult,
                    CodeSymbolType.ROUTE,
                    methodName,
                    parentSymbol,
                    extractSignature(node),
                    annotations,
                    combineRoutePath(classRoute, methodRoute),
                    null,
                    node
            ));
        }

        return symbols;
    }

    private String collectJavaAnnotations(Node node) {
        List<String> texts = new ArrayList<>();
        Node modifiers = findFirstChild(node, "modifiers");
        if (modifiers != null) {
            texts.addAll(collectChildTexts(modifiers, "annotation", "marker_annotation"));
        }
        texts.addAll(collectChildTexts(node, "annotation", "marker_annotation"));
        return joinTexts(texts);
    }

    private String findNearestClassRoute(Node node) {
        Node current = getParent(node);
        while (current != null) {
            if (CLASS_LIKE_TYPES.contains(getNodeType(current))) {
                return extractSpringRoute(collectJavaAnnotations(current));
            }
            current = getParent(current);
        }
        return null;
    }

    private String extractSpringRoute(String annotations) {
        if (annotations == null) {
            return null;
        }
        Matcher matcher = ROUTE_ANNOTATION_PATTERN.matcher(annotations);
        while (matcher.find()) {
            String annotationName = matcher.group(1);
            String arguments = matcher.group(3);
            if ("RequestMapping".equals(annotationName)) {
                return extractArgumentValue(arguments, "path");
            }
            return extractFirstQuotedText(arguments);
        }
        return null;
    }

    private boolean containsSpringRouteAnnotation(String annotations) {
        return annotations != null && ROUTE_ANNOTATION_PATTERN.matcher(annotations).find();
    }
}
