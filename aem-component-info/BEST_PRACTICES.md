# 最佳实践指南

## AEM 组件信息提取的最佳实践

本文档总结了在使用组件信息提取工具时的最佳实践，帮助你编写更高效、更安全、更易维护的代码。

---

## 目录

1. [代码质量](#代码质量)
2. [性能优化](#性能优化)
3. [错误处理](#错误处理)
4. [安全实践](#安全实践)
5. [资源管理](#资源管理)
6. [测试实践](#测试实践)
7. [文档和注释](#文档和注释)

---

## 代码质量

### 1. 使用工具类方法

✅ **好的做法**：
```java
// 使用 JCRUtil 的安全方法
String title = JCRUtil.getProperty(node, "jcr:title", "默认标题");
Node child = JCRUtil.getChildNode(node, "childName");
```

❌ **不好的做法**：
```java
// 直接访问，没有错误处理
String title = node.getProperty("jcr:title").getString();
Node child = node.getNode("childName");
```

### 2. 避免硬编码路径

✅ **好的做法**：
```java
private static final String COMPONENTS_BASE_PATH = "/apps/myproject/components";
String componentPath = COMPONENTS_BASE_PATH + "/mycomponent";
```

❌ **不好的做法**：
```java
String componentPath = "/apps/myproject/components/mycomponent"; // 硬编码
```

### 3. 使用常量定义

✅ **好的做法**：
```java
public class ComponentConstants {
    public static final String CQ_COMPONENT_TYPE = "cq:Component";
    public static final String SLING_RESOURCE_TYPE = "sling:resourceType";
    public static final String JCR_TITLE = "jcr:title";
}
```

### 4. 方法职责单一

✅ **好的做法**：
```java
// 每个方法只做一件事
public Map<String, String> extractBasicProperties(Node node) { ... }
public Map<String, Object> extractDialogInfo(Node node) { ... }
public Map<String, Object> extractTemplateInfo(Node node) { ... }
```

❌ **不好的做法**：
```java
// 一个方法做太多事情
public Map<String, Object> extractEverything(Node node) {
    // 提取基本属性
    // 提取对话框
    // 提取模板
    // 提取依赖
    // ... 太多职责
}
```

---

## 性能优化

### 1. 复用 Session

✅ **好的做法**：
```java
Session session = repository.loginAdministrative(null);
try {
    ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
    for (String path : componentPaths) {
        extractor.extractComponentInfo(path);
    }
} finally {
    session.logout();
}
```

❌ **不好的做法**：
```java
for (String path : componentPaths) {
    Session session = repository.loginAdministrative(null);
    try {
        ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        extractor.extractComponentInfo(path);
    } finally {
        session.logout();
    }
}
```

### 2. 使用批量操作

✅ **好的做法**：
```java
// 批量提取
List<Map<String, Object>> components = 
    extractor.extractComponentsFromPath("/apps/myproject/components");
```

❌ **不好的做法**：
```java
// 逐个提取
List<String> paths = getComponentPaths();
List<Map<String, Object>> components = new ArrayList<>();
for (String path : paths) {
    components.add(extractor.extractComponentInfo(path));
}
```

### 3. 限制搜索深度

✅ **好的做法**：
```java
// 限制递归深度
List<Node> nodes = JCRUtil.getAllDescendants(node, 5); // 最大深度 5
```

❌ **不好的做法**：
```java
// 无限制递归
List<Node> nodes = JCRUtil.getAllDescendants(node, -1); // 可能很深
```

### 4. 使用缓存

✅ **好的做法**：
```java
private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();

public Map<String, Object> getComponentInfo(String path) {
    return cache.computeIfAbsent(path, this::extractComponentInfo);
}

@Activate
protected void activate() {
    // 定期清理缓存
    scheduler.schedule(this::clearCache, 3600); // 每小时清理一次
}
```

### 5. 延迟加载

✅ **好的做法**：
```java
// 只在需要时提取详细信息
public Map<String, Object> getComponentInfo(String path, boolean includeDetails) {
    if (includeDetails) {
        return extractComponentInfo(path);
    } else {
        return extractComponentInfoSimple(path);
    }
}
```

---

## 错误处理

### 1. 始终处理异常

✅ **好的做法**：
```java
try {
    Map<String, Object> info = extractor.extractComponentInfo(path);
    return info;
} catch (RepositoryException e) {
    log.error("提取组件信息失败: " + path, e);
    return createErrorResponse("提取失败: " + e.getMessage());
} catch (Exception e) {
    log.error("意外错误", e);
    return createErrorResponse("系统错误");
}
```

❌ **不好的做法**：
```java
// 不处理异常
Map<String, Object> info = extractor.extractComponentInfo(path);
return info;
```

### 2. 提供有意义的错误消息

✅ **好的做法**：
```java
if (!JCRUtil.nodeExists(session, path)) {
    throw new IllegalArgumentException("组件路径不存在: " + path);
}
```

❌ **不好的做法**：
```java
if (!JCRUtil.nodeExists(session, path)) {
    throw new RuntimeException("错误"); // 消息不明确
}
```

### 3. 使用检查方法

✅ **好的做法**：
```java
public Map<String, Object> extractComponentInfo(String path) {
    Node componentNode = JCRUtil.getNode(session, path);
    if (componentNode == null) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "组件节点不存在: " + path);
        return error;
    }
    
    try {
        if (!componentNode.isNodeType("cq:Component")) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "指定的节点不是组件节点");
            return error;
        }
    } catch (RepositoryException e) {
        // 处理异常
    }
    
    // 继续处理...
}
```

---

## 安全实践

### 1. 不要在生产环境使用管理会话

✅ **好的做法**（生产环境）：
```java
Session session = repository.loginService("component-reader", null);
```

❌ **不好的做法**：
```java
Session session = repository.loginAdministrative(null); // 仅用于开发
```

### 2. 验证输入参数

✅ **好的做法**：
```java
public Map<String, Object> extractComponentInfo(String path) {
    if (path == null || path.isEmpty()) {
        throw new IllegalArgumentException("路径不能为空");
    }
    
    if (!path.startsWith("/apps/") && !path.startsWith("/libs/")) {
        throw new IllegalArgumentException("路径必须从 /apps/ 或 /libs/ 开始");
    }
    
    // 防止路径遍历攻击
    if (path.contains("..")) {
        throw new IllegalArgumentException("无效的路径");
    }
    
    // 继续处理...
}
```

### 3. 限制资源访问

✅ **好的做法**：
```java
// 只允许访问特定路径
private static final String ALLOWED_BASE_PATH = "/apps/myproject";

public Map<String, Object> extractComponentInfo(String path) {
    if (!path.startsWith(ALLOWED_BASE_PATH)) {
        throw new SecurityException("不允许访问该路径: " + path);
    }
    // ...
}
```

### 4. 日志敏感信息

✅ **好的做法**：
```java
log.debug("提取组件信息: {}", path); // 只记录路径
// 不记录敏感信息
```

❌ **不好的做法**：
```java
log.info("用户 " + username + " 访问组件 " + path); // 可能泄露敏感信息
```

---

## 资源管理

### 1. 始终关闭 Session

✅ **好的做法**：
```java
Session session = repository.loginAdministrative(null);
try {
    // 使用会话
} finally {
    if (session != null) {
        session.logout();
    }
}
```

### 2. 使用 try-with-resources（如果支持）

✅ **好的做法**（ResourceResolver）：
```java
try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(...)) {
    Session session = resolver.adaptTo(Session.class);
    // 使用会话
} // 自动关闭
```

### 3. 处理大型数据集

✅ **好的做法**：
```java
// 分批处理，避免内存溢出
int batchSize = 100;
for (int i = 0; i < allComponents.size(); i += batchSize) {
    List<Map<String, Object>> batch = allComponents.subList(i, 
        Math.min(i + batchSize, allComponents.size()));
    processBatch(batch);
}
```

---

## 测试实践

### 1. 编写单元测试

✅ **好的做法**：
```java
@Test
public void testExtractBasicProperties() {
    // 使用 Mock 对象
    Node mockNode = Mockito.mock(Node.class);
    when(mockNode.hasProperty("jcr:title")).thenReturn(true);
    when(mockNode.getProperty("jcr:title")).thenReturn(mockProperty);
    
    Map<String, String> props = ComponentPropertyExtractor.extractBasicProperties(mockNode);
    
    assertNotNull(props);
    assertEquals("Expected Title", props.get("jcr:title"));
}
```

### 2. 测试边界情况

✅ **好的做法**：
```java
@Test
public void testExtractNonExistentComponent() {
    Session session = ...;
    ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
    
    Map<String, Object> result = extractor.extractComponentInfo("/nonexistent/path");
    
    assertTrue(result.containsKey("error"));
}

@Test
public void testExtractNullPath() {
    assertThrows(IllegalArgumentException.class, () -> {
        extractor.extractComponentInfo(null);
    });
}
```

### 3. 集成测试

✅ **好的做法**：
```java
@ExtendWith(AemContextExtension.class)
class ComponentInfoExtractorIntegrationTest {
    
    @Test
    void testExtractComponentInAemContext(AemContext context) {
        // 使用 AEM Mocks 测试框架
        // ...
    }
}
```

---

## 文档和注释

### 1. 方法注释

✅ **好的做法**：
```java
/**
 * 提取组件的基本属性
 * 
 * @param componentNode 组件节点（必须是 cq:Component 类型）
 * @return 包含组件基本属性的 Map，键为属性名，值为属性值
 * @throws IllegalArgumentException 如果节点不是组件节点
 */
public static Map<String, String> extractBasicProperties(Node componentNode) {
    // 实现
}
```

### 2. 复杂逻辑注释

✅ **好的做法**：
```java
// 递归提取字段，因为对话框可能有嵌套的 items 节点
private void extractFieldsRecursive(Node node, List<Map<String, Object>> fields) {
    // 检查是否是字段节点
    if (isFieldNode(node.getPrimaryNodeType().getName())) {
        fields.add(extractFieldInfo(node));
    }
    
    // 递归处理子节点
    NodeIterator iterator = node.getNodes();
    while (iterator.hasNext()) {
        extractFieldsRecursive(iterator.nextNode(), fields);
    }
}
```

### 3. TODO 注释

✅ **好的做法**：
```java
// TODO: 添加对经典对话框的完整支持
// TODO: 优化大量组件的提取性能
// FIXME: 处理特殊字符的转义
```

---

## 代码组织

### 1. 包结构

✅ **好的做法**：
```
com.aem.component
├── info          # 核心功能
├── util          # 工具类
├── examples      # 示例代码
└── tools         # 工具程序
```

### 2. 类命名

✅ **好的做法**：
- `ComponentInfoExtractor` - 清晰、描述性
- `DialogAnalyzer` - 职责明确
- `JCRUtil` - 工具类

❌ **不好的做法**：
- `Extractor` - 不够明确
- `Util` - 太通用
- `Helper` - 不具体

### 3. 方法命名

✅ **好的做法**：
- `extractComponentInfo()` - 动词开头，描述动作
- `getProperty()` - 获取属性
- `hasDialog()` - 布尔检查

---

## 日志记录

### 1. 使用适当的日志级别

✅ **好的做法**：
```java
log.debug("提取组件: {}", path);           // 调试信息
log.info("成功提取 {} 个组件", count);      // 重要操作
log.warn("组件没有对话框配置: {}", path);   // 警告
log.error("提取失败", exception);           // 错误
```

### 2. 结构化日志

✅ **好的做法**：
```java
log.info("组件提取完成 - 路径: {}, 耗时: {}ms, 字段数: {}", 
    path, duration, fieldCount);
```

### 3. 不记录敏感信息

✅ **好的做法**：
```java
log.debug("提取组件信息: {}", componentPath); // 只记录路径
```

---

## 配置管理

### 1. 使用配置文件

✅ **好的做法**：
```java
@Component(service = ComponentInfoService.class)
@Designate(ocd = ComponentInfoServiceConfig.class)
public class ComponentInfoService {
    
    @ObjectClassDefinition(name = "Component Info Service Configuration")
    public @interface ComponentInfoServiceConfig {
        String basePath() default "/apps/myproject/components";
        int maxDepth() default 5;
        boolean enableCache() default true;
    }
    
    @Activate
    protected void activate(ComponentInfoServiceConfig config) {
        this.basePath = config.basePath();
        this.maxDepth = config.maxDepth();
    }
}
```

---

## 总结

遵循这些最佳实践可以帮助你：

1. ✅ 编写更清晰的代码
2. ✅ 提高性能
3. ✅ 减少错误
4. ✅ 增强安全性
5. ✅ 便于维护
6. ✅ 易于测试

记住：**代码是写给人看的，只是偶尔会被机器执行！** 📚

