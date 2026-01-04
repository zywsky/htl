# 部署指南

## 如何在 AEM 中部署和使用组件信息提取工具

本指南详细说明如何将组件信息提取工具部署到 AEM 实例中。

---

## 方式 1: 作为 OSGi Bundle 部署（推荐）

### 步骤 1: 创建 AEM Maven 项目

如果你还没有 AEM 项目，创建一个标准的 AEM Maven 项目：

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.adobe.aem \
  -DarchetypeArtifactId=aem-project-archetype \
  -DarchetypeVersion=41 \
  -DgroupId=com.mycompany \
  -DartifactId=my-aem-project \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=com.mycompany \
  -DappTitle="My AEM Project" \
  -DappId="myproject" \
  -DaemVersion="cloud"
```

### 步骤 2: 复制代码到项目

将组件信息提取工具的代码复制到你的 AEM 项目中：

```bash
# 复制工具类
cp -r "aem component info/src/main/java/com/aem/component" \
     your-aem-project/core/src/main/java/com/mycompany/

# 或者只复制需要的类
```

### 步骤 3: 添加依赖

在你的 `core/pom.xml` 中添加必要的依赖（如果还没有）：

```xml
<dependencies>
    <!-- Sling API -->
    <dependency>
        <groupId>org.apache.sling</groupId>
        <artifactId>org.apache.sling.api</artifactId>
        <scope>provided</scope>
    </dependency>
    
    <!-- Jackson (如果需要 JSON 导出) -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
</dependencies>
```

### 步骤 4: 创建 OSGi 服务

在你的项目中创建 OSGi 服务（参考 `OSGiComponentExtractorService.java`）：

```java
package com.mycompany.core.services;

import com.mycompany.component.info.ComponentInfoExtractor;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ComponentInfoService.class)
public class ComponentInfoService {
    
    @Reference
    private SlingRepository repository;
    
    public Map<String, Object> getComponentInfo(String path) {
        // 实现代码
    }
}
```

### 步骤 5: 构建和部署

```bash
# 构建项目
mvn clean install

# 部署到 AEM（如果配置了 autoInstallPackage profile）
mvn clean install -PautoInstallPackage

# 或者手动部署 bundle
# 将 core/target/*.jar 上传到 AEM 的 /system/console/bundles
```

---

## 方式 2: 作为独立工具使用

### 步骤 1: 编译为 JAR

```bash
cd "aem component info"
mvn clean package
```

### 步骤 2: 在 AEM 中运行

如果你有直接访问 AEM JCR 的方式，可以：

1. **通过 Groovy 控制台运行**：
   - 访问 `http://localhost:4502/system/console/groovyconsole`
   - 将 Java 代码转换为 Groovy 脚本运行

2. **通过 Sling Scripting 运行**：
   - 创建 `.java` 或 `.groovy` 脚本
   - 放在 `/apps/myproject/scripts/` 下

---

## 方式 3: 通过 HTTP Servlet 访问

### 步骤 1: 创建 Servlet

参考 `ComponentInfoServlet.java` 创建 HTTP Servlet。

### 步骤 2: 配置 Servlet 路径

在 `@Component` 注解中配置路径：

```java
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/componentinfo",
        "sling.servlet.methods=GET"
    }
)
```

### 步骤 3: 部署和访问

部署后访问：
```
http://localhost:4502/bin/componentinfo?path=/apps/myproject/components/mycomponent
```

---

## 方式 4: 集成到 Sling Model

### 步骤 1: 创建 Sling Model

```java
@Model(adaptables = SlingHttpServletRequest.class)
public interface ComponentInfoModel {
    
    @Inject
    ComponentInfoService componentInfoService;
    
    @Inject
    @Optional
    String componentPath;
    
    default Map<String, Object> getComponentInfo() {
        if (componentPath != null) {
            return componentInfoService.getComponentInfo(componentPath);
        }
        return null;
    }
}
```

### 步骤 2: 在 HTL 中使用

