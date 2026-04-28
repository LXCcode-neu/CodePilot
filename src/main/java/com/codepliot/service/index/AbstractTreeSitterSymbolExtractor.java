package com.codepliot.service.index;

import ch.usi.si.seart.treesitter.Node;
import com.codepliot.model.ParsedCodeSymbol;
import com.codepliot.model.TreeSitterParseResult;
import com.codepliot.model.CodeSymbolType;
import com.codepliot.service.index.TreeSitterNodeUtils;
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
 * AbstractTreeSitterSymbolExtractor 服务类，负责封装业务流程和领域能力。
 */
public abstract class AbstractTreeSitterSymbolExtractor implements LanguageSymbolExtractor {

    private static final int CONTENT_LIMIT = 4000;
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_.$]*");
    private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile("['\"`](.*?)['\"`]");

    protected final TreeSitterNodeUtils nodeUtils;
/**
 * 创建 AbstractTreeSitterSymbolExtractor 实例。
 */
protected AbstractTreeSitterSymbolExtractor(TreeSitterNodeUtils nodeUtils) {
        this.nodeUtils = nodeUtils;
    }
/**
 * 获取Root Node相关逻辑。
 */
protected Node getRootNode(TreeSitterParseResult parseResult) {
        return nodeUtils.getRootNode(parseResult);
    }
/**
 * 执行 walk 相关逻辑。
 */
protected void walk(Node node, Consumer<Node> consumer) {
        if (node == null || consumer == null) {
            return;
        }
        consumer.accept(node);
        for (Node child : nodeUtils.getChildren(node)) {
            walk(child, consumer);
        }
    }
/**
 * 获取Node Type相关逻辑。
 */
protected String getNodeType(Node node) {
        return nodeUtils.getNodeType(node);
    }
/**
 * 获取Node Text相关逻辑。
 */
protected String getNodeText(Node node) {
        return nodeUtils.getNodeText(node);
    }
/**
 * 获取Start Line相关逻辑。
 */
protected int getStartLine(Node node) {
        return nodeUtils.getStartLine(node);
    }
/**
 * 获取End Line相关逻辑。
 */
protected int getEndLine(Node node) {
        return nodeUtils.getEndLine(node);
    }
/**
 * 获取Children相关逻辑。
 */
protected List<Node> getChildren(Node node) {
        return nodeUtils.getChildren(node);
    }
/**
 * 获取Named Children相关逻辑。
 */
protected List<Node> getNamedChildren(Node node) {
        return nodeUtils.getNamedChildren(node);
    }
/**
 * 获取Parent相关逻辑。
 */
protected Node getParent(Node node) {
        return nodeUtils.getParent(node);
    }
/**
 * 获取Child By Field Name相关逻辑。
 */
protected Node getChildByFieldName(Node node, String fieldName) {
        return nodeUtils.getChildByFieldName(node, fieldName);
    }
/**
 * 执行 isType 相关逻辑。
 */
protected boolean isType(Node node, String expectedType) {
        return nodeUtils.isType(node, expectedType);
    }
/**
 * 规范化Path相关逻辑。
 */
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
/**
 * 提取Identifier相关逻辑。
 */
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
/**
 * 提取Signature相关逻辑。
 */
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
/**
 * 执行 cleanText 相关逻辑。
 */
protected String cleanText(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.trim().replace("\r\n", "\n");
        return cleaned.isEmpty() ? null : cleaned;
    }
/**
 * 执行 truncateContent 相关逻辑。
 */
protected String truncateContent(String text) {
        if (text == null || text.length() <= CONTENT_LIMIT) {
            return text;
        }
        return text.substring(0, CONTENT_LIMIT);
    }
/**
 * 查找Nearest Ancestor Name相关逻辑。
 */
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
/**
 * 查找First Child相关逻辑。
 */
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
/**
 * 执行 joinTexts 相关逻辑。
 */
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
/**
 * 执行 collectChildTexts 相关逻辑。
 */
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
/**
 * 提取First Quoted Text相关逻辑。
 */
protected String extractFirstQuotedText(String text) {
        String normalized = cleanText(text);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = QUOTED_TEXT_PATTERN.matcher(normalized);
        return matcher.find() ? matcher.group(1) : null;
    }
/**
 * 提取Argument Value相关逻辑。
 */
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
/**
 * 转换为Lower Case相关逻辑。
 */
protected String toLowerCase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
/**
 * 执行 combineRoutePath 相关逻辑。
 */
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
/**
 * 执行 newOrderedSet 相关逻辑。
 */
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
/**
 * 执行 isSimpleIdentifier 相关逻辑。
 */
private boolean isSimpleIdentifier(String text) {
        return text.indexOf('\n') < 0 && IDENTIFIER_PATTERN.matcher(text).matches();
    }
}
