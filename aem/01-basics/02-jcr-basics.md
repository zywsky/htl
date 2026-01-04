# AEM 基础：第二部分 - JCR (Java Content Repository) 基础

## 📖 什么是 JCR?

JCR (Java Content Repository) 是 Java 内容仓库 API 规范，它为内容存储提供了标准化的接口。AEM 使用 Apache Jackrabbit 作为 JCR 实现，所有内容都存储在 JCR 仓库中。

## 🏗️ JCR 核心概念

### 1. 节点 (Node) 和属性 (Property)

JCR 使用节点树结构存储数据，类似于文件系统：

```
节点 (Node)
├── 属性 (Property): name = "value"
├── 属性 (Property): age = 30
└── 子节点 (Child Node)
    └── 属性 (Property): type = "child"
```

**节点 (Node)**:
- 类似于文件系统中的目录
- 可以包含属性和子节点
- 必须有唯一名称

**属性 (Property)**:
- 类似于文件系统中的文件
- 存储实际的数据值
- 有名称和值（值可以是多种类型）

### 2. 路径 (Path)

每个节点都有唯一路径，从根节点 `/` 开始：

```
/content/myproject/en/home
│       │         │  │
│       │         │  └── home 节点
│       │         └── en 节点（语言）
│       └── myproject 节点（项目）
└── content 节点（内容根）
```

## 💻 JCR API 基础操作

### 示例 1：获取 Session 和访问节点

```java
package com.example.core.jcr;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.commons.JcrUtils;
import org.osgi.service.component.annotations.Component;

/**
 * JCR Session 管理示例
 * 
 * Session 是与 JCR 仓库交互的主要接口
 * 类似于数据库连接
 */
@Component(service = JcrBasicExample.class)
public class JcrBasicExample {

    /**
     * 获取 JCR Session
     * 
     * @return Session 对象
     * @throws RepositoryException 如果无法连接到仓库
     */
    public Session getSession() throws RepositoryException {
        // 获取仓库实例（在 AEM 中通常通过 Sling 框架注入）
        Repository repository = JcrUtils.getRepository();
        
        // 创建凭证
        SimpleCredentials creds = new SimpleCredentials(
            "admin",                    // 用户名
            "admin".toCharArray()       // 密码（字符数组）
        );
        
        // 登录并获取 Session
        Session session = repository.login(creds);
        
        return session;
    }

    /**
     * 访问现有节点
     * 
     * @param path 节点路径
     * @return Node 对象
     */
    public Node getNode(String path) throws RepositoryException {
        Session session = getSession();
        try {
            // 检查节点是否存在
            if (session.nodeExists(path)) {
                // 获取节点
                Node node = session.getNode(path);
                return node;
            } else {
                throw new RepositoryException("节点不存在: " + path);
            }
        } finally {
            // 使用完毕后注销 Session
            session.logout();
        }
    }
}
```

### 示例 2：读取节点属性

```java
package com.example.core.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * 读取节点属性的示例
 */
public class JcrPropertyReader {

    /**
     * 读取节点的单个属性
     * 
     * @param session JCR Session
     * @param nodePath 节点路径
     * @param propertyName 属性名称
     * @return 属性值（字符串）
     */
    public String readProperty(Session session, String nodePath, String propertyName) 
            throws RepositoryException {
        
        // 获取节点
        Node node = session.getNode(nodePath);
        
        // 检查属性是否存在
        if (node.hasProperty(propertyName)) {
            // 获取属性
            Property property = node.getProperty(propertyName);
            
            // 根据属性类型获取值
            if (property.isMultiple()) {
                // 多值属性：返回第一个值
                Value[] values = property.getValues();
                return values[0].getString();
            } else {
                // 单值属性：直接获取
                return property.getString();
            }
        } else {
            return null; // 属性不存在
        }
    }

    /**
     * 读取多个属性值
     * 
     * @param session JCR Session
     * @param nodePath 节点路径
     * @param propertyName 属性名称
     * @return 属性值数组
     */
    public String[] readMultipleProperty(Session session, String nodePath, String propertyName) 
            throws RepositoryException {
        
        Node node = session.getNode(nodePath);
        
        if (node.hasProperty(propertyName)) {
            Property property = node.getProperty(propertyName);
            
            if (property.isMultiple()) {
                // 多值属性：返回所有值
                Value[] values = property.getValues();
                String[] result = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    result[i] = values[i].getString();
                }
                return result;
            } else {
                // 单值属性：转换为数组
                return new String[]{property.getString()};
            }
        }
        
        return new String[0]; // 属性不存在，返回空数组
    }

    /**
     * 读取不同数据类型的属性
     * 
     * @param session JCR Session
     * @param nodePath 节点路径
     * @param propertyName 属性名称
     * @return 属性值（Object，根据类型返回）
     */
    public Object readTypedProperty(Session session, String nodePath, String propertyName) 
            throws RepositoryException {
        
        Node node = session.getNode(nodePath);
        Property property = node.getProperty(propertyName);
        
        // 根据属性类型返回相应的值
        switch (property.getType()) {
            case javax.jcr.PropertyType.STRING:
                return property.getString();
            
            case javax.jcr.PropertyType.LONG:
                return property.getLong();
            
            case javax.jcr.PropertyType.DOUBLE:
                return property.getDouble();
            
            case javax.jcr.PropertyType.BOOLEAN:
                return property.getBoolean();
            
            case javax.jcr.PropertyType.DATE:
                return property.getDate().getTime(); // 返回时间戳
            
            default:
                return property.getString(); // 默认返回字符串
        }
    }
}
```

