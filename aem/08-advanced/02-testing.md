# AEM 高级主题：第二部分 - 测试策略

## 📖 为什么需要测试?

测试是确保代码质量和稳定性的关键。AEM 开发中，我们需要测试：
- **单元测试**：测试单个类和方法
- **集成测试**：测试组件之间的交互
- **UI 测试**：测试用户界面

## 🧪 单元测试

### 示例 1：Sling Model 单元测试

```java
package com.example.core.models.impl;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sling Model 单元测试示例
 * 
 * 使用 wcm.io Testing 框架进行 AEM 模拟测试
 */
@ExtendWith(AemContextExtension.class)
class ArticleModelImplTest {

    private final AemContext context = new AemContext();

    private ArticleModelImpl model;

    @BeforeEach
    void setUp() {
        // 创建测试资源
        context.create().resource("/content/test/article",
            "jcr:title", "测试标题",
            "jcr:description", "测试描述",
            "author", "测试作者",
            "featured", true
        );

        // 获取资源并适配为 Model
        model = context.resourceResolver()
            .getResource("/content/test/article")
            .adaptTo(ArticleModelImpl.class);
    }

    @Test
    void testGetTitle() {
        // 测试获取标题
        assertEquals("测试标题", model.getTitle());
    }

    @Test
    void testGetDescription() {
        // 测试获取描述
        assertEquals("测试描述", model.getDescription());
    }

    @Test
    void testGetAuthor() {
        // 测试获取作者
        assertEquals("测试作者", model.getAuthor());
    }

    @Test
    void testIsFeatured() {
        // 测试推荐标记
        assertTrue(model.isFeatured());
    }

    @Test
    void testEmptyModel() {
        // 测试空模型
        context.create().resource("/content/test/empty");
        ArticleModelImpl emptyModel = context.resourceResolver()
            .getResource("/content/test/empty")
            .adaptTo(ArticleModelImpl.class);
        
        assertNotNull(emptyModel);
        assertNull(emptyModel.getTitle());
    }
}
```

### 示例 2：OSGi 服务单元测试

```java
package com.example.core.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * OSGi 服务单元测试示例
 * 
 * 使用 Mockito 进行模拟测试
 */
@ExtendWith(MockitoExtension.class)
class DataServiceImplTest {

    @Mock
    private StorageService storageService;

    private DataServiceImpl dataService;

    @BeforeEach
    void setUp() {
        dataService = new DataServiceImpl();
        // 注入模拟的依赖服务
        // 注意：实际实现中需要通过反射或设置器注入
    }

    @Test
    void testGetData() {
        // 模拟依赖服务的返回值
        List<String> mockData = Arrays.asList("data1", "data2", "data3");
        when(storageService.read()).thenReturn(mockData);

        // 测试获取数据
        List<String> result = dataService.getData();

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("data1", result.get(0));

        // 验证依赖服务被调用
        verify(storageService, times(1)).read();
    }

    @Test
    void testSaveData() {
        // 测试保存数据
        dataService.saveData("test data");

        // 验证依赖服务被调用
        verify(storageService, times(1)).write("test data");
    }

    @Test
    void testGetDataWhenServiceUnavailable() {
        // 测试服务不可用的情况
        when(storageService.read()).thenReturn(null);

        List<String> result = dataService.getData();

        // 应该返回空列表或处理错误
        assertNotNull(result);
    }
}
```

### 示例 3：工具类单元测试

```java
package com.example.core.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 工具类单元测试示例
 */
class StringUtilsTest {

    @Test
    void testIsEmpty() {
        // 测试空字符串
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertTrue(StringUtils.isEmpty("   "));
        assertFalse(StringUtils.isEmpty("text"));
    }

    @Test
    void testTruncate() {
        // 测试字符串截断
        assertEquals("Hello", StringUtils.truncate("Hello World", 5));
        assertEquals("Hello...", StringUtils.truncate("Hello World", 8, true));
        
        // 测试边界情况
        assertEquals("", StringUtils.truncate("Hello", 0));
        assertEquals("Hello", StringUtils.truncate("Hello", 10));
    }
}
```

