package com.aem.component.info;

import com.aem.component.util.JCRUtil;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组件查询工具类
 * 
 * 提供使用 JCR 查询来查找组件的高级功能：
 * - 根据资源类型查找组件
 * - 查找使用特定组件的页面
 * - 查找组件依赖关系
 * - 组件使用统计
 */
public class ComponentQueryUtil {

    /**
     * 根据资源类型查找组件节点
     * 
     * @param session JCR 会话
     * @param resourceType 资源类型（如 "myproject/components/mycomponent"）
     * @return 组件节点，如果不存在则返回 null
     */
    public static Node findComponentByResourceType(Session session, String resourceType) 
            throws RepositoryException {
        
        // 构建查询路径
        String componentPath = "/apps/" + resourceType;
        if (JCRUtil.nodeExists(session, componentPath)) {
            return JCRUtil.getNode(session, componentPath);
        }
        
        // 尝试 /libs 路径
        componentPath = "/libs/" + resourceType;
        if (JCRUtil.nodeExists(session, componentPath)) {
            return JCRUtil.getNode(session, componentPath);
        }
        
        return null;
    }

    /**
     * 查找使用指定组件的所有页面内容节点
     * 
     * 这个查询会查找所有使用该组件的页面内容节点。
     * 
     * @param session JCR 会话
     * @param resourceType 组件资源类型
     * @return 使用该组件的页面内容节点列表
     */
    public static List<Node> findPagesUsingComponent(Session session, String resourceType) 
            throws RepositoryException {
        
        List<Node> result = new ArrayList<>();
        
        // 构建 SQL2 查询
        String queryString = "SELECT * FROM [cq:PageContent] " +
                           "WHERE [sling:resourceType] = '" + resourceType + "'";
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
        QueryResult queryResult = query.execute();
        
        NodeIterator nodeIterator = queryResult.getNodes();
        while (nodeIterator.hasNext()) {
            result.add(nodeIterator.nextNode());
        }
        
        return result;
    }

    /**
     * 获取组件的使用统计信息
     * 
     * @param session JCR 会话
     * @param resourceType 组件资源类型
     * @return 使用统计信息
     */
    public static Map<String, Object> getComponentUsageStats(Session session, String resourceType) 
            throws RepositoryException {
        
        Map<String, Object> stats = new HashMap<>();
        
        List<Node> pages = findPagesUsingComponent(session, resourceType);
        stats.put("usageCount", pages.size());
        stats.put("resourceType", resourceType);
        
        // 按页面路径分组统计
        Map<String, Integer> pathCount = new HashMap<>();
        for (Node pageContent : pages) {
            try {
                String pagePath = pageContent.getPath();
                // 提取页面路径（去掉 /jcr:content）
                if (pagePath.contains("/jcr:content")) {
                    pagePath = pagePath.substring(0, pagePath.indexOf("/jcr:content"));
                }
                pathCount.put(pagePath, pathCount.getOrDefault(pagePath, 0) + 1);
            } catch (RepositoryException e) {
                // 忽略无法访问的节点
            }
        }
        
        stats.put("pages", pathCount);
        
        return stats;
    }

    /**
     * 查找所有组件节点（在指定路径下）
     * 
     * @param session JCR 会话
     * @param basePath 基础路径（如 "/apps/myproject"）
     * @return 组件节点列表
     */
    public static List<Node> findAllComponents(Session session, String basePath) 
            throws RepositoryException {
        
        List<Node> result = new ArrayList<>();
        
        // 构建 XPath 查询（更灵活）
        String queryString = "/jcr:root" + basePath + "//element(*, cq:Component)";
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.XPATH);
        QueryResult queryResult = query.execute();
        
        NodeIterator nodeIterator = queryResult.getNodes();
        while (nodeIterator.hasNext()) {
            result.add(nodeIterator.nextNode());
        }
        
