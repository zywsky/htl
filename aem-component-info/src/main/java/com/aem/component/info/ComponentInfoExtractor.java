package com.aem.component.info;

import com.aem.component.util.JCRUtil;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组件信息提取器 - 核心类
 * 
 * 这个类是提取 AEM 组件信息的核心工具。
 * 它整合了属性提取、对话框分析等功能，提供统一的接口来获取组件的完整信息。
 * 
 * 使用示例：
 * <pre>
 * ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
 * Map<String, Object> componentInfo = extractor.extractComponentInfo("/apps/myproject/components/mycomponent");
 * </pre>
 */
public class ComponentInfoExtractor {

    private final Session session;

    /**
     * 构造函数
     * 
     * @param session JCR 会话
     */
    public ComponentInfoExtractor(Session session) {
        this.session = session;
    }

    /**
     * 提取组件的完整信息
     * 
     * 这是主要方法，返回组件的所有相关信息，包括：
     * - 基本属性
     * - 对话框配置
     * - 设计对话框配置
     * - 编辑配置
     * - 客户端库依赖
     * - 模板文件信息
     * 
     * @param componentPath 组件路径（如 /apps/myproject/components/mycomponent）
     * @return 组件的完整信息 Map
     */
    public Map<String, Object> extractComponentInfo(String componentPath) {
        Map<String, Object> componentInfo = new HashMap<>();
        
        Node componentNode = JCRUtil.getNode(session, componentPath);
        if (componentNode == null) {
            componentInfo.put("error", "组件节点不存在: " + componentPath);
            return componentInfo;
        }
        
        try {
            // 验证是否是组件节点
            if (!componentNode.isNodeType("cq:Component")) {
                componentInfo.put("error", "指定的节点不是组件节点");
                return componentInfo;
            }
            
            // 1. 提取基本属性
            Map<String, String> basicProperties = ComponentPropertyExtractor.extractBasicProperties(componentNode);
            componentInfo.put("basicProperties", basicProperties);
            
            // 2. 提取所有属性
            Map<String, Object> allProperties = ComponentPropertyExtractor.extractAllProperties(componentNode);
            componentInfo.put("properties", allProperties);
            
            // 3. 分析对话框
            Map<String, Object> dialogInfo = DialogAnalyzer.analyzeDialog(componentNode);
            componentInfo.put("dialog", dialogInfo);
            
            // 4. 分析设计对话框
            Map<String, Object> designDialogInfo = DialogAnalyzer.analyzeDesignDialog(componentNode);
            componentInfo.put("designDialog", designDialogInfo);
            
            // 5. 提取模板文件信息
            Map<String, Object> templateInfo = extractTemplateInfo(componentNode);
            componentInfo.put("template", templateInfo);
            
            // 6. 检查组件依赖
            Map<String, Object> dependencies = extractDependencies(componentNode);
            componentInfo.put("dependencies", dependencies);
            
            // 7. 提取组件使用信息（如果有）
            Map<String, Object> usageInfo = extractUsageInfo(componentNode);
            componentInfo.put("usage", usageInfo);
            
            // 8. 元数据
            componentInfo.put("extractedAt", System.currentTimeMillis());
            componentInfo.put("componentPath", componentPath);
            componentInfo.put("componentName", JCRUtil.getName(componentNode));
            
        } catch (RepositoryException e) {
            componentInfo.put("error", "提取组件信息时出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        return componentInfo;
    }

    /**
     * 提取组件的模板文件信息
     * 
     * AEM 组件通常有 HTL（Sightly）模板文件，如 component.html。
     * 这个方法提取模板文件的相关信息。
     * 
     * @param componentNode 组件节点
     * @return 模板文件信息
     */
    private Map<String, Object> extractTemplateInfo(Node componentNode) {
        Map<String, Object> templateInfo = new HashMap<>();
        
        try {
            // 常见的模板文件名
            String[] templateNames = {
                "component.html",
                "template.html",
                "component.jsp",
                "template.jsp",
                "component.js",
                "component.htl"
            };
            
            List<String> foundTemplates = new ArrayList<>();
            for (String templateName : templateNames) {
                Node templateNode = JCRUtil.getChildNode(componentNode, templateName);
                if (templateNode != null) {
                    foundTemplates.add(templateName);
                    
                    // 提取模板文件的属性
                    Map<String, String> templateProperties = JCRUtil.getAllProperties(templateNode);
                    templateInfo.put(templateName + "_properties", templateProperties);
                }
            }
            
            templateInfo.put("templateFiles", foundTemplates);
            
            // 检查是否有模板文件夹
            Node templateFolderNode = JCRUtil.getChildNode(componentNode, "template");
            if (templateFolderNode != null) {
                templateInfo.put("hasTemplateFolder", true);
                List<Node> templateNodes = JCRUtil.getChildNodes(templateFolderNode);
                templateInfo.put("templateFolderFiles", templateNodes.size());
            }
            
        } catch (Exception e) {
            System.err.println("提取模板信息时出错: " + e.getMessage());
            templateInfo.put("error", e.getMessage());
        }
        
        return templateInfo;
    }

    /**
     * 提取组件的依赖信息
     * 
     * 包括：
     * - 继承的父组件（sling:resourceSuperType）
     * - 引用的客户端库
     * - 其他依赖关系
     * 
     * @param componentNode 组件节点
     * @return 依赖信息
     */
    private Map<String, Object> extractDependencies(Node componentNode) {
        Map<String, Object> dependencies = new HashMap<>();
        
        try {
            // 父组件
            String superType = JCRUtil.getProperty(componentNode, "sling:resourceSuperType");
            if (superType != null && !superType.isEmpty()) {
                dependencies.put("resourceSuperType", superType);
                
                // 如果父组件存在，可以递归提取父组件信息
                if (JCRUtil.nodeExists(session, superType)) {
                    dependencies.put("resourceSuperTypeExists", true);
                } else {
                    dependencies.put("resourceSuperTypeExists", false);
                }
            }
            
            // 客户端库（已在 ComponentPropertyExtractor 中提取，这里可以扩展）
            Map<String, Object> clientLibs = ComponentPropertyExtractor.extractClientLibraries(componentNode);
            dependencies.put("clientLibraries", clientLibs);
            
            // 可以添加更多依赖分析，如：
            // - 使用的其他组件
            // - API 依赖
            // - 服务依赖等
            
        } catch (Exception e) {
            System.err.println("提取依赖信息时出错: " + e.getMessage());
            dependencies.put("error", e.getMessage());
        }
        
        return dependencies;
    }

    /**
     * 提取组件的使用信息
     * 
     * 分析组件在 AEM 中的使用情况（需要额外的查询，这里提供框架）
     * 
     * @param componentNode 组件节点
     * @return 使用信息
     */
    private Map<String, Object> extractUsageInfo(Node componentNode) {
        Map<String, Object> usageInfo = new HashMap<>();
        
        try {
            String resourceType = JCRUtil.getProperty(componentNode, "sling:resourceType");
            
            // 这里可以添加 JCR 查询来查找使用该组件的页面
            // 例如使用 XPath 或 SQL2 查询
            
            usageInfo.put("resourceType", resourceType);
            usageInfo.put("note", "使用信息提取需要额外的 JCR 查询，此处提供框架");
            
            // 示例查询框架（需要根据实际需求实现）：
            // String query = "SELECT * FROM [cq:PageContent] WHERE [sling:resourceType] = '" + resourceType + "'";
            // QueryManager queryManager = session.getWorkspace().getQueryManager();
            // Query query = queryManager.createQuery(query, Query.JCR_SQL2);
            // QueryResult result = query.execute();
            // ...
            
        } catch (Exception e) {
            System.err.println("提取使用信息时出错: " + e.getMessage());
            usageInfo.put("error", e.getMessage());
        }
        
        return usageInfo;
    }

    /**
     * 批量提取组件信息
     * 
     * 从指定的路径下提取所有组件的信息。
     * 
     * @param basePath 基础路径（如 /apps/myproject/components）
     * @return 组件信息列表
     */
    public List<Map<String, Object>> extractComponentsFromPath(String basePath) {
        List<Map<String, Object>> componentsInfo = new ArrayList<>();
        
        Node baseNode = JCRUtil.getNode(session, basePath);
        if (baseNode == null) {
            System.err.println("基础路径不存在: " + basePath);
            return componentsInfo;
        }
        
        try {
            // 递归查找所有组件节点
            List<Node> componentNodes = findComponentNodes(baseNode);
            
            for (Node componentNode : componentNodes) {
                String componentPath = JCRUtil.getPath(componentNode);
                Map<String, Object> componentInfo = extractComponentInfo(componentPath);
                componentsInfo.add(componentInfo);
            }
            
        } catch (RepositoryException e) {
            System.err.println("批量提取组件信息时出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        return componentsInfo;
    }

    /**
     * 递归查找组件节点
     * 
     * @param node 起始节点
     * @return 组件节点列表
     */
    private List<Node> findComponentNodes(Node node) throws RepositoryException {
        List<Node> componentNodes = new ArrayList<>();
        
        // 检查当前节点是否是组件
        if (node.isNodeType("cq:Component")) {
            componentNodes.add(node);
            // 找到组件节点后，不需要继续向下搜索（组件通常是叶子节点）
            return componentNodes;
        }
        
        // 继续搜索子节点
        List<Node> childNodes = JCRUtil.getChildNodes(node);
        for (Node childNode : childNodes) {
            componentNodes.addAll(findComponentNodes(childNode));
        }
        
        return componentNodes;
    }

    /**
     * 提取组件的简化信息（仅包含关键属性）
     * 
     * 适用于需要快速获取组件基本信息而不需要完整分析的场景。
     * 
     * @param componentPath 组件路径
     * @return 简化信息
     */
    public Map<String, Object> extractComponentInfoSimple(String componentPath) {
        Map<String, Object> componentInfo = new HashMap<>();
        
        Node componentNode = JCRUtil.getNode(session, componentPath);
        if (componentNode == null) {
            componentInfo.put("error", "组件节点不存在: " + componentPath);
            return componentInfo;
        }
        
        try {
            // 仅提取基本属性
            Map<String, String> basicProperties = ComponentPropertyExtractor.extractBasicProperties(componentNode);
            componentInfo.putAll(basicProperties);
            
            // 是否有对话框
            boolean hasDialog = ComponentPropertyExtractor.hasDialog(componentNode);
            componentInfo.put("hasDialog", hasDialog);
            
        } catch (RepositoryException e) {
            componentInfo.put("error", "提取组件信息时出错: " + e.getMessage());
        }
        
        return componentInfo;
    }
}