### 示例 3：创建节点和属性

```java
package com.example.core.jcr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * 创建和修改节点的示例
 */
public class JcrNodeCreator {

    /**
     * 创建新节点
     * 
     * @param session JCR Session
     * @param parentPath 父节点路径
     * @param nodeName 新节点名称
     * @param nodeType 节点类型（可选，如 "nt:unstructured"）
     * @return 创建的节点
     */
    public Node createNode(Session session, String parentPath, String nodeName, String nodeType) 
            throws RepositoryException {
        
        // 获取父节点
        Node parentNode = session.getNode(parentPath);
        
        // 检查子节点是否已存在
        if (parentNode.hasNode(nodeName)) {
            // 如果存在，直接返回
            return parentNode.getNode(nodeName);
        }
        
        // 添加新节点
        Node newNode;
        if (nodeType != null && !nodeType.isEmpty()) {
            // 指定节点类型创建
            newNode = parentNode.addNode(nodeName, nodeType);
        } else {
            // 使用默认节点类型创建
            newNode = parentNode.addNode(nodeName);
        }
        
        // 保存更改（必须调用，否则更改不会持久化）
        session.save();
        
        return newNode;
    }

    /**
     * 设置节点属性
     * 
     * @param node 目标节点
     * @param propertyName 属性名称
     * @param value 属性值
     */
    public void setProperty(Node node, String propertyName, Object value) 
            throws RepositoryException {
        
        Session session = node.getSession();
        ValueFactory valueFactory = session.getValueFactory();
        
        // 根据值的类型设置属性
        if (value instanceof String) {
            node.setProperty(propertyName, (String) value);
        } else if (value instanceof Long) {
            node.setProperty(propertyName, (Long) value);
        } else if (value instanceof Integer) {
            node.setProperty(propertyName, (Integer) value);
        } else if (value instanceof Double) {
            node.setProperty(propertyName, (Double) value);
        } else if (value instanceof Boolean) {
            node.setProperty(propertyName, (Boolean) value);
        } else if (value instanceof String[]) {
            // 多值属性
            Value[] values = new Value[((String[]) value).length];
            for (int i = 0; i < values.length; i++) {
                values[i] = valueFactory.createValue(((String[]) value)[i]);
            }
            node.setProperty(propertyName, values);
        } else {
            // 默认转换为字符串
            node.setProperty(propertyName, value.toString());
        }
        
        // 保存更改
        session.save();
    }

    /**
     * 创建完整的页面结构示例
     * 
     * @param session JCR Session
     * @param pagePath 页面路径（如 /content/myproject/en/home）
     */
    public void createPageStructure(Session session, String pagePath) 
            throws RepositoryException {
        
        // 确保父路径存在（如果不存在则创建）
        ensurePathExists(session, pagePath);
        
        // 获取页面节点
        Node pageNode = session.getNode(pagePath);
        
        // 创建 jcr:content 节点（页面内容节点）
        Node contentNode;
        if (pageNode.hasNode("jcr:content")) {
            contentNode = pageNode.getNode("jcr:content");
        } else {
            // 使用 cq:PageContent 节点类型（AEM 页面内容的标准类型）
            contentNode = pageNode.addNode("jcr:content", "cq:PageContent");
        }
        
        // 设置页面属性
        setProperty(contentNode, "jcr:title", "我的首页");
        setProperty(contentNode, "jcr:description", "这是首页的描述");
        setProperty(contentNode, "sling:resourceType", "myproject/components/page/home");
        
        // 创建段落系统（par）节点（用于存放组件）
        Node parNode;
        if (!contentNode.hasNode("par")) {
            parNode = contentNode.addNode("par");
            setProperty(parNode, "sling:resourceType", "foundation/components/parsys");
        }
        
        // 保存所有更改
        session.save();
    }

    /**
     * 确保路径存在（如果不存在则创建）
     * 
     * @param session JCR Session
     * @param path 目标路径
     */
    private void ensurePathExists(Session session, String path) throws RepositoryException {
        String[] segments = path.split("/");
        String currentPath = "";
        
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue; // 跳过空段（开头的 /）
            }
            
            currentPath += "/" + segment;
            
            if (!session.nodeExists(currentPath)) {
                // 获取父节点
                int lastSlash = currentPath.lastIndexOf('/');
                String parentPath = currentPath.substring(0, lastSlash);
                if (parentPath.isEmpty()) {
                    parentPath = "/";
                }
                
                Node parentNode = session.getNode(parentPath);
                parentNode.addNode(segment);
                session.save();
            }
        }
    }
}
```