        return result;
    }

    /**
     * 查找组件的所有子组件（继承关系）
     * 
     * 查找所有继承自指定组件的组件。
     * 
     * @param session JCR 会话
     * @param parentResourceType 父组件资源类型
     * @return 子组件节点列表
     */
    public static List<Node> findChildComponents(Session session, String parentResourceType) 
            throws RepositoryException {
        
        List<Node> result = new ArrayList<>();
        
        // 构建查询：查找所有 sling:resourceSuperType 等于指定值的组件
        String queryString = "SELECT * FROM [cq:Component] " +
                           "WHERE [sling:resourceSuperType] = '" + parentResourceType + "'";
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
        QueryResult queryResult = query.execute();
        
        NodeIterator nodeIterator = queryResult.getNodes();
        while (nodeIterator.hasNext()) {
            result.add(nodeIterator.nextNode());
        }
        
        return result;
    }

    /**
     * 查找组件依赖的其他组件
     * 
     * 通过分析组件的模板和配置，找出组件依赖的其他组件。
     * 
     * @param session JCR 会话
     * @param componentPath 组件路径
     * @return 依赖的组件资源类型列表
     */
    public static List<String> findComponentDependencies(Session session, String componentPath) 
            throws RepositoryException {
        
        List<String> dependencies = new ArrayList<>();
        
        Node componentNode = JCRUtil.getNode(session, componentPath);
        if (componentNode == null) {
            return dependencies;
        }
        
        // 检查 resourceSuperType
        String superType = JCRUtil.getProperty(componentNode, "sling:resourceSuperType");
        if (superType != null && !superType.isEmpty()) {
            dependencies.add(superType);
        }
        
        // 可以进一步扩展：
        // - 分析模板文件中的组件引用
        // - 检查对话框中的组件选择器
        // - 分析客户端库依赖
        
        return dependencies;
    }

    /**
     * 搜索组件（根据名称、标题等）
     * 
     * @param session JCR 会话
     * @param searchTerm 搜索关键词
     * @param basePath 搜索基础路径
     * @return 匹配的组件节点列表
     */
    public static List<Node> searchComponents(Session session, String searchTerm, String basePath) 
            throws RepositoryException {
        
        List<Node> result = new ArrayList<>();
        
        // 使用 LIKE 查询进行模糊搜索
        String queryString = "SELECT * FROM [cq:Component] " +
                           "WHERE ISDESCENDANTNODE('" + basePath + "') " +
                           "AND ([jcr:title] LIKE '%" + searchTerm + "%' " +
                           "OR [jcr:description] LIKE '%" + searchTerm + "%' " +
                           "OR NAME() LIKE '%" + searchTerm + "%')";
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
        QueryResult queryResult = query.execute();
        
        NodeIterator nodeIterator = queryResult.getNodes();
        while (nodeIterator.hasNext()) {
            result.add(nodeIterator.nextNode());
        }
        
        return result;
    }

    /**
     * 获取组件统计信息（在指定路径下）
     * 
     * @param session JCR 会话
     * @param basePath 基础路径
     * @return 统计信息
     */
    public static Map<String, Object> getComponentStatistics(Session session, String basePath) 
            throws RepositoryException {
        
        Map<String, Object> stats = new HashMap<>();
        
        List<Node> components = findAllComponents(session, basePath);
        stats.put("totalComponents", components.size());
        
        // 按组件分组统计
        Map<String, Integer> groupCount = new HashMap<>();
        int withDialog = 0;
        int withDesignDialog = 0;
        
        for (Node component : components) {
            // 组件分组统计
            String group = JCRUtil.getProperty(component, "componentGroup", "未分组");
            groupCount.put(group, groupCount.getOrDefault(group, 0) + 1);
            
            // 对话框统计
            if (ComponentPropertyExtractor.hasDialog(component)) {
                withDialog++;
            }
            if (ComponentPropertyExtractor.hasDesignDialog(component)) {
                withDesignDialog++;
            }
        }
        
        stats.put("componentsByGroup", groupCount);
        stats.put("componentsWithDialog", withDialog);
        stats.put("componentsWithDesignDialog", withDesignDialog);
        
        return stats;
    }
}

