package com.aem.component.util;

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
 * JCR 查询工具类
 * 
 * 提供常用的 JCR 查询功能，用于查找组件使用情况、搜索组件等。
 */
public class JCRQueryUtil {

    /**
     * 查询使用指定资源类型的页面内容节点
     * 
     * 这个查询用于查找哪些页面使用了某个组件。
     * 
     * @param session JCR 会话
     * @param resourceType 资源类型（如 "myproject/components/mycomponent"）
     * @return 使用该组件的节点路径列表
     * @throws RepositoryException JCR 操作错误
     */
    public static List<String> findPagesUsingComponent(Session session, String resourceType) 
            throws RepositoryException {
        
        List<String> pagePaths = new ArrayList<>();
        
        // 构建 SQL2 查询
        // 注意：resourceType 需要转义，防止 SQL 注入
        String escapedResourceType = resourceType.replace("'", "''");
        String queryString = 
            "SELECT * FROM [cq:PageContent] " +
            "WHERE [sling:resourceType] = '" + escapedResourceType + "'";
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
        QueryResult result = query.execute();
        
        NodeIterator nodeIterator = result.getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            // 获取页面路径（PageContent 的父节点是 Page）
            try {
                Node pageNode = node.getParent();
                if (pageNode != null) {
                    pagePaths.add(pageNode.getPath());
                }
            } catch (RepositoryException e) {
                // 如果无法获取父节点，使用当前节点路径
                pagePaths.add(node.getPath());
            }
        }
        
        return pagePaths;
    }

    /**
     * 统计组件使用次数
     * 
     * @param session JCR 会话
     * @param resourceType 资源类型
     * @return 使用统计信息
     * @throws RepositoryException JCR 操作错误
     */
    public static Map<String, Object> getComponentUsageStats(Session session, String resourceType) 
            throws RepositoryException {
        
        Map<String, Object> stats = new HashMap<>();
        
        List<String> pagePaths = findPagesUsingComponent(session, resourceType);
        
        stats.put("resourceType", resourceType);
        stats.put("usageCount", pagePaths.size());
        stats.put("pagePaths", pagePaths);
        
        return stats;
    }

    /**
     * 查找所有组件节点
     * 
     * 在指定路径下查找所有 cq:Component 类型的节点。
     * 
     * @param session JCR 会话
     * @param basePath 基础路径（如 "/apps/myproject/components"）
     * @return 组件节点路径列表
     * @throws RepositoryException JCR 操作错误
     */
    public static List<String> findAllComponents(Session session, String basePath) 
            throws RepositoryException {
        
        List<String> componentPaths = new ArrayList<>();
        
        // 构建 SQL2 查询
        String escapedBasePath = basePath.replace("'", "''");
        String queryString = 
            "SELECT * FROM [cq:Component] " +
            "WHERE ISDESCENDANTNODE('" + escapedBasePath + "')";
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
        QueryResult result = query.execute();
        
        NodeIterator nodeIterator = result.getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            componentPaths.add(node.getPath());
        }
        
        return componentPaths;
    }

    /**
     * 查找具有特定属性的组件
     * 
     * @param session JCR 会话
     * @param propertyName 属性名称
     * @param propertyValue 属性值
     * @param basePath 搜索的基础路径（可选，null 表示搜索全部）
     * @return 匹配的组件路径列表
     * @throws RepositoryException JCR 操作错误
     */
    public static List<String> findComponentsByProperty(Session session, 
                                                        String propertyName,
                                                        String propertyValue,
                                                        String basePath) 
            throws RepositoryException {
        
        List<String> componentPaths = new ArrayList<>();
        
        // 构建查询
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM [cq:Component] ");
        queryBuilder.append("WHERE [").append(propertyName).append("] = '");
        queryBuilder.append(propertyValue.replace("'", "''")).append("'");
        
        if (basePath != null && !basePath.isEmpty()) {
            String escapedBasePath = basePath.replace("'", "''");
            queryBuilder.append(" AND ISDESCENDANTNODE('").append(escapedBasePath).append("')");
        }
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryBuilder.toString(), Query.JCR_SQL2);
        QueryResult result = query.execute();
        
        NodeIterator nodeIterator = result.getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            componentPaths.add(node.getPath());
        }
        
        return componentPaths;
    }

    /**
     * 查找继承自指定父类型的组件
     * 
     * @param session JCR 会话
     * @param superType 父资源类型
     * @param basePath 搜索的基础路径（可选）
     * @return 匹配的组件路径列表
     * @throws RepositoryException JCR 操作错误
     */
    public static List<String> findComponentsBySuperType(Session session,
                                                         String superType,
                                                         String basePath) 
            throws RepositoryException {
        
        return findComponentsByProperty(session, "sling:resourceSuperType", superType, basePath);
    }

    /**
     * 查找属于特定组件组的组件
     * 
     * @param session JCR 会话
     * @param componentGroup 组件组名称
     * @param basePath 搜索的基础路径（可选）
     * @return 匹配的组件路径列表
     * @throws RepositoryException JCR 操作错误
     */
    public static List<String> findComponentsByGroup(Session session,
                                                     String componentGroup,
                                                     String basePath) 
            throws RepositoryException {
        
        return findComponentsByProperty(session, "componentGroup", componentGroup, basePath);
    }

    /**
     * 执行自定义 JCR 查询
     * 
     * @param session JCR 会话
     * @param queryString 查询字符串（SQL2 格式）
     * @return 查询结果节点路径列表
     * @throws RepositoryException JCR 操作错误
     */
    public static List<String> executeQuery(Session session, String queryString) 
            throws RepositoryException {
        
        List<String> nodePaths = new ArrayList<>();
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
        QueryResult result = query.execute();
        
        NodeIterator nodeIterator = result.getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            nodePaths.add(node.getPath());
        }
        
        return nodePaths;
    }
}

