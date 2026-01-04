# 从 JSP 迁移到 HTL 指南

## 迁移概述

本指南帮助你从 JSP (JavaServer Pages) 迁移到 HTL (HTML Template Language)。

## 主要差异

### 1. 语法差异

#### JSP
```jsp
<h1><%= properties.get("title", "默认标题") %></h1>
```

#### HTL
```html
<h1>${properties.title || '默认标题'}</h1>
```

### 2. 条件渲染

#### JSP
```jsp
<% if (properties.get("isActive", false)) { %>
    <div>活跃内容</div>
<% } %>
```

#### HTL
```html
<div data-sly-test="${properties.isActive}">
    活跃内容
</div>
```

### 3. 循环

#### JSP
```jsp
<ul>
    <% for (String item : items) { %>
        <li><%= item %></li>
    <% } %>
</ul>
```

#### HTL
```html
<ul>
    <li data-sly-list="${items}">${item}</li>
</ul>
```

### 4. 包含其他组件

#### JSP
```jsp
<sling:include path="child" resourceType="myapp/components/child"/>
```

#### HTL
```html
<div data-sly-resource="${'child' @ resourceType='myapp/components/child'}"></div>
```

## 迁移步骤

### 步骤 1: 分析现有 JSP 代码

1. 识别所有 JSP 脚本
2. 识别业务逻辑
3. 识别数据访问模式
4. 识别包含和转发

### 步骤 2: 提取业务逻辑

将业务逻辑从 JSP 移到 Sling Models:

#### JSP (之前)
```jsp
<%
    String title = properties.get("title", "");
    String formattedTitle = title.toUpperCase();
%>
<h1><%= formattedTitle %></h1>
```

#### HTL + Sling Model (之后)

**HTL:**
```html
<sly data-sly-use.model="${'com.example.MyModel'}"></sly>
<h1>${model.formattedTitle}</h1>
```

**Sling Model:**
```java
@Model(adaptables = Resource.class)
public interface MyModel {
    @Inject
    String getTitle();
    
    default String getFormattedTitle() {
        return getTitle() != null ? getTitle().toUpperCase() : "";
    }
}
```

### 步骤 3: 转换表达式

#### JSP 表达式 → HTL 表达式

| JSP | HTL |
|-----|-----|
| `<%= variable %>` | `${variable}` |
| `<%= obj.getProperty() %>` | `${obj.property}` |
| `<%= obj.get("key") %>` | `${obj.key}` |
| `<%= obj != null ? obj.getValue() : "default" %>` | `${obj?.value || 'default'}` |

### 步骤 4: 转换条件语句

#### JSP
```jsp
<% if (condition) { %>
    <div>内容</div>
<% } else { %>
    <div>其他内容</div>
<% } %>
```

#### HTL
```html
<div data-sly-test="${condition}">
    内容
</div>
<div data-sly-test="${!condition}">
    其他内容
</div>
```

### 步骤 5: 转换循环

#### JSP
```jsp
<ul>
    <% 
    List<String> items = (List<String>) properties.get("items", Collections.emptyList());
    for (int i = 0; i < items.size(); i++) {
        String item = items.get(i);
    %>
        <li><%= item %></li>
    <% } %>
</ul>
```

#### HTL
```html
<ul>
    <li data-sly-list="${properties.items}">${item}</li>
</ul>
```

### 步骤 6: 转换包含

#### JSP
```jsp
<sling:include path="child" resourceType="myapp/components/child"/>
```

#### HTL
```html
<div data-sly-resource="${'child' @ resourceType='myapp/components/child'}"></div>
```

## 常见迁移模式

### 模式 1: 使用 Use API 替代 Scriptlet

#### JSP
```jsp
<%
    UseBean bean = new UseBean();
    bean.init(resource, request, response);
    String value = bean.getValue();
%>
<div><%= value %></div>
```

#### HTL
```html
<sly data-sly-use.bean="${'com.example.UseBean'}"></sly>
<div>${bean.value}</div>
```

### 模式 2: 使用 Sling Models

#### JSP
```jsp
<%
    ResourceResolver resolver = resource.getResourceResolver();
    PageManager pageManager = resolver.adaptTo(PageManager.class);
    Page currentPage = pageManager.getContainingPage(resource);
%>
<h1><%= currentPage.getTitle() %></h1>
```

#### HTL
```html
<sly data-sly-use.page="${'com.example.PageModel'}"></sly>
<h1>${page.title}</h1>
```

### 模式 3: 属性访问

#### JSP
```jsp
<%
    String title = properties.get("title", "");
    String subtitle = properties.get("subtitle", "");
%>
<h1><%= title %></h1>
<h2><%= subtitle %></h2>
```

#### HTL
```html
<h1>${properties.title || ''}</h1>
<h2>${properties.subtitle || ''}</h2>
```

## 迁移检查清单

- [ ] 所有 JSP 脚本已转换为 HTL
- [ ] 业务逻辑已移到 Sling Models
- [ ] 所有表达式已转换
- [ ] 条件语句已转换
- [ ] 循环已转换
- [ ] 包含已转换
- [ ] 错误处理已实现
- [ ] 空值检查已添加
- [ ] 测试已更新
- [ ] 文档已更新

## 迁移工具

1. **手动迁移**: 逐步转换每个组件
2. **代码审查**: 确保转换正确
3. **测试**: 验证功能一致性
4. **性能测试**: 确保性能不下降

## 最佳实践

1. **逐步迁移**: 不要一次性迁移所有代码
2. **保持功能一致**: 确保迁移后功能相同
3. **使用 Sling Models**: 将业务逻辑移到 Models
4. **测试**: 充分测试迁移后的代码
5. **文档**: 更新相关文档

## 常见问题

### Q: JSP 中的复杂逻辑如何处理？
A: 移到 Sling Models 中处理。

### Q: 如何迁移自定义标签库？
A: 使用 HTL Use API 或 Sling Models 替代。

### Q: JSP 中的 Java 代码如何处理？
A: 移到 Sling Models 或 Use API 类中。

### Q: 性能会提升吗？
A: HTL 通常比 JSP 性能更好，因为更简洁且优化更好。

## 迁移示例

查看 `05-complete-project/` 目录中的完整组件示例，了解 HTL 的最佳实践。

