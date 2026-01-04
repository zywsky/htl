package com.aem.component.info.examples;

import com.aem.component.util.JCRUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.NodeIterator;
import java.util.List;

/**
 * 基础 JCR 访问示例
 * 
 * 这个类展示了如何连接到 AEM 的 JCR，并访问组件节点。
 * 这是学习 AEM 组件信息提取的第一步。
 * 
 * 使用说明：
 * 1. 确保你可以访问 AEM 实例（本地或远程）
 * 2. 修改下面的连接参数（URL、用户名、密码）
 * 3. 运行此示例代码
 */
public class BasicJCRAccess {

    private static final Logger log = LoggerFactory.getLogger(BasicJCRAccess.class);

    // AEM 连接配置 - 根据你的实际环境修改这些值
    private static final String REPOSITORY_URL = "http://localhost:4502/crx/server";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    public static void main(String[] args) {
        BasicJCRAccess example = new BasicJCRAccess();
        example.run();
    }

    /**
     * 主执行方法
     */
    public void run() {
        Session session = null;
        try {
            // 步骤 1: 连接到 JCR 仓库
            log.info("正在连接到 AEM JCR 仓库...");
            session = connectToRepository();

            if (session == null) {
                log.error("无法连接到 JCR 仓库。请检查连接参数。");
                return;
            }

            log.info("成功连接到 JCR 仓库！");

            // 步骤 2: 访问根节点
            Node rootNode = session.getRootNode();
            log.info("根节点路径: " + rootNode.getPath());

            // 步骤 3: 访问 /apps 路径（用户定义的组件通常在这里）
            demonstrateAppsAccess(session);

            // 步骤 4: 访问 /libs 路径（系统组件，通常只读）
            demonstrateLibsAccess(session);

            // 步骤 5: 查找组件节点
            demonstrateComponentSearch(session);

        } catch (RepositoryException e) {
            log.error("JCR 操作出错: " + e.getMessage(), e);
        } finally {
            // 步骤 6: 关闭会话（重要！）
            if (session != null) {
                session.logout();
                log.info("JCR 会话已关闭");
            }
        }
    }

