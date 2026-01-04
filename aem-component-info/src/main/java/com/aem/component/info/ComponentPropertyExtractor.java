package com.aem.component.info;

import com.aem.component.util.JCRUtil;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

/**
 * 组件属性提取器
 * 
 * 从 AEM 组件节点中提取所有重要的属性信息。
 * 这些属性对于理解组件的功能和配置至关重要。
 */
public class ComponentPropertyExtractor {

    /**
     * 提取组件的基本属性
     * 
     * 组件的基本属性包括：
     * - sling:resourceType: 组件的资源类型（唯一标识符）
     * - jcr:title: 组件显示名称
     * - jcr:description: 组件描述
     * - componentGroup: 组件所属的组（在组件浏览器中的分类）
     * - sling:resourceSuperType: 组件继承的父类型
     * 
     * @param componentNode 组件节点（应该是 cq:Component 类型）
     * @return 包含组件基本属性的 Map
     */
    public static Map<String, String> extractBasicProperties(Node componentNode) {
        Map<String, String> properties = new HashMap<>();
        
        try {
            // 核心标识属性
            String resourceType = JCRUtil.getProperty(componentNode, "sling:resourceType");
            properties.put("sling:resourceType", resourceType);
            
            // 显示信息
            String title = JCRUtil.getProperty(componentNode, "jcr:title");
            properties.put("jcr:title", title);
            
            String description = JCRUtil.getProperty(componentNode, "jcr:description");
            properties.put("jcr:description", description);
            
            // 组件分组
            String componentGroup = JCRUtil.getProperty(componentNode, "componentGroup");
            properties.put("componentGroup", componentGroup);
            
            // 继承关系
            String resourceSuperType = JCRUtil.getProperty(componentNode, "sling:resourceSuperType");
            properties.put("sling:resourceSuperType", resourceSuperType);
            
            // 组件图标（如果有）
            String icon = JCRUtil.getProperty(componentNode, "cq:icon");
            properties.put("cq:icon", icon);
            
            // 组件标签（用于组件浏览器）
            String[] tags = getStringArrayProperty(componentNode, "cq:tags");
            if (tags != null && tags.length > 0) {
                properties.put("cq:tags", String.join(",", tags));
            }
            
            // 路径信息
            properties.put("componentPath", JCRUtil.getPath(componentNode));
            properties.put("componentName", JCRUtil.getName(componentNode));
            
            // 节点类型
            try {
                properties.put("primaryNodeType", componentNode.getPrimaryNodeType().getName());
                properties.put("isCqComponent", String.valueOf(componentNode.isNodeType("cq:Component")));
            } catch (RepositoryException e) {
                properties.put("primaryNodeType", "unknown");
            }
            
        } catch (Exception e) {
            System.err.println("提取组件基本属性时出错: " + e.getMessage());
        }
        
        return properties;
    }

    /**
     * 提取组件的编辑配置信息
     * 
     * 编辑配置（_cq_editConfig）定义了组件在 AEM 编辑器中如何显示和行为：
     * - 内联编辑功能
     * - 拖放配置
     * - 监听器配置
     * 
     * @param componentNode 组件节点
     * @return 编辑配置信息的 Map
     */
    public static Map<String, Object> extractEditConfig(Node componentNode) {
        Map<String, Object> editConfig = new HashMap<>();
        
        Node editConfigNode = JCRUtil.getChildNode(componentNode, "_cq_editConfig");
        if (editConfigNode == null) {
            editConfig.put("exists", false);
            return editConfig;
        }
        
        editConfig.put("exists", true);
        
        try {
            // 提取编辑配置的属性
            Map<String, String> properties = JCRUtil.getAllProperties(editConfigNode);
            editConfig.put("properties", properties);
            
            // 检查是否有内联编辑配置
            Node inplaceEditingNode = JCRUtil.getChildNode(editConfigNode, "cq:inplaceEditing");
            if (inplaceEditingNode != null) {
                Map<String, String> inplaceEditing = new HashMap<>();
                inplaceEditing.put("editorType", JCRUtil.getProperty(inplaceEditingNode, "editorType"));
                inplaceEditing.put("active", JCRUtil.getProperty(inplaceEditingNode, "active"));
                editConfig.put("inplaceEditing", inplaceEditing);
            }
            
            // 检查是否有监听器
            Node listenersNode = JCRUtil.getChildNode(editConfigNode, "cq:listeners");
            if (listenersNode != null) {
                Map<String, String> listeners = JCRUtil.getAllProperties(listenersNode);
                editConfig.put("listeners", listeners);
            }
            
            // 检查是否有表单参数
            Node formParametersNode = JCRUtil.getChildNode(editConfigNode, "cq:formParameters");
            if (formParametersNode != null) {
                Map<String, String> formParameters = JCRUtil.getAllProperties(formParametersNode);
                editConfig.put("formParameters", formParameters);
            }
            
        } catch (Exception e) {
            System.err.println("提取编辑配置时出错: " + e.getMessage());
        }
        
        return editConfig;
    }

