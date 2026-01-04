# HTL 常见问题和解决方案

## 1. 属性访问问题

### 问题: 属性未定义导致错误

**错误示例:**
```html
<div>${properties.title.value}</div>
```

**解决方案:**
```html
<!-- 使用可选链 -->
<div>${properties.title?.value || '默认值'}</div>

<!-- 或先检查 -->
<div data-sly-test="${properties.title}">
    ${properties.title.value}
</div>
```

## 2. 循环问题

### 问题: 空列表导致错误

**错误示例:**
```html
<ul>
    <li data-sly-list="${properties.items}">
        ${item.name}
    </li>
</ul>
```

**解决方案:**
```html
<ul data-sly-test="${properties.items && properties.items.size > 0}">
    <li data-sly-list="${properties.items}">
        ${item.name}
    </li>
</ul>
<div data-sly-test="${!properties.items || properties.items.size == 0}">
    暂无项目
</div>
```

### 问题: 循环中的空值

**解决方案:**
```html
<ul>
    <li data-sly-list="${properties.items}"
        data-sly-test="${item}">
        ${item.name || '未命名'}
    </li>
</ul>
```

## 3. 字符串拼接问题

### 问题: 类名拼接错误

**错误示例:**
```html
<div class="${'base ' + properties.class}"></div>
```

**解决方案:**
```html
<!-- 方法 1: 使用数组 -->
<sly data-sly-set.classes="${['base', properties.class].filter(Boolean).join(' ')}"></sly>
<div class="${classes}"></div>

<!-- 方法 2: 使用三元运算符 -->
<div class="base ${properties.class || ''}"></div>
```

## 4. 条件渲染问题

### 问题: 条件表达式不正确

**错误示例:**
```html
<div data-sly-test="${properties.isActive}">内容</div>
<!-- 如果 isActive 是字符串 "false"，仍然会渲染 -->
```

**解决方案:**
```html
<div data-sly-test="${properties.isActive == true}">内容</div>
<!-- 或 -->
<div data-sly-test="${properties.isActive && properties.isActive != 'false'}">内容</div>
```

## 5. Use API 问题

### 问题: Use API 对象为 null

**错误示例:**
```html
<sly data-sly-use.model="${'com.example.Model'}"></sly>
<div>${model.value}</div>
```

**解决方案:**
```html
<sly data-sly-use.model="${'com.example.Model'}"></sly>
<div data-sly-test="${model}">
    ${model.value}
</div>
<div data-sly-test="${!model}">
    <p>模型初始化失败</p>
</div>
```

### 问题: 参数传递错误

**解决方案:**
```html
<!-- 确保参数类型正确 -->
<sly data-sly-use.model="${'com.example.Model' @ 
    stringParam=properties.text,
    numberParam=parseInt(properties.number),
    booleanParam=properties.flag == 'true'}"></sly>
```

## 6. 资源包含问题

### 问题: 资源不存在

**错误示例:**
```html
<div data-sly-resource="${'child' @ resourceType='myapp/components/child'}"></div>
```

**解决方案:**
```html
<div data-sly-test="${resource.getChild('child')}">
    <div data-sly-resource="${'child' @ resourceType='myapp/components/child'}"></div>
</div>
```

## 7. 客户端库问题

### 问题: 客户端库未加载

**错误示例:**
```html
<sly data-sly-call="${clientlib.css @ categories='myapp.components'}"></sly>
```

**解决方案:**
```html
<!-- 确保先引入 clientlib 模板 -->
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components'}"></sly>

<!-- 检查类别名称是否正确 -->
<!-- 检查客户端库配置是否正确 -->
```

## 8. 国际化问题

### 问题: 翻译键不存在

**错误示例:**
```html
<sly data-sly-use.i18n="${'com.day.cq.i18n.I18n' @ request=request}"></sly>
<p>${i18n.get('nonexistent.key')}</p>
```

**解决方案:**
```html
<sly data-sly-use.i18n="${'com.day.cq.i18n.I18n' @ request=request}"></sly>
<p>${i18n.get('my.key', '默认文本')}</p>
```

## 9. 性能问题

### 问题: 在循环中进行复杂计算

**错误示例:**
```html
<ul>
    <li data-sly-list="${properties.items}">
        ${model.complexCalculation(item)}
    </li>
</ul>
```

**解决方案:**
```html
<!-- 在 Use API 中预处理数据 -->
<sly data-sly-use.model="${'com.example.Model' @ items=properties.items}"></sly>
<ul>
    <li data-sly-list="${model.processedItems}">
        ${item.result}
    </li>
</ul>
```

## 10. 转义问题

### 问题: HTML 被转义

**错误示例:**
```html
<div>${properties.htmlContent}</div>
<!-- HTML 标签被转义为文本 -->
```

**解决方案:**
```html
<!-- 谨慎使用，确保内容安全 -->
<div data-sly-unescape="${properties.htmlContent}"></div>

<!-- 更好的方式: 使用 Use API 处理 -->
<sly data-sly-use.processor="${'com.example.HtmlProcessor' @ content=properties.htmlContent}"></sly>
<div>${processor.safeHtml}</div>
```

## 11. 变量作用域问题

### 问题: 变量在作用域外使用

**错误示例:**
```html
<div data-sly-set.var="${'value'}">
    ${var}
</div>
<div>${var}</div> <!-- 错误: var 不在作用域内 -->
```

**解决方案:**
```html
<sly data-sly-set.var="${'value'}"></sly>
<div>${var}</div>
<div>${var}</div> <!-- 正确: var 在作用域内 -->
```

## 12. 日期格式化问题

### 问题: 日期显示格式不正确

**解决方案:**
```html
<sly data-sly-use.i18n="${'com.day.cq.i18n.I18n' @ request=request}"></sly>
<sly data-sly-use.locale="${'java.util.Locale' @ language=currentPage.language}"></sly>
<p>${i18n.getDate(properties.date, locale)}</p>
```

## 调试技巧

1. **启用开发者模式**: 在 AEM 中启用开发者模式查看详细信息
2. **使用临时调试输出**: 输出变量值查看实际内容
3. **检查浏览器控制台**: 查看 JavaScript 错误
4. **查看 AEM 日志**: 检查服务器端错误
5. **使用 Sling Model 日志**: 在 Model 中使用 Logger
6. **验证属性路径**: 确保属性路径正确
7. **检查资源类型**: 确保组件资源类型正确

## 预防措施

1. **始终检查空值**: 使用可选链或条件检查
2. **验证数据**: 在 Use API 中验证数据
3. **使用类型安全**: 使用 Sling Models 而不是直接访问
4. **错误处理**: 在 Use API 中处理异常
5. **测试**: 编写单元测试和集成测试
6. **代码审查**: 进行代码审查发现潜在问题

