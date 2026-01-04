# AEM 组件信息提取 - 高级指南

## 目录

1. [在 AEM Bundle 中使用](#在-aem-bundle-中使用)
2. [通过 HTTP Servlet 访问](#通过-http-servlet-访问)
3. [性能优化](#性能优化)
4. [常见问题解决](#常见问题解决)
5. [扩展功能](#扩展功能)

## 在 AEM Bundle 中使用

### 步骤 1: 创建 Maven Bundle 项目

如果你还没有 AEM bundle 项目，需要创建一个标准的 AEM Maven 项目结构。

### 步骤 2: 添加依赖

在你的 bundle 的 `pom.xml` 中添加必要的依赖：

```xml
<dependencies>
    <!-- AEM 和 Sling 依赖 -->
    <dependency>
        <groupId>org.apache.sling</groupId>
        <artifactId>org.apache.sling.api</artifactId>
        <version>2.27.0</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- 其他依赖... -->
</dependencies>
```

### 步骤 3: 使用 OSGi 服务

参考 `OSGiComponentExtractorService.java` 示例，创建一个 OSGi 服务：

```java
@Reference
private SlingRepository repository;

public Map<String, Object> extractComponent(String path) {
    Session session = repository.loginAdministrative(null);
    try {
        ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        return extractor.extractComponentInfo(path);
    } finally {
        session.logout();
    }
}
```

## 通过 HTTP Servlet 访问

### 创建 HTTP Servlet

创建一个 HTTP Servlet 来通过 REST API 访问组件信息：

```java
@Component(service = Servlet.class)
@SlingServletPaths("/bin/componentinfo")
public class ComponentInfoServlet extends SlingSafeMethodsServlet {

    @Reference
    private OSGiComponentExtractorService extractorService;

    @Override
    protected void doGet(SlingHttpServletRequest request, 
                        SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        String componentPath = request.getParameter("path");
        if (componentPath == null) {
            response.sendError(400, "缺少 path 参数");
            return;
        }
        
        String json = extractorService.exportComponentInfoAsJson(componentPath);
        if (json == null) {
            response.sendError(500, "提取组件信息失败");
            return;
        }
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
```

### 使用示例

```bash
# 获取组件信息
curl "http://localhost:4502/bin/componentinfo?path=/apps/myproject/components/mycomponent"

# 批量获取
curl "http://localhost:4502/bin/componentinfo/batch?basePath=/apps/myproject/components"
```

## 性能优化

### 1. 会话管理

**避免**: 频繁创建和关闭会话
```java
// 不好
for (String path : paths) {
    Session session = repository.loginAdministrative(null);
    // ... 使用会话
    session.logout();
}
```

**推荐**: 复用会话
```java
// 好
Session session = repository.loginAdministrative(null);
try {
    ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
    for (String path : paths) {
        extractor.extractComponentInfo(path);
    }
} finally {
    session.logout();
}
```

### 2. 批量处理

使用批量提取方法而不是逐个提取：

```java
// 推荐
List<Map<String, Object>> components = 
    extractor.extractComponentsFromPath("/apps/myproject/components");

// 而不是
for (String path : paths) {
    extractor.extractComponentInfo(path);
}
```

### 3. 缓存结果

对于不经常变化的组件信息，考虑使用缓存：

```java
@Component
public class CachedComponentExtractor {
    
    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();
    
    public Map<String, Object> getComponentInfo(String path) {
        return cache.computeIfAbsent(path, this::extractComponentInfo);
    }
    
    public void clearCache() {
        cache.clear();
    }
}
```

### 4. 限制搜索深度

在提取组件信息时，限制递归深度：

```java
// 在 findComponentNodes 方法中设置合理的深度限制
List<Node> components = findComponentNodes(baseNode, 0, 5); // 最大深度 5
```

## 常见问题解决

### 问题 1: 无法访问 /libs 路径下的组件

**原因**: `/libs` 路径通常是只读的，或者当前会话没有读取权限。

**解决方案**:
```java
// 使用管理会话
Session session = repository.loginAdministrative(null);

// 或者检查节点是否存在
if (JCRUtil.nodeExists(session, path)) {
    // 继续处理
}
```

### 问题 2: 对话框字段提取不完整

**原因**: 对话框结构可能使用了嵌套的 items 节点。

**解决方案**: 确保使用递归方法 `extractFieldsRecursive` 来提取所有字段。

### 问题 3: 组件节点类型不是 cq:Component

**原因**: 某些组件可能使用不同的节点类型或 mixin。

**解决方案**:
```java
// 检查多个可能的节点类型
boolean isComponent = node.isNodeType("cq:Component") || 
                      node.hasProperty("sling:resourceType");
```

### 问题 4: 导出 JSON 时内存不足

**原因**: 大型组件树可能导致内存问题。

**解决方案**:
- 使用流式 JSON 处理
- 分批处理组件
- 增加 JVM 堆内存

## 扩展功能

### 1. 添加组件使用统计

```java
public Map<String, Object> getComponentUsage(String resourceType, Session session) 
        throws RepositoryException {
    
    String query = "SELECT * FROM [cq:PageContent] WHERE [sling:resourceType] = '" 
                   + resourceType + "'";
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query q = queryManager.createQuery(query, Query.JCR_SQL2);
    QueryResult result = q.execute();
    
    int count = 0;
    NodeIterator nodes = result.getNodes();
    while (nodes.hasNext()) {
        nodes.nextNode();
        count++;
    }
    
    Map<String, Object> usage = new HashMap<>();
    usage.put("usageCount", count);
    usage.put("resourceType", resourceType);
    return usage;
}
```

### 2. 提取组件模板内容

```java
public String extractTemplateContent(Node componentNode) 
        throws RepositoryException, IOException {
    
    Node templateNode = JCRUtil.getChildNode(componentNode, "component.html");
    if (templateNode != null) {
        Property contentProperty = templateNode.getProperty("jcr:data");
        if (contentProperty != null) {
            InputStream stream = contentProperty.getBinary().getStream();
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }
    return null;
}
```

### 3. 分析组件依赖关系图

```java
public Map<String, Object> buildComponentDependencyGraph(String basePath, Session session) {
    // 提取所有组件
    ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
    List<Map<String, Object>> components = extractor.extractComponentsFromPath(basePath);
    
    // 构建依赖图
    Map<String, List<String>> graph = new HashMap<>();
    
    for (Map<String, Object> component : components) {
        @SuppressWarnings("unchecked")
        Map<String, String> basicProps = 
            (Map<String, String>) component.get("basicProperties");
        
        if (basicProps != null) {
            String resourceType = basicProps.get("sling:resourceType");
            String superType = basicProps.get("sling:resourceSuperType");
            
            if (superType != null) {
                graph.computeIfAbsent(resourceType, k -> new ArrayList<>())
                     .add(superType);
            }
        }
    }
    
    Map<String, Object> result = new HashMap<>();
    result.put("dependencyGraph", graph);
    return result;
}
```

### 4. 导出为其他格式

除了 JSON，你还可以导出为其他格式：

```java
// 导出为 CSV
public void exportToCsv(List<Map<String, Object>> components, String outputPath) {
    // CSV 导出逻辑
}

// 导出为 XML
public void exportToXml(Map<String, Object> componentInfo, String outputPath) {
    // XML 导出逻辑
}

// 导出为 Markdown 文档
public void exportToMarkdown(Map<String, Object> componentInfo, String outputPath) {
    // Markdown 导出逻辑
}
```

### 5. 集成到构建流程

在 Maven 构建过程中自动提取组件信息：

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <phase>compile</phase>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>com.aem.component.info.tools.ComponentExtractorTool</mainClass>
                <arguments>
                    <argument>--basePath</argument>
                    <argument>/apps/myproject/components</argument>
                    <argument>--output</argument>
                    <argument>${project.build.directory}/component-info</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 最佳实践总结

1. **会话管理**: 始终在 finally 块中关闭会话
2. **错误处理**: 使用 try-catch 处理 RepositoryException
3. **权限**: 根据场景使用适当的会话权限（administrative vs user）
4. **性能**: 批量处理，复用会话，使用缓存
5. **日志**: 记录关键操作和错误信息
6. **测试**: 在不同环境（开发、测试、生产）中测试代码

## 下一步学习

- 深入学习 JCR 查询 API
- 了解 Sling Resource API
- 学习 AEM 组件的其他配置（如 _cq_template）
- 探索 HTL 模板解析
- 研究 AEM 客户端库结构

