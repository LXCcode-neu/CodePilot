package com.codepliot.index.tree;

import ch.usi.si.seart.treesitter.Node;
import ch.usi.si.seart.treesitter.Point;
import ch.usi.si.seart.treesitter.Tree;
import com.codepliot.index.dto.TreeSitterParseResult;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Tree-sitter 节点工具类。
 * 封装节点类型、文本、行号、子节点及类型判断等常用操作。
 */
@Component
public class TreeSitterNodeUtils {

    /**
     * 从统一解析结果中取根节点。
     */
    public Node getRootNode(TreeSitterParseResult parseResult) {
        if (parseResult == null || !(parseResult.astObject() instanceof Tree tree)) {
            return null;
        }
        return tree.getRootNode();
    }

    /**
     * 获取节点类型。
     */
    public String getNodeType(Node node) {
        return node == null ? "" : node.getType();
    }

    /**
     * 获取节点原始文本。
     */
    public String getNodeText(Node node) {
        return node == null ? "" : node.getContent();
    }

    /**
     * 获取节点起始行号，转为 1-based。
     */
    public int getStartLine(Node node) {
        if (node == null) {
            return 0;
        }
        return toOneBasedLine(node.getStartPoint());
    }

    /**
     * 获取节点结束行号，转为 1-based。
     */
    public int getEndLine(Node node) {
        if (node == null) {
            return 0;
        }
        return toOneBasedLine(node.getEndPoint());
    }

    /**
     * 获取直接子节点列表。
     */
    public List<Node> getChildren(Node node) {
        if (node == null) {
            return Collections.emptyList();
        }
        return node.getChildren();
    }

    /**
     * 判断节点类型是否匹配。
     */
    public boolean isType(Node node, String expectedType) {
        if (node == null || expectedType == null) {
            return false;
        }
        return expectedType.equals(node.getType());
    }

    private int toOneBasedLine(Point point) {
        if (point == null) {
            return 0;
        }
        return point.getRow() + 1;
    }
}
