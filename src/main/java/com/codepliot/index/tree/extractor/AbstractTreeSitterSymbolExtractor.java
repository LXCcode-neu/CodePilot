package com.codepliot.index.tree.extractor;

import ch.usi.si.seart.treesitter.Node;
import com.codepliot.index.dto.ParsedCodeSymbol;
import com.codepliot.index.dto.TreeSitterParseResult;
import com.codepliot.index.enums.CodeSymbolType;
import com.codepliot.index.tree.TreeSitterNodeUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tree-sitter 符号提取公共基类。
 * 只沉淀遍历、取名、字符串清洗和统一结果构造等通用能力，避免把多语言规则堆到一个类里。
 */
public abstract class AbstractTreeSitterSymbolExtractor implements LanguageSymbolExtractor {

    private static final int CONTENT_LIMIT = 4000;
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_.$]*");
    private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile("['\"`](.*?)['\"`]");

    protected final TreeSitterNodeUtils nodeUtils;

    protected AbstractTreeSitterSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        this.nodeUtils = nodeUtils;
    }

    protected Node getRootNode(TreeSitterParseResult parseResult) {
        return nodeUtils.getRootNode(parseResult);
    }

    protected void walk(Node node, Consumer<Node> consumer) {
        if (node == null || consumer == null) {
            return;
        }
        consumer.accept(node);
        for (Node child : nodeUtils.getChildren(node)) {
            walk(child, consumer);
        }
    }

    protected String getNodeType(Node node) {
        return nodeUtils.getNodeType(node);
    }

    protected String getNodeText(Node node) {
        return nodeUtils.getNodeText(node);
    }

    protected int getStartLine(Node node) {
        return nodeUtils.getStartLine(node);
    }

    protected int getEndLine(Node node) {
        return nodeUtils.getEndLine(node);
    }

    protected List<Node> getChildren(Node node) {
        return nodeUtils.getChildren(node);
    }

    protected List<Node> getNamedChildren(Node node) {
        return nodeUtils.getNamedChildren(node);
    }

    protected Node getParent(Node node) {
        return nodeUtils.getParent(node);
    }

    protected Node getChildByFieldName(Node node, String fieldName) {
        return nodeUtils.getChildByFieldName(node, fieldName);
    }

    protected boolean isType(Node node, String expectedType) {
        return nodeUtils.isType(node, expectedType);
    }

    protected String normalizePath(TreeSitterParseResult parseResult) {
        if (parseResult == null) {
            return "";
        }
        if (parseResult.relativePath() != null && !parseResult.relativePath().isBlank()) {
            return parseResult.relativePath();
        }
        return parseResult.filePath() == null ? "" : parseResult.filePath();
    }

    protected ParsedCodeSymbol createSymbol(TreeSitterParseResult parseResult,
                                            CodeSymbolType symbolType,
                                            String symbolName,
                                            String parentSymbol,
                                            String signature,
                                            String annotations,
                                            String routePath,
                                            String importText,
                                            Node node) {
        return new ParsedCodeSymbol(
                null,
                parseResult.language(),
                normalizePath(parseResult),
                symbolType,
                symbolName,
                parentSymbol,
                cleanText(signature),
                cleanText(annotations),
                cleanText(routePath),
                cleanText(importText),
                node == null ? null : getStartLine(node),
                node == null ? null : getEndLine(node),
                truncateContent(cleanText(getNodeText(node)))
        );
    }

    protected ParsedCodeSymbol createSymbol(TreeSitterParseResult parseResult,
                                            CodeSymbolType symbolType,
                                            String symbolName,
                                            String parentSymbol,
                                            String signature,
                                            String annotations,
                                            String routePath,
                                            String importText,
                                            Integer startLine,
                                            Integer endLine,
                                            String content) {
        return new ParsedCodeSymbol(
                null,
                parseResult.language(),
                normalizePath(parseResult),
                symbolType,
                symbolName,
                parentSymbol,
                cleanText(signature),
                cleanText(annotations),
                cleanText(routePath),
                cleanText(importText),
                startLine,
                endLine,
                truncateContent(cleanText(content))
        );
    }

    protected String extractIdentifier(Node node) {
        if (node == null) {
            return null;
        }

        Node nameNode = getChildByFieldName(node, "name");
        if (nameNode != null && nameNode != node) {
            String identifier = extractIdentifier(nameNode);
            if (identifier != null) {
                return identifier;
            }
        }

        Node propertyNode = getChildByFieldName(node, "property");
        if (propertyNode != null && propertyNode != node) {
            String identifier = extractIdentifier(propertyNode);
            if (identifier != null) {
                return identifier;
            }
        }

        Node functionNode = getChildByFieldName(node, "function");
        if (functionNode != null && functionNode != node) {
            String identifier = extractIdentifier(functionNode);
            if (identifier != null) {
                return identifier;
            }
        }

        String text = cleanText(getNodeText(node));
        if (text != null && isSimpleIdentifier(text)) {
            return text;
        }

        for (Node child : getNamedChildren(node)) {
            String identifier = extractIdentifier(child);
            if (identifier != null) {
                return identifier;
            }
        }

        if (text == null) {
            return null;
        }
        Matcher matcher = IDENTIFIER_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    protected String extractSignature(Node node) {
        String text = cleanText(getNodeText(node));
        if (text == null) {
            return null;
        }
        int bodyIndex = text.indexOf('{');
        if (bodyIndex > 0) {
            return text.substring(0, bodyIndex).trim();
        }
        return text;
    }

    protected String cleanText(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.trim().replace("\r\n", "\n");
        return cleaned.isEmpty() ? null : cleaned;
    }

    protected String truncateContent(String text) {
        if (text == null || text.length() <= CONTENT_LIMIT) {
            return text;
        }
        return text.substring(0, CONTENT_LIMIT);
    }

    protected String findNearestAncestorName(Node node, Set<String> containerTypes) {
        Node current = getParent(node);
        while (current != null) {
            if (containerTypes.contains(getNodeType(current))) {
                return extractIdentifier(current);
            }
            current = getParent(current);
        }
        return null;
    }

    protected Node findFirstChild(Node node, String... expectedTypes) {
        if (node == null || expectedTypes == null || expectedTypes.length == 0) {
            return null;
        }
        Set<String> typeSet = Set.of(expectedTypes);
        for (Node child : getChildren(node)) {
            if (typeSet.contains(getNodeType(child))) {
                return child;
            }
        }
        return null;
    }

    protected String joinTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return null;
        }
        List<String> normalized = texts.stream()
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return null;
        }
        return String.join("\n", normalized);
    }

    protected List<String> collectChildTexts(Node node, String... childTypes) {
        if (node == null || childTypes == null || childTypes.length == 0) {
            return List.of();
        }
        Set<String> expectedTypes = Set.of(childTypes);
        List<String> texts = new ArrayList<>();
        for (Node child : getChildren(node)) {
            if (expectedTypes.contains(getNodeType(child))) {
                String text = cleanText(getNodeText(child));
                if (text != null) {
                    texts.add(text);
                }
            }
        }
        return texts;
    }

    protected String extractFirstQuotedText(String text) {
        String normalized = cleanText(text);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = QUOTED_TEXT_PATTERN.matcher(normalized);
        return matcher.find() ? matcher.group(1) : null;
    }

    protected String extractArgumentValue(String text, String argumentName) {
        String normalized = cleanText(text);
        if (normalized == null || argumentName == null || argumentName.isBlank()) {
            return null;
        }

        Pattern namedArgumentPattern = Pattern.compile(argumentName + "\\s*=\\s*['\"`](.*?)['\"`]");
        Matcher matcher = namedArgumentPattern.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return extractFirstQuotedText(normalized);
    }

    protected String toLowerCase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    protected String combineRoutePath(String prefix, String methodPath) {
        String normalizedPrefix = cleanText(prefix);
        String normalizedMethodPath = cleanText(methodPath);
        if (normalizedPrefix == null) {
            return normalizedMethodPath;
        }
        if (normalizedMethodPath == null) {
            return normalizedPrefix;
        }
        if (normalizedPrefix.endsWith("/") && normalizedMethodPath.startsWith("/")) {
            return normalizedPrefix.substring(0, normalizedPrefix.length() - 1) + normalizedMethodPath;
        }
        if (!normalizedPrefix.endsWith("/") && !normalizedMethodPath.startsWith("/")) {
            return normalizedPrefix + "/" + normalizedMethodPath;
        }
        return normalizedPrefix + normalizedMethodPath;
    }

    protected Set<String> newOrderedSet(String... values) {
        Set<String> set = new LinkedHashSet<>();
        if (values == null) {
            return set;
        }
        for (String value : values) {
            if (value != null) {
                set.add(value);
            }
        }
        return set;
    }

    private boolean isSimpleIdentifier(String text) {
        return text.indexOf('\n') < 0 && IDENTIFIER_PATTERN.matcher(text).matches();
    }
}
