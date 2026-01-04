package com.aem.component.util;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.PathNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JCR 工具类
 * 
 * 提供常用的 JCR 操作辅助方法，简化节点访问和属性提取
 */
public class JCRUtil {

    /**
     * 安全地获取节点属性值（字符串类型）
     * 
     * @param node 目标节点
     * @param propertyName 属性名称
     * @param defaultValue 如果属性不存在时返回的默认值
     * @return 属性值，如果不存在则返回默认值
     */
    public static String getProperty(Node node, String propertyName, String defaultValue) {
        try {
            if (node.hasProperty(propertyName)) {
                Property property = node.getProperty(propertyName);
                return property.getString();
            }
        } catch (RepositoryException e) {
            System.err.println("获取属性 " + propertyName + " 时出错: " + e.getMessage());
        }
        return defaultValue;
    }

    /**
     * 安全地获取节点属性值（字符串类型，无默认值）
     * 
     * @param node 目标节点
     * @param propertyName 属性名称
     * @return 属性值，如果不存在则返回 null
     */
    public static String getProperty(Node node, String propertyName) {
        return getProperty(node, propertyName, null);
    }

    /**
     * 获取节点的所有属性（字符串值）
     * 
     * @param node 目标节点
     * @return 属性名到属性值的映射
     */
    public static Map<String, String> getAllProperties(Node node) {
        Map<String, String> properties = new HashMap<>();
        try {
            PropertyIterator propertyIterator = node.getProperties();
            while (propertyIterator.hasNext()) {
                Property property = propertyIterator.nextProperty();
                String name = property.getName();
                // 跳过 jcr 系统属性，除非需要
                if (!name.startsWith("jcr:")) {
                    try {
                        if (property.isMultiple()) {
                            // 多值属性，用逗号连接
                            Value[] values = property.getValues();
                            StringBuilder sb = new StringBuilder();
                            for (Value value : values) {
                                if (sb.length() > 0) {
                                    sb.append(",");
                                }
                                sb.append(value.getString());
                            }
                            properties.put(name, sb.toString());
                        } else {
                            properties.put(name, property.getString());
                        }
                    } catch (ValueFormatException e) {
                        // 忽略非字符串类型的属性（如二进制、日期等）
                        properties.put(name, "[非字符串类型]");
                    }
                }
            }
        } catch (RepositoryException e) {
            System.err.println("获取节点属性时出错: " + e.getMessage());
        }
        return properties;
    }

    /**
     * 安全地获取子节点
     * 
     * @param node 父节点
     * @param childName 子节点名称
     * @return 子节点，如果不存在则返回 null
     */
    public static Node getChildNode(Node node, String childName) {
        try {
            if (node.hasNode(childName)) {
                return node.getNode(childName);
            }
        } catch (RepositoryException e) {
            System.err.println("获取子节点 " + childName + " 时出错: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取节点的所有直接子节点
     * 
     * @param node 父节点
     * @return 子节点列表
     */
    public static List<Node> getChildNodes(Node node) {
        List<Node> children = new ArrayList<>();
        try {
            NodeIterator nodeIterator = node.getNodes();
            while (nodeIterator.hasNext()) {
                children.add(nodeIterator.nextNode());
            }
        } catch (RepositoryException e) {
            System.err.println("获取子节点列表时出错: " + e.getMessage());
        }
        return children;
    }

    /**
     * 递归获取节点的所有子节点（包括子节点的子节点）
     * 
     * @param node 起始节点
     * @param maxDepth 最大深度，-1 表示不限制
     * @return 所有子节点的列表
     */
    public static List<Node> getAllDescendants(Node node, int maxDepth) {
        List<Node> allNodes = new ArrayList<>();
        getAllDescendantsRecursive(node, allNodes, 0, maxDepth);
        return allNodes;
    }

    /**
     * 递归获取子节点的辅助方法
     */
    private static void getAllDescendantsRecursive(Node node, List<Node> allNodes, int currentDepth, int maxDepth) {
        if (maxDepth >= 0 && currentDepth >= maxDepth) {
            return;
        }

        try {
            NodeIterator nodeIterator = node.getNodes();
            while (nodeIterator.hasNext()) {
                Node child = nodeIterator.nextNode();
                allNodes.add(child);
                getAllDescendantsRecursive(child, allNodes, currentDepth + 1, maxDepth);
            }
        } catch (RepositoryException e) {
            System.err.println("递归获取子节点时出错: " + e.getMessage());
        }
    }

    /**
     * 安全地获取节点路径
     * 
     * @param node 目标节点
     * @return 节点路径
     */
    public static String getPath(Node node) {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            System.err.println("获取节点路径时出错: " + e.getMessage());
            return "";
        }
    }

    /**
     * 安全地获取节点名称
     * 
     * @param node 目标节点
     * @return 节点名称
     */
    public static String getName(Node node) {
        try {
            return node.getName();
        } catch (RepositoryException e) {
            System.err.println("获取节点名称时出错: " + e.getMessage());
            return "";
        }
    }

    /**
     * 检查节点是否存在指定的 mixin 类型
     * 
     * @param node 目标节点
     * @param mixinType mixin 类型名称（如 "cq:Component"）
     * @return 如果节点具有该 mixin 类型则返回 true
     */
    public static boolean hasMixin(Node node, String mixinType) {
        try {
            if (node.canAddMixin(mixinType) || node.isNodeType(mixinType)) {
                return true;
            }
        } catch (RepositoryException e) {
            // 忽略异常，返回 false
        }
        return false;
    }

    /**
     * 安全地检查节点是否存在指定路径
     * 
     * @param session JCR 会话
     * @param path 节点路径
     * @return 如果节点存在则返回 true
     */
    public static boolean nodeExists(Session session, String path) {
        try {
            return session.nodeExists(path);
        } catch (RepositoryException e) {
            return false;
        }
    }

    /**
     * 安全地获取节点
     * 
     * @param session JCR 会话
     * @param path 节点路径
     * @return 节点对象，如果不存在则返回 null
     */
    public static Node getNode(Session session, String path) {
        try {
            if (session.nodeExists(path)) {
                return session.getNode(path);
            }
        } catch (RepositoryException e) {
            System.err.println("获取节点 " + path + " 时出错: " + e.getMessage());
        }
        return null;
    }

    /**
     * 打印节点的详细信息（用于调试）
     * 
     * @param node 目标节点
     * @param indent 缩进字符串（用于递归打印时保持格式）
     */
    public static void printNodeInfo(Node node, String indent) {
        try {
            String nodeName = node.getName();
            String nodePath = node.getPath();
            String nodeType = node.getPrimaryNodeType().getName();
            
            System.out.println(indent + "节点: " + nodeName);
            System.out.println(indent + "  路径: " + nodePath);
            System.out.println(indent + "  类型: " + nodeType);
            
            // 打印属性
            Map<String, String> properties = getAllProperties(node);
            if (!properties.isEmpty()) {
                System.out.println(indent + "  属性:");
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    System.out.println(indent + "    " + entry.getKey() + " = " + entry.getValue());
                }
            }
            
            // 递归打印子节点
            List<Node> children = getChildNodes(node);
            if (!children.isEmpty()) {
                System.out.println(indent + "  子节点:");
                for (Node child : children) {
                    printNodeInfo(child, indent + "  ");
                }
            }
            
            System.out.println();
        } catch (RepositoryException e) {
            System.err.println("打印节点信息时出错: " + e.getMessage());
        }
    }
}

