# HTL 组件对话框开发指南

## 对话框概述

组件对话框是内容作者配置组件属性的界面。虽然对话框主要是 XML 配置，但了解如何在 HTL 中使用对话框配置的属性很重要。

## 对话框类型

### 1. 编辑对话框 (_cq_dialog)

用于编辑组件内容的主要对话框。

### 2. 设计对话框 (_cq_design_dialog)

用于设计时配置，影响组件的样式和行为。

## 在 HTL 中访问对话框属性

### 基础属性访问

```html
<!-- 访问编辑对话框中的属性 -->
<div>${properties.title}</div>
<div>${properties.description}</div>
```

### 嵌套属性访问

```html
<!-- 访问嵌套属性 -->
<div>${properties.author.name}</div>
<div>${properties.config.theme}</div>
```

### 多值属性

```html
<!-- 访问多值属性（数组） -->
<ul>
    <li data-sly-list="${properties.tags}">${item}</li>
</ul>
```

## 对话框字段类型

### 文本字段

```html
<!-- 文本输入 -->
<div>${properties.textField}</div>
```

### 富文本编辑器

```html
<!-- 富文本内容 -->
<div data-sly-unescape="${properties.richText}"></div>
```

### 路径字段

```html
<!-- 路径选择器 -->
<a href="${properties.linkPath}.html">${properties.linkText}</a>
```

### 图像字段

```html
<!-- 图像选择器 -->
<img src="${properties.imagePath}" alt="${properties.altText}">
```

### 选择字段

```html
<!-- 下拉选择 -->
<div class="component ${properties.theme}">${properties.content}</div>
```

### 复选框

```html
<!-- 布尔值 -->
<div data-sly-test="${properties.isActive}">活跃内容</div>
```

### 多字段

```html
<!-- 多字段（键值对） -->
<dl>
    <dt data-sly-list="${properties.metadata}">
        ${item.key}:
        <dd>${item.value}</dd>
    </dt>
</dl>
```

## 实际应用示例

### 示例 1: 卡片组件对话框

**对话框配置要点:**
- title: 文本字段
- description: 文本区域
- imagePath: 图像字段
- linkUrl: 路径字段
- cardType: 选择字段（default, featured, compact）
- showMeta: 复选框

**HTL 使用:**
```html
<article class="card ${properties.cardType}">
    <h2>${properties.title}</h2>
    <p>${properties.description}</p>
    <img src="${properties.imagePath}" alt="${properties.title}">
    <a href="${properties.linkUrl}">了解更多</a>
    <div data-sly-test="${properties.showMeta}">
        <!-- 元数据 -->
    </div>
</article>
```

### 示例 2: 列表组件对话框

**对话框配置要点:**
- rootPath: 路径字段
- itemCount: 数字字段
- displayType: 选择字段（list, grid, cards）
- showPagination: 复选框

**HTL 使用:**
```html
<sly data-sly-use.list="${'com.example.ListModel' @ 
    rootPath=properties.rootPath,
    itemCount=properties.itemCount}"></sly>
<div class="list-component ${properties.displayType}">
    <div data-sly-list="${list.items}">
        ${item.content}
    </div>
    <div data-sly-test="${properties.showPagination}">
        <!-- 分页 -->
    </div>
</div>
```

### 示例 3: 表单组件对话框

**对话框配置要点:**
- formFields: 多字段
- submitUrl: 路径字段
- submitLabel: 文本字段
- validation: 选择字段

**HTL 使用:**
```html
<form action="${properties.submitUrl}" method="post">
    <div data-sly-list="${properties.formFields}">
        <label>${item.label}</label>
        <input type="${item.type}" 
               name="${item.name}"
               data-sly-attribute.required="${item.required}">
    </div>
    <button type="submit">${properties.submitLabel}</button>
</form>
```

## 对话框验证

### 在 HTL 中验证

```html
<sly data-sly-set.isValid="${properties.title && properties.title.length > 0}"></sly>
<div data-sly-test="${isValid}">
    ${properties.title}
</div>
<div data-sly-test="${!isValid}" class="error">
    请提供有效的标题
</div>
```

### 在 Sling Model 中验证

```java
@Model(adaptables = Resource.class)
public interface ValidatedModel {
    @Inject
    String getTitle();
    
    default boolean isValid() {
        return getTitle() != null && !getTitle().isEmpty();
    }
    
    default String getErrorMessage() {
        return isValid() ? null : "请提供有效的标题";
    }
}
```

## 对话框最佳实践

1. **使用有意义的字段名**: 字段名应该清晰描述其用途
2. **提供默认值**: 在 HTL 中使用默认值处理空属性
3. **验证输入**: 在 HTL 和 Sling Model 中验证数据
4. **组织字段**: 使用标签和字段集组织对话框
5. **提供帮助文本**: 帮助内容作者理解字段用途
6. **使用适当的字段类型**: 选择合适的字段类型
7. **处理多值**: 正确处理数组和集合属性

## 常见对话框模式

### 模式 1: 简单内容组件

```html
<div class="component">
    <h2>${properties.title || '默认标题'}</h2>
    <div>${properties.content}</div>
</div>
```

### 模式 2: 配置驱动组件

```html
<sly data-sly-set.config="${{
    'theme': properties.theme || 'default',
    'layout': properties.layout || 'standard',
    'showTitle': properties.showTitle != null ? properties.showTitle : true
}}"></sly>
<div class="component theme-${config.theme} layout-${config.layout}">
    <h2 data-sly-test="${config.showTitle}">${properties.title}</h2>
    <div>${properties.content}</div>
</div>
```

### 模式 3: 条件字段

```html
<div class="component">
    <div data-sly-test="${properties.showImage && properties.imagePath}">
        <img src="${properties.imagePath}" alt="${properties.altText}">
    </div>
    <div>${properties.content}</div>
    <div data-sly-test="${properties.showLink && properties.linkUrl}">
        <a href="${properties.linkUrl}">${properties.linkText}</a>
    </div>
</div>
```

## 对话框配置参考

虽然对话框主要是 XML 配置，但了解以下字段类型很有用：

- **textfield**: 单行文本输入
- **textarea**: 多行文本输入
- **richtext**: 富文本编辑器
- **pathfield**: 路径选择器
- **imagefield**: 图像选择器
- **select**: 下拉选择
- **checkbox**: 复选框
- **multifield**: 多字段（重复字段）
- **numberfield**: 数字输入
- **datepicker**: 日期选择器

## 总结

1. **理解对话框结构**: 了解对话框如何映射到 properties
2. **使用默认值**: 在 HTL 中提供合理的默认值
3. **验证数据**: 验证对话框输入
4. **组织代码**: 使用 Sling Models 处理复杂逻辑
5. **文档化**: 文档化组件接受的属性

