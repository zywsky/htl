# 故障排除指南

## 常见问题和解决方案

本文档提供详细的故障排除指南，帮助你解决在使用组件信息提取工具时遇到的问题。

---

## 目录

1. [编译和构建问题](#编译和构建问题)
2. [运行时错误](#运行时错误)
3. [JCR 访问问题](#jcr-访问问题)
4. [数据提取问题](#数据提取问题)
5. [性能问题](#性能问题)
6. [部署问题](#部署问题)

---

## 编译和构建问题

### 问题 1: Maven 依赖下载失败

**症状**：
```
[ERROR] Failed to execute goal ...: Could not resolve dependencies
```

**解决方案**：

1. **检查网络连接**
   ```bash
   ping repo.maven.apache.org
   ```

2. **清理 Maven 缓存**
   ```bash
   rm -rf ~/.m2/repository
   mvn clean install
   ```

3. **使用国内镜像**（如果在中国）
   
   编辑 `~/.m2/settings.xml`：
   ```xml
   <mirrors>
     <mirror>
       <id>aliyun</id>
       <mirrorOf>central</mirrorOf>
       <url>https://maven.aliyun.com/repository/public</url>
     </mirror>
   </mirrors>
   ```

4. **检查 Maven 版本**
   ```bash
   mvn --version
   # 需要 Maven 3.6+
   ```

### 问题 2: 编译错误 - 找不到类

**症状**：
```
[ERROR] cannot find symbol: class ComponentInfoExtractor
```

**解决方案**：

1. **检查包名是否正确**
   ```java
   package com.aem.component.info; // 确保包名正确
   ```

2. **检查导入语句**
   ```java
   import com.aem.component.info.ComponentInfoExtractor;
   ```

3. **重新编译**
   ```bash
   mvn clean compile
   ```

### 问题 3: 版本冲突

**症状**：
```
[ERROR] ClassNotFoundException: org.apache.sling.api.SlingHttpServletRequest
```

**解决方案**：

1. **检查依赖版本**
   ```xml
   <dependency>
       <groupId>org.apache.sling</groupId>
       <artifactId>org.apache.sling.api</artifactId>
       <version>2.27.0</version>
   </dependency>
   ```

2. **使用依赖树分析**
   ```bash
   mvn dependency:tree
   ```

3. **排除冲突依赖**
   ```xml
   <dependency>
       <groupId>...</groupId>
       <artifactId>...</artifactId>
       <exclusions>
           <exclusion>
               <groupId>conflicting-group</groupId>
               <artifactId>conflicting-artifact</artifactId>
           </exclusion>
       </exclusions>
   </dependency>
   ```

---

## 运行时错误

### 问题 4: NullPointerException

**症状**：
```
java.lang.NullPointerException
    at com.aem.component.info.ComponentInfoExtractor.extractComponentInfo
```

**可能原因**：
- Session 为 null
- 组件节点不存在
- 属性不存在

**解决方案**：

1. **检查 Session**
   ```java
   if (session == null) {
       throw new IllegalStateException("Session is null");
   }
   ```

2. **检查节点是否存在**
   ```java
   if (!JCRUtil.nodeExists(session, componentPath)) {
       // 处理节点不存在的情况
   }
   ```

3. **使用安全方法**
   ```java
   String value = JCRUtil.getProperty(node, "propertyName", "defaultValue");
   ```

### 问题 5: RepositoryException

**症状**：
```
javax.jcr.RepositoryException: Access denied
```

**可能原因**：
- 权限不足
- 节点路径错误
- 会话已关闭

**解决方案**：

1. **检查权限**
   ```java
   // 使用管理会话（仅开发环境）
   Session session = repository.loginAdministrative(null);
   
   // 或使用有权限的服务用户（生产环境）
   Session session = repository.loginService("component-reader", null);
   ```

2. **检查路径**
   ```java
   // 确保路径正确
   String path = "/apps/myproject/components/mycomponent";
   if (JCRUtil.nodeExists(session, path)) {
       // 继续处理
   }
   ```

3. **确保会话未关闭**
   ```java
   try {
       // 使用会话
   } finally {
       // 只在最后关闭
       if (session != null) {
           session.logout();
       }
   }
   ```

### 问题 6: ClassCastException

**症状**：
```
java.lang.ClassCastException: java.util.HashMap cannot be cast to java.util.Map
```

**可能原因**：
- 类型转换错误
- JSON 反序列化问题

**解决方案**：

1. **使用正确的类型转换**
   ```java
   @SuppressWarnings("unchecked")
   Map<String, String> props = (Map<String, String>) componentInfo.get("basicProperties");
   ```

2. **检查类型**
   ```java
   Object obj = componentInfo.get("basicProperties");
   if (obj instanceof Map) {
       Map<String, String> props = (Map<String, String>) obj;
   }
   ```

---

## JCR 访问问题

### 问题 7: 无法访问 /libs 路径

**症状**：
```
PathNotFoundException: /libs/core/wcm/components/text
```

**解决方案**：

1. **使用管理会话**
   ```java
   Session session = repository.loginAdministrative(null);
   ```

2. **检查节点是否存在**
   ```java
   if (JCRUtil.nodeExists(session, path)) {
       Node node = JCRUtil.getNode(session, path);
   }
   ```

3. **处理异常**
   ```java
   try {
       Node node = session.getNode(path);
   } catch (PathNotFoundException e) {
       log.warn("节点不存在或无法访问: " + path);
   }
   ```

### 问题 8: 会话超时

**症状**：
```
RepositoryException: Session is closed
```

**解决方案**：

1. **检查会话状态**
   ```java
   if (session.isLive()) {
       // 会话有效
   }
   ```

2. **重新获取会话**
   ```java
   if (!session.isLive()) {
       session = repository.loginAdministrative(null);
   }
   ```

3. **使用 ResourceResolver**（推荐）
   ```java
   try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(...)) {
       Session session = resolver.adaptTo(Session.class);
       // 使用会话
   } // 自动关闭
   ```

### 问题 9: 查询失败

**症状**：
```
QueryException: Invalid query syntax
```

**解决方案**：

1. **检查查询语法**
   ```java
   // SQL2 查询
   String query = "SELECT * FROM [cq:Component] WHERE [sling:resourceType] = '...'";
   
   // XPath 查询
   String query = "/jcr:root/apps//element(*, cq:Component)";
   ```

2. **转义特殊字符**
   ```java
   String resourceType = resourceType.replace("'", "''");
   String query = "SELECT * FROM [cq:Component] WHERE [sling:resourceType] = '" + resourceType + "'";
   ```

3. **使用参数化查询**（如果支持）

---

## 数据提取问题

### 问题 10: 对话框字段提取不完整

**症状**：
- 某些字段没有被提取
- 字段信息不完整

**解决方案**：

1. **检查对话框结构**
   ```java
   // 打印对话框结构
   JCRUtil.printNodeInfo(dialogNode, "");
   ```

2. **使用递归提取**
   ```java
   // DialogAnalyzer 已经使用递归方法
   // 确保没有跳过嵌套的 items 节点
   ```

3. **添加字段类型**
   ```java
   // 在 DialogAnalyzer.isFieldNode() 中添加新的字段类型
   if (nodeType.contains("granite/ui/components/coral/foundation/form/yourfieldtype")) {
       return true;
   }
   ```

### 问题 11: 属性值为 null

**症状**：
- 提取的属性值为 null
- 某些属性缺失

**解决方案**：

1. **使用默认值**
   ```java
   String value = JCRUtil.getProperty(node, "propertyName", "defaultValue");
   ```

2. **检查属性是否存在**
   ```java
   if (node.hasProperty("propertyName")) {
       Property prop = node.getProperty("propertyName");
       // 处理属性
   }
   ```

3. **处理多值属性**
   ```java
   if (property.isMultiple()) {
       Value[] values = property.getValues();
       // 处理多值
   }
   ```

### 问题 12: JSON 导出格式错误

**症状**：
- JSON 格式不正确
- 某些字段缺失

**解决方案**：

1. **检查对象映射**
   ```java
   objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
   objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
   ```

2. **验证 JSON**
   ```bash
   cat output.json | python -m json.tool
   ```

3. **处理循环引用**
   ```java
   objectMapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
   ```

---

## 性能问题

### 问题 13: 提取速度慢

**症状**：
- 提取单个组件需要很长时间
- 批量提取非常慢

**解决方案**：

1. **复用会话**
   ```java
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

2. **限制搜索深度**
   ```java
   List<Node> nodes = JCRUtil.getAllDescendants(node, 5); // 最大深度 5
   ```

3. **使用缓存**
   ```java
   private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();
   ```

4. **批量处理**
   ```java
   // 使用批量提取方法
   List<Map<String, Object>> components = extractor.extractComponentsFromPath(basePath);
   ```

### 问题 14: 内存不足

**症状**：
```
OutOfMemoryError: Java heap space
```

**解决方案**：

1. **增加堆内存**
   ```bash
   java -Xmx2g -jar your-app.jar
   ```

2. **分批处理**
   ```java
   int batchSize = 10;
   for (int i = 0; i < components.size(); i += batchSize) {
       List<Map<String, Object>> batch = components.subList(i, 
           Math.min(i + batchSize, components.size()));
       // 处理批次
   }
   ```

3. **流式处理**
   ```java
   // 不要一次性加载所有数据
   // 使用迭代器或流式处理
   ```

---

## 部署问题

### 问题 15: Bundle 无法启动

**症状**：
- Bundle 状态为 "Installed" 而不是 "Active"
- 错误日志显示导入包失败

**解决方案**：

1. **检查导入包**
   ```xml
   <Import-Package>
       org.apache.sling.api.*,
       javax.jcr.*,
       ...
   </Import-Package>
   ```

2. **检查依赖**
   ```bash
   mvn dependency:tree
   ```

3. **查看错误日志**
   - 访问 `http://localhost:4502/system/console/bundles`
   - 点击 bundle 查看错误信息

### 问题 16: 服务无法注入

**症状**：
```
NullPointerException: repository is null
```

**解决方案**：

1. **检查 @Reference 注解**
   ```java
   @Reference
   private SlingRepository repository;
   ```

2. **检查服务注册**
   ```java
   @Component(service = YourService.class)
   public class YourService {
       // ...
   }
   ```

3. **使用激活方法**
   ```java
   @Activate
   protected void activate() {
       // 确保依赖已注入
       if (repository == null) {
           throw new IllegalStateException("Repository not injected");
       }
   }
   ```

---

## 调试技巧

### 1. 启用详细日志

```java
Logger log = LoggerFactory.getLogger(YourClass.class);
log.debug("调试信息: {}", variable);
```

### 2. 打印节点信息

```java
JCRUtil.printNodeInfo(node, "");
```

### 3. 使用断点调试

在 IDE 中设置断点，逐步调试。

### 4. 检查 JCR 内容

使用 CRXDE Lite 手动检查节点结构。

---

## 获取帮助

如果以上方法都无法解决问题：

1. **查看日志**
   - AEM 错误日志：`crx-quickstart/logs/error.log`
   - 应用日志：`logs/component-info.log`

2. **检查文档**
   - README.md
   - FAQ.md
   - ADVANCED_GUIDE.md

3. **运行示例代码**
   - 参考 examples 目录下的示例

4. **简化问题**
   - 创建最小复现示例
   - 逐步添加功能

---

## 预防措施

1. **错误处理**: 始终使用 try-catch
2. **空值检查**: 检查 null 值
3. **资源清理**: 在 finally 中关闭资源
4. **日志记录**: 记录关键操作
5. **单元测试**: 编写测试用例

---

祝你解决问题顺利！🔧