    /**
     * 连接到 JCR 仓库
     * 
     * 注意：这是一个简化的示例。在实际 AEM OSGi 环境中，
     * 你可能通过 SlingRepository 服务来获取会话。
     * 
     * @return JCR 会话对象
     */
    private Session connectToRepository() {
        try {
            // 在实际环境中，你可能需要通过不同的方式获取 Repository 对象
            // 这里假设你已经有一个 Repository 实例
            // 
            // 如果你在 AEM OSGi bundle 中，可以这样获取：
            // @Reference
            // private SlingRepository repository;
            // Session session = repository.loginAdministrative(null);
            
            log.warn("注意：这是一个简化的示例。实际环境中需要通过 AEM 的 OSGi 服务获取 Repository。");
            log.warn("如果你在 AEM bundle 中，使用 @Reference SlingRepository 注入。");
            
            // 返回 null 表示需要在实际环境中实现
            return null;
            
        } catch (Exception e) {
            log.error("连接失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 演示如何访问 /apps 路径
     * /apps 是用户定义组件的主要位置
     */
    private void demonstrateAppsAccess(Session session) throws RepositoryException {
        log.info("\n=== 访问 /apps 路径 ===");
        
        String appsPath = "/apps";
        if (JCRUtil.nodeExists(session, appsPath)) {
            Node appsNode = JCRUtil.getNode(session, appsPath);
            log.info("找到 /apps 节点");
            
            // 列出 /apps 下的直接子节点（通常是项目名称）
            List<Node> projectNodes = JCRUtil.getChildNodes(appsNode);
            log.info("/apps 下有 " + projectNodes.size() + " 个子节点:");
            
            for (Node projectNode : projectNodes) {
                String projectName = JCRUtil.getName(projectNode);
                String projectPath = JCRUtil.getPath(projectNode);
                log.info("  项目: " + projectName + " (路径: " + projectPath + ")");
            }
        } else {
            log.warn("/apps 节点不存在或无法访问");
        }
    }

    /**
     * 演示如何访问 /libs 路径
     * /libs 包含 AEM 的核心组件，通常是只读的
     */
    private void demonstrateLibsAccess(Session session) throws RepositoryException {
        log.info("\n=== 访问 /libs 路径 ===");
        
        String libsPath = "/libs";
        if (JCRUtil.nodeExists(session, libsPath)) {
            Node libsNode = JCRUtil.getNode(session, libsPath);
            log.info("找到 /libs 节点");
            
            // 查找核心组件
            String coreComponentsPath = "/libs/core/wcm/components";
            if (JCRUtil.nodeExists(session, coreComponentsPath)) {
                log.info("找到核心组件路径: " + coreComponentsPath);
                Node coreComponentsNode = JCRUtil.getNode(session, coreComponentsPath);
                List<Node> componentGroups = JCRUtil.getChildNodes(coreComponentsNode);
                log.info("核心组件组数量: " + componentGroups.size());
            }
        } else {
            log.warn("/libs 节点不存在或无法访问");
        }
    }

    /**
     * 演示如何搜索组件节点
     * 
     * 组件节点的典型特征：
     * 1. 具有 cq:Component 节点类型
     * 2. 位于 /apps 或 /libs 下的 components 路径
     * 3. 包含 sling:resourceType 属性
     */
    private void demonstrateComponentSearch(Session session) throws RepositoryException {
        log.info("\n=== 搜索组件节点 ===");
        
        // 示例：搜索特定路径下的组件
        String[] searchPaths = {
            "/apps/myproject/components",
            "/apps/geometrixx/components",
            "/libs/core/wcm/components"
        };
        
        for (String searchPath : searchPaths) {
            if (JCRUtil.nodeExists(session, searchPath)) {
                log.info("\n搜索路径: " + searchPath);
                Node componentsNode = JCRUtil.getNode(session, searchPath);
                findComponentsRecursive(componentsNode, 0, 3); // 最大深度 3
            }
        }
    }

    /**
     * 递归查找组件节点
     * 
     * @param node 起始节点
     * @param depth 当前深度
     * @param maxDepth 最大搜索深度
     */
    private void findComponentsRecursive(Node node, int depth, int maxDepth) 
            throws RepositoryException {
        if (depth > maxDepth) {
            return;
        }

        try {
            // 检查节点是否是组件节点
            // 组件节点通常具有 cq:Component mixin 类型
            if (node.isNodeType("cq:Component")) {
                String componentPath = JCRUtil.getPath(node);
                String componentTitle = JCRUtil.getProperty(node, "jcr:title", 
                    JCRUtil.getName(node));
                String resourceType = JCRUtil.getProperty(node, "sling:resourceType");
                
                log.info("  找到组件: " + componentTitle + 
                    " (路径: " + componentPath + 
                    ", resourceType: " + resourceType + ")");
            }
            
            // 继续搜索子节点
            NodeIterator nodeIterator = node.getNodes();
            while (nodeIterator.hasNext()) {
                findComponentsRecursive(nodeIterator.nextNode(), depth + 1, maxDepth);
            }
        } catch (RepositoryException e) {
            // 某些节点可能无法访问，继续处理其他节点
            log.debug("访问节点时出错: " + e.getMessage());
        }
    }

    /**
     * 打印节点树的辅助方法
     * 
     * @param node 起始节点
     * @param prefix 前缀字符串（用于格式化输出）
     */
    private void printNodeTree(Node node, String prefix) throws RepositoryException {
        String nodeName = node.getName();
        String nodePath = node.getPath();
        System.out.println(prefix + nodeName + " (" + nodePath + ")");
        
        NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            printNodeTree(nodeIterator.nextNode(), prefix + "  ");
        }
    }
}