```html
<sly data-sly-use.model="com.mycompany.core.models.ComponentInfoModel">
    <div data-sly-test="${model.componentInfo}">
        <h2>${model.componentInfo.basicProperties['jcr:title']}</h2>
    </div>
</sly>
```

---

## 配置说明

### 日志配置

如果使用 Log4j2，创建 `src/main/resources/log4j2.xml`（参考提供的示例）。

如果使用 SLF4J + Logback，创建 `src/main/resources/logback.xml`。

### 权限配置

确保服务用户有足够的权限访问组件节点：

1. **创建服务用户**：
   - 在 AEM 中创建服务用户（如 `component-reader`）

2. **配置权限**：
   ```java
   Session session = repository.loginService("component-reader", null);
   ```

3. **ACL 配置**：
   - 确保服务用户对 `/apps` 和 `/libs` 有读取权限

---

## 验证部署

### 检查 Bundle 状态

1. 访问 `http://localhost:4502/system/console/bundles`
2. 查找你的 bundle
3. 确保状态为 "Active"

### 测试 OSGi 服务

1. 访问 `http://localhost:4502/system/console/services`
2. 查找你的服务
3. 验证服务已注册

### 测试 HTTP Servlet

```bash
# 测试单个组件
curl "http://localhost:4502/bin/componentinfo?path=/apps/myproject/components/mycomponent"

# 测试批量提取
curl "http://localhost:4502/bin/componentinfo/batch?basePath=/apps/myproject/components"
```

---

## 常见部署问题

### 问题 1: Bundle 无法启动

**可能原因**：
- 缺少依赖
- 包导入错误
- OSGi 配置错误

**解决方案**：
1. 检查 `pom.xml` 中的依赖
2. 检查 `META-INF/MANIFEST.MF` 中的导入包
3. 查看 AEM 错误日志

### 问题 2: 服务无法注入

**可能原因**：
- `@Reference` 注解配置错误
- 服务未正确注册
- 循环依赖

**解决方案**：
1. 检查 `@Component` 和 `@Reference` 注解
2. 确保服务接口正确
3. 检查 OSGi 服务注册

### 问题 3: 权限不足

**可能原因**：
- 服务用户权限不足
- JCR 路径访问受限

**解决方案**：
1. 使用管理会话（仅开发环境）
2. 配置适当的服务用户权限
3. 检查 ACL 配置

---

## 生产环境建议

### 1. 不要使用管理会话

```java
// ❌ 不要这样做（生产环境）
Session session = repository.loginAdministrative(null);

// ✅ 使用服务用户
Session session = repository.loginService("component-reader", null);
```

### 2. 添加错误处理

```java
try {
    // 组件提取逻辑
} catch (RepositoryException e) {
    log.error("提取组件信息失败", e);
    // 返回错误信息
} finally {
    if (session != null) {
        session.logout();
    }
}
```

### 3. 添加缓存

```java
@Component(service = ComponentInfoService.class)
public class ComponentInfoService {
    
    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();
    
    public Map<String, Object> getComponentInfo(String path) {
        return cache.computeIfAbsent(path, this::extractComponentInfo);
    }
    
    @Activate
    protected void activate() {
        // 定期清理缓存
    }
}
```

### 4. 性能监控

```java
public Map<String, Object> getComponentInfo(String path) {
    long startTime = System.currentTimeMillis();
    try {
        return extractComponentInfo(path);
    } finally {
        long duration = System.currentTimeMillis() - startTime;
        log.debug("提取组件信息耗时: {}ms", duration);
    }
}
```

---

## 部署检查清单

- [ ] 代码已复制到 AEM 项目
- [ ] 依赖已添加到 `pom.xml`
- [ ] OSGi 服务已创建并配置
- [ ] Bundle 已构建成功
- [ ] Bundle 已部署到 AEM
- [ ] Bundle 状态为 "Active"
- [ ] 服务已注册
- [ ] 权限已配置
- [ ] 日志已配置
- [ ] 功能测试通过

---

## 下一步

部署成功后：

1. 测试基本功能
2. 集成到你的工作流程
3. 根据需要扩展功能
4. 监控性能和错误

祝你部署顺利！🚀

