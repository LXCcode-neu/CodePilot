package com.codepliot.service.index;

import ch.usi.si.seart.treesitter.Node;
import ch.usi.si.seart.treesitter.Point;
import ch.usi.si.seart.treesitter.Tree;
import com.codepliot.model.TreeSitterParseResult;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
/**
 * TreeSitterNodeUtils 服务类，负责封装业务流程和领域能力。
 */
@Component
public class TreeSitterNodeUtils {
/**
 * 获取Root Node相关逻辑。
 */
public Node getRootNode(TreeSitterParseResult parseResult) {
        if (parseResult == null || !(parseResult.astObject() instanceof Tree tree)) {
            return null;
        }
        return tree.getRootNode();
    }
/**
 * 获取Node Type相关逻辑。
 */
public String getNodeType(Node node) {
        return node == null ? "" : node.getType();
    }
/**
 * 获取Node Text相关逻辑。
 */
public String getNodeText(Node node) {
        return node == null ? "" : node.getContent();
    }
/**
 * 获取Start Line相关逻辑。
 */
public int getStartLine(Node node) {
        if (node == null) {
            return 0;
        }
        return toOneBasedLine(node.getStartPoint());
    }
/**
 * 获取End Line相关逻辑。
 */
public int getEndLine(Node node) {
        if (node == null) {
            return 0;
        }
        return toOneBasedLine(node.getEndPoint());
    }
/**
 * 获取Children相关逻辑。
 */
public List<Node> getChildren(Node node) {
        if (node == null) {
            return Collections.emptyList();
        }
        return node.getChildren();
    }
/**
 * 获取Child By Field Name相关逻辑。
 */
public Node getChildByFieldName(Node node, String fieldName) {
        if (node == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        return node.getChildByFieldName(fieldName);
    }
/**
 * 获取Parent相关逻辑。
 */
public Node getParent(Node node) {
        if (node == null) {
            return null;
        }
        return node.getParent();
    }
/**
 * 获取Named Children相关逻辑。
 */
public List<Node> getNamedChildren(Node node) {
        if (node == null) {
            return Collections.emptyList();
        }
        return node.getNamedChildren();
    }
/**
 * 执行 isType 相关逻辑。
 */
public boolean isType(Node node, String expectedType) {
        if (node == null || expectedType == null) {
            return false;
        }
        return expectedType.equals(node.getType());
    }
/**
 * 转换为One Based Line相关逻辑。
 */
private int toOneBasedLine(Point point) {
        if (point == null) {
            return 0;
        }
        return point.getRow() + 1;
    }
}