    /**
     * 提取组件的客户端库依赖
     * 
     * 客户端库（clientlibs）定义了组件所需的 CSS 和 JavaScript 资源。
     * 这对于 React 重构非常重要，因为需要知道组件的样式和行为依赖。
     * 
     * @param componentNode 组件节点
     * @return 客户端库信息
     */
    public static Map<String, Object> extractClientLibraries(Node componentNode) {
        Map<String, Object> clientLibs = new HashMap<>();
        
        try {
            // 检查是否有客户端库节点
            Node clientlibsNode = JCRUtil.getChildNode(componentNode, "cq:clientlibs");
            if (clientlibsNode != null) {
                clientLibs.put("exists", true);
                Map<String, String> properties = JCRUtil.getAllProperties(clientlibsNode);
                clientLibs.put("properties", properties);
            } else {
                clientLibs.put("exists", false);
            }
            
            // 提取 cq:htmlTag 属性（可能包含客户端库引用）
            String htmlTag = JCRUtil.getProperty(componentNode, "cq:htmlTag");
            if (htmlTag != null && !htmlTag.isEmpty()) {
                clientLibs.put("htmlTag", htmlTag);
            }
            
            // 检查组件的 .content.xml 或模板文件，看是否有客户端库引用
            // 这里可以进一步扩展，解析 HTL 模板文件
            
        } catch (Exception e) {
            System.err.println("提取客户端库信息时出错: " + e.getMessage());
        }
        
        return clientLibs;
    }

    /**
     * 提取组件的所有属性（包括系统属性和自定义属性）
     * 
     * @param componentNode 组件节点
     * @return 所有属性的 Map
     */
    public static Map<String, Object> extractAllProperties(Node componentNode) {
        Map<String, Object> allProperties = new HashMap<>();
        
        // 基本属性
        Map<String, String> basicProperties = extractBasicProperties(componentNode);
        allProperties.put("basic", basicProperties);
        
        // 编辑配置
        Map<String, Object> editConfig = extractEditConfig(componentNode);
        allProperties.put("editConfig", editConfig);
        
        // 客户端库
        Map<String, Object> clientLibs = extractClientLibraries(componentNode);
        allProperties.put("clientLibraries", clientLibs);
        
        // 所有原始属性（包括系统属性）
        try {
            Map<String, String> rawProperties = JCRUtil.getAllProperties(componentNode);
            allProperties.put("rawProperties", rawProperties);
        } catch (Exception e) {
            System.err.println("提取原始属性时出错: " + e.getMessage());
        }
        
        return allProperties;
    }

    /**
     * 获取字符串数组类型的属性
     * 
     * @param node 节点
     * @param propertyName 属性名称
     * @return 字符串数组，如果属性不存在或出错则返回 null
     */
    private static String[] getStringArrayProperty(Node node, String propertyName) {
        try {
            if (node.hasProperty(propertyName)) {
                javax.jcr.Property property = node.getProperty(propertyName);
                if (property.isMultiple()) {
                    javax.jcr.Value[] values = property.getValues();
                    String[] result = new String[values.length];
                    for (int i = 0; i < values.length; i++) {
                        result[i] = values[i].getString();
                    }
                    return result;
                } else {
                    return new String[]{property.getString()};
                }
            }
        } catch (RepositoryException e) {
            System.err.println("获取数组属性 " + propertyName + " 时出错: " + e.getMessage());
        }
        return null;
    }

    /**
     * 检查组件是否有对话框
     * 
     * @param componentNode 组件节点
     * @return 如果有对话框则返回 true
     */
    public static boolean hasDialog(Node componentNode) {
        Node touchDialog = JCRUtil.getChildNode(componentNode, "_cq_dialog");
        Node classicDialog = JCRUtil.getChildNode(componentNode, "dialog");
        return touchDialog != null || classicDialog != null;
    }

    /**
     * 检查组件是否有设计对话框
     * 
     * @param componentNode 组件节点
     * @return 如果有设计对话框则返回 true
     */
    public static boolean hasDesignDialog(Node componentNode) {
        Node designDialog = JCRUtil.getChildNode(componentNode, "_cq_design_dialog");
        return designDialog != null;
    }
}