### 示例 4：查询和遍历节点

```java
package com.example.core.jcr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * JCR 查询示例
 * 
 * JCR 支持 SQL 和 XPath 两种查询语言
 */
public class JcrQueryExample {

    /**
     * 遍历节点的所有子节点
     * 
     * @param session JCR Session
     * @param parentPath 父节点路径
     */
    public void traverseChildren(Session session, String parentPath) 
            throws RepositoryException {
        
        Node parentNode = session.getNode(parentPath);
        
        // 获取所有子节点
        NodeIterator children = parentNode.getNodes();
        
        System.out.println("节点 " + parentPath + " 的子节点：");
        while (children.hasNext()) {
            Node child = children.nextNode();
            System.out.println("  - " + child.getName() + " (路径: " + child.getPath() + ")");
        }
    }

    /**
     * 使用 SQL 查询节点
     * 
     * @param session JCR Session
     * @param sqlQuery SQL 查询语句
     * @return 查询结果迭代器
     */
    public NodeIterator queryWithSQL(Session session, String sqlQuery) 
            throws RepositoryException {
        
        // 获取查询管理器
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        
        // 创建 SQL 查询
        Query query = queryManager.createQuery(sqlQuery, Query.SQL);
        
        // 执行查询
        QueryResult result = query.execute();
        
        // 返回节点迭代器
        return result.getNodes();
    }

    /**
     * 查找所有 cq:Page 类型的节点
     * 
     * @param session JCR Session
     * @param rootPath 搜索根路径（如 /content/myproject）
     * @return 页面节点迭代器
     */
    public NodeIterator findAllPages(Session session, String rootPath) 
            throws RepositoryException {
        
        // SQL 查询：查找指定路径下所有 cq:Page 类型的节点
        String sql = "SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE('" + rootPath + "')";
        
        return queryWithSQL(session, sql);
    }

    /**
     * 根据属性值查找节点（使用 SQL）
     * 
     * @param session JCR Session
     * @param propertyName 属性名称
     * @param propertyValue 属性值
     * @param rootPath 搜索根路径
     * @return 匹配的节点迭代器
     */
    public NodeIterator findNodesByProperty(Session session, String propertyName, 
                                           String propertyValue, String rootPath) 
            throws RepositoryException {
        
        // SQL 查询：根据属性值查找节点
        String sql = "SELECT * FROM [nt:base] " +
                     "WHERE [" + propertyName + "] = '" + propertyValue + "' " +
                     "AND ISDESCENDANTNODE('" + rootPath + "')";
        
        return queryWithSQL(session, sql);
    }

    /**
     * 使用 XPath 查询（另一种查询方式）
     * 
     * @param session JCR Session
     * @param xpathQuery XPath 查询语句
     * @return 查询结果迭代器
     */
    public NodeIterator queryWithXPath(Session session, String xpathQuery) 
            throws RepositoryException {
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        
        // 创建 XPath 查询
        Query query = queryManager.createQuery(xpathQuery, Query.XPATH);
        
        QueryResult result = query.execute();
        return result.getNodes();
    }

    /**
     * 递归遍历节点树（深度优先搜索）
     * 
     * @param node 起始节点
     * @param depth 当前深度（用于缩进显示）
     */
    public void traverseTree(Node node, int depth) throws RepositoryException {
        // 打印当前节点（带缩进）
        String indent = "  ".repeat(depth);
        System.out.println(indent + node.getName() + " (" + node.getPath() + ")");
        
        // 遍历子节点
        NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            Node child = children.nextNode();
            // 递归遍历子节点
            traverseTree(child, depth + 1);
        }
    }
}
```

## 📋 JCR 属性类型

JCR 支持以下属性类型：

| 类型 | 常量 | Java 类型 | 说明 |
|------|------|-----------|------|
| String | `PropertyType.STRING` | `String` | 字符串 |
| Long | `PropertyType.LONG` | `Long` | 长整数 |
| Double | `PropertyType.DOUBLE` | `Double` | 双精度浮点数 |
| Boolean | `PropertyType.BOOLEAN` | `Boolean` | 布尔值 |
| Date | `PropertyType.DATE` | `Calendar` | 日期时间 |
| Binary | `PropertyType.BINARY` | `InputStream` | 二进制数据（文件） |
| Reference | `PropertyType.REFERENCE` | `String` | 节点引用 |
| WeakReference | `PropertyType.WEAKREFERENCE` | `String` | 弱引用 |

## 🔑 关键要点

1. **Session 管理**：Session 类似于数据库连接，使用后要注销
2. **保存更改**：修改节点后必须调用 `session.save()` 才能持久化
3. **路径规范**：JCR 路径总是以 `/` 开头
4. **节点类型**：节点可以有不同的类型（如 `nt:unstructured`, `cq:Page`）
5. **查询语言**：支持 SQL 和 XPath 两种查询方式

## ➡️ 下一步

在下一节中，我们将学习 **Apache Sling 框架**，了解 AEM 如何处理 HTTP 请求和路由。

