# HTL 组件测试策略

## 测试概述

测试是确保 HTL 组件质量和可靠性的关键。本指南介绍 HTL 组件的测试策略和方法。

## 测试类型

### 1. 单元测试 (Unit Testing)

#### Sling Model 测试

```java
@Test
public void testCardModel() {
    // 创建模拟资源
    Resource resource = Mockito.mock(Resource.class);
    ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
    valueMap.put("title", "测试标题");
    valueMap.put("content", "测试内容");
    
    when(resource.getValueMap()).thenReturn(valueMap);
    when(resource.adaptTo(ValueMap.class)).thenReturn(valueMap);
    
    // 创建 Model
    CardModel model = resource.adaptTo(CardModel.class);
    
    // 断言
    assertNotNull(model);
    assertEquals("测试标题", model.getTitle());
    assertEquals("测试内容", model.getContent());
}
```

#### Use API 测试

```java
@Test
public void testUseBean() {
    UseBean bean = new UseBean();
    bean.init(resource, request, response);
    
    String result = bean.getValue();
    assertNotNull(result);
    assertEquals("预期值", result);
}
```

### 2. 集成测试 (Integration Tests)

#### 组件渲染测试

```java
@Test
public void testComponentRendering() throws Exception {
    // 创建测试内容
    Resource componentResource = createComponentResource();
    
    // 渲染组件
    String html = renderComponent(componentResource);
    
    // 验证输出
    assertTrue(html.contains("预期内容"));
    assertTrue(html.contains("class=\"component\""));
}
```

#### 资源包含测试

```java
@Test
public void testResourceInclude() throws Exception {
    Resource parentResource = createParentResource();
    Resource childResource = createChildResource();
    
    // 测试资源包含
    String html = renderWithInclude(parentResource, childResource);
    
    // 验证子组件被包含
    assertTrue(html.contains("子组件内容"));
}
```

### 3. 功能测试 (Functional Tests)

#### 浏览器自动化测试

```java
@Test
public void testComponentInBrowser() {
    WebDriver driver = new ChromeDriver();
    driver.get("http://localhost:4502/content/test-page.html");
    
    // 验证组件存在
    WebElement component = driver.findElement(By.className("component"));
    assertNotNull(component);
    
    // 验证组件内容
    WebElement title = component.findElement(By.tagName("h1"));
    assertEquals("预期标题", title.getText());
    
    driver.quit();
}
```

### 4. 可访问性测试 (Accessibility Testing)

#### 使用 axe-core

```javascript
// 在浏览器控制台中运行
axe.run(document, (err, results) => {
    if (err) throw err;
    console.log(results.violations);
});
```

#### 使用 WAVE API

```java
@Test
public void testAccessibility() {
    String html = renderComponent(componentResource);
    
    // 使用 WAVE API 检查可访问性
    WaveReport report = waveAPI.analyze(html);
    
    assertEquals(0, report.getErrors().size());
    assertTrue(report.getWarnings().size() < 5);
}
```

## 测试工具

### 1. JUnit

用于 Java 单元测试和集成测试。

### 2. Mockito

用于模拟依赖对象。

### 3. Sling Mocks

用于模拟 Sling 框架对象。

```java
@ExtendWith(SlingContextExtension.class)
public class ComponentTest {
    @RegisterExtension
    public final SlingContext context = new SlingContext();
    
    @Test
    public void testComponent() {
        context.create().resource("/content/test");
        // 测试代码
    }
}
```

### 4. Selenium/WebDriver

用于浏览器自动化测试。

### 5. AEM Testing Tools

- AEM Test Framework
- AEM Component Testing Tools

## 测试最佳实践

### 1. 测试覆盖

- 覆盖所有主要功能
- 测试边界条件
- 测试错误情况
- 测试空值处理

### 2. 测试数据

```java
public class TestDataBuilder {
    public static ValueMap createComponentProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("title", "测试标题");
        props.put("content", "测试内容");
        props.put("isActive", true);
        return new ValueMapDecorator(props);
    }
}
```

### 3. 测试隔离

每个测试应该独立运行，不依赖其他测试。

### 4. 测试命名

使用描述性的测试方法名称。

```java
@Test
public void testCardModel_WithValidTitle_ReturnsTitle() {
    // 测试代码
}

@Test
public void testCardModel_WithNullTitle_ReturnsDefault() {
    // 测试代码
}
```

### 5. 断言清晰

使用清晰的断言消息。

```java
assertEquals("标题应该匹配", expectedTitle, model.getTitle());
```

## 测试示例

### 完整组件测试示例

```java
@ExtendWith(SlingContextExtension.class)
public class CardComponentTest {
    
    @RegisterExtension
    public final SlingContext context = new SlingContext();
    
    @Test
    public void testCardComponent_WithAllProperties_RendersCorrectly() {
        // 准备测试数据
        Resource componentResource = context.create().resource("/content/card",
            ImmutableMap.<String, Object>builder()
                .put("title", "测试标题")
                .put("content", "测试内容")
                .put("imagePath", "/content/dam/test.jpg")
                .build());
        
        // 创建 Model
        CardModel model = componentResource.adaptTo(CardModel.class);
        
        // 断言
        assertNotNull(model);
        assertEquals("测试标题", model.getTitle());
        assertEquals("测试内容", model.getContent());
        assertTrue(model.hasImage());
    }
    
    @Test
    public void testCardComponent_WithMissingProperties_HandlesGracefully() {
        Resource componentResource = context.create().resource("/content/card");
        
        CardModel model = componentResource.adaptTo(CardModel.class);
        
        assertNotNull(model);
        assertNull(model.getTitle());
        assertFalse(model.isConfigured());
    }
}
```

## 测试检查清单

- [ ] 所有 Sling Models 有单元测试
- [ ] 所有 Use API 类有单元测试
- [ ] 组件渲染有集成测试
- [ ] 资源包含有测试
- [ ] 错误情况有测试
- [ ] 边界条件有测试
- [ ] 可访问性有测试
- [ ] 性能有测试
- [ ] 测试覆盖率达到 80% 以上
- [ ] 所有测试通过

## 持续集成

### CI/CD 集成

```yaml
# .github/workflows/test.yml
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run tests
        run: mvn test
```

## 测试报告

使用工具生成测试报告：
- JaCoCo (代码覆盖率)
- Allure (测试报告)
- Surefire Reports (Maven)

## 总结

1. **编写测试**: 为所有组件编写测试
2. **自动化**: 使用 CI/CD 自动运行测试
3. **持续改进**: 根据测试结果改进代码
4. **文档化**: 记录测试策略和结果