## 🔗 集成测试

### 示例 4：Sling Servlet 集成测试

```java
package com.example.core.servlets;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sling Servlet 集成测试
 */
@ExtendWith(AemContextExtension.class)
class ArticlesApiServletTest {

    private final AemContext context = new AemContext();

    private ArticlesApiServlet servlet;

    @BeforeEach
    void setUp() {
        // 注册 Servlet
        servlet = context.registerService(new ArticlesApiServlet());
        context.registerInjectActivateService(servlet);
    }

    @Test
    void testDoGet() throws ServletException, IOException {
        // 创建请求
        MockSlingHttpServletRequest request = context.request();
        request.setResource(context.resourceResolver().getResource("/content"));
        request.addRequestParameter("limit", "10");

        // 创建响应
        MockSlingHttpServletResponse response = context.response();

        // 执行 Servlet
        servlet.doGet(request, response);

        // 验证响应
        assertEquals("application/json", response.getContentType());
        assertNotNull(response.getOutputAsString());
        
        // 验证 JSON 内容
        String json = response.getOutputAsString();
        assertTrue(json.contains("\"status\""));
        assertTrue(json.contains("\"success\""));
    }
}
```

## 🎨 HTL 模板测试

### 示例 5：HTL 模板渲染测试

```java
package com.example.core;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTL 模板测试
 */
@ExtendWith(AemContextExtension.class)
class ArticleComponentTest {

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @BeforeEach
    void setUp() {
        // 加载测试内容
        context.load().json("/test-content/content.json", "/content");
        
        // 加载客户端库
        context.addModelsForPackage("com.example.core.models");
    }

    @Test
    void testArticleComponentRendering() {
        // 获取资源
        context.currentResource("/content/myproject/en/home/jcr:content/par/article");

        // 获取渲染结果（使用 Sling 渲染）
        String html = context.response().getOutputAsString();

        // 验证 HTML 内容
        assertNotNull(html);
        assertTrue(html.contains("article-component"));
        assertTrue(html.contains("测试标题"));
    }
}
```

## 📋 测试最佳实践

### 1. 测试结构（AAA 模式）

```java
@Test
void testMethodName() {
    // Arrange: 准备测试数据和环境
    String input = "test";
    
    // Act: 执行被测试的方法
    String result = methodUnderTest(input);
    
    // Assert: 验证结果
    assertEquals("expected", result);
}
```

### 2. 测试命名约定

```java
// 格式：test[MethodName]_[Scenario]_[ExpectedResult]
@Test
void testGetTitle_WhenTitleExists_ReturnsTitle() { }

@Test
void testGetTitle_WhenTitleIsNull_ReturnsEmptyString() { }
```

### 3. 测试覆盖率

- 目标是达到 70-80% 的代码覆盖率
- 重点测试核心业务逻辑
- 不要过度测试简单的 getter/setter

### 4. Mock 和 Stub

```java
// 使用 Mock 模拟依赖
@Mock
private Service service;

// 使用 Stub 提供测试数据
when(service.getData()).thenReturn(testData);
```

## 🔧 测试工具和框架

### Maven 依赖

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.9.2</version>
        <scope>test</scope>
    </dependency>
    
    <!-- wcm.io Testing (AEM Mock) -->
    <dependency>
        <groupId>io.wcm</groupId>
        <artifactId>io.wcm.testing.aem-mock.junit5</artifactId>
        <version>4.0.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.1.1</version>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ (更流畅的断言) -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.24.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 🔑 关键要点

1. **单元测试**：测试独立的类和方法
2. **集成测试**：测试组件之间的交互
3. **Mock 框架**：使用 Mockito 模拟依赖
4. **AEM Mock**：使用 wcm.io Testing 模拟 AEM 环境
5. **测试覆盖率**：保持合理的测试覆盖率

## ➡️ 下一步

学习更多测试技术，如性能测试、UI 自动化测试等。

