# HTL 常见陷阱和错误

## 概述

本指南列出 HTL 开发中常见的陷阱和错误，帮助你避免这些问题。

## 1. 表达式陷阱

### 陷阱 1: 空值访问

**错误:**
```html
<div>${properties.nested.value}</div>
<!-- 如果 properties.nested 为 null，会报错 -->
```

**正确:**
```html
<div>${properties.nested?.value || '默认值'}</div>
<!-- 或 -->
<div data-sly-test="${properties.nested}">
    ${properties.nested.value}
</div>
```

### 陷阱 2: 字符串比较

**错误:**
```html
<div data-sly-test="${properties.status == 'active'}">活跃</div>
<!-- 如果 properties.status 是字符串 "Active"（大写），不会匹配 -->
```

**正确:**
```html
<div data-sly-test="${properties.status && properties.status.toLowerCase() == 'active'}">
    活跃
</div>
<!-- 或使用 Sling Model 处理 -->
```

### 陷阱 3: 布尔值判断

**错误:**
```html
<div data-sly-test="${properties.isActive}">活跃</div>
<!-- 如果 properties.isActive 是字符串 "false"，仍然会渲染 -->
```

**正确:**
```html
<div data-sly-test="${properties.isActive == true}">活跃</div>
<!-- 或 -->
<div data-sly-test="${properties.isActive && properties.isActive != 'false'}">
    活跃
</div>
```

## 2. 循环陷阱

### 陷阱 4: 空列表访问

**错误:**
```html
<ul>
    <li data-sly-list="${properties.items}">${item}</li>
</ul>
<!-- 如果 properties.items 为 null，会报错 -->
```

**正确:**
```html
<ul data-sly-test="${properties.items && properties.items.size > 0}">
    <li data-sly-list="${properties.items}">${item}</li>
</ul>
<div data-sly-test="${!properties.items || properties.items.size == 0}">
    暂无项目
</div>
```

### 陷阱 5: 循环中的变量作用域

**错误:**
```html
<div data-sly-set.total="${0}"></div>
<ul>
    <li data-sly-list="${properties.items}">
        <sly data-sly-set.total="${total + 1}"></sly>
        ${item}
    </li>
</ul>
<div>总计: ${total}</div>
<!-- total 在循环内部的变化不会影响外部 -->
```

**正确:**
```html
<sly data-sly-use.calculator="${'com.example.Calculator' @ items=properties.items}"></sly>
<ul>
    <li data-sly-list="${properties.items}">${item}</li>
</ul>
<div>总计: ${calculator.total}</div>
```

## 3. 条件渲染陷阱

### 陷阱 6: 条件表达式优先级

**错误:**
```html
<div data-sly-test="${properties.isActive || properties.isFeatured && properties.isVisible}">
    内容
</div>
<!-- 运算符优先级可能导致意外结果 -->
```

**正确:**
```html
<div data-sly-test="${properties.isActive || (properties.isFeatured && properties.isVisible)}">
    内容
</div>
<!-- 或使用变量 -->
<sly data-sly-set.shouldShow="${properties.isActive || (properties.isFeatured && properties.isVisible)}"></sly>
<div data-sly-test="${shouldShow}">内容</div>
```

### 陷阱 7: 嵌套条件

**错误:**
```html
<div data-sly-test="${properties.isActive}">
    <div data-sly-test="${properties.isVisible}">
        <div data-sly-test="${properties.isPublished}">
            内容
        </div>
    </div>
</div>
<!-- 过度嵌套，难以维护 -->
```

**正确:**
```html
<sly data-sly-set.shouldRender="${properties.isActive && properties.isVisible && properties.isPublished}"></sly>
<div data-sly-test="${shouldRender}">
    内容
</div>
```

## 4. 属性操作陷阱

### 陷阱 8: 类名拼接

**错误:**
```html
<div class="${'base-class ' + properties.cssClass}"></div>
<!-- 如果 properties.cssClass 为空，会有多余空格 -->
```

**正确:**
```html
<sly data-sly-set.classes="${['base-class', properties.cssClass].filter(Boolean).join(' ')}"></sly>
<div class="${classes}"></div>
```

### 陷阱 9: 属性覆盖

**错误:**
```html
<div class="static-class" 
     data-sly-attribute.class="${properties.cssClass}">
    内容
</div>
<!-- static-class 会被覆盖 -->
```

**正确:**
```html
<div data-sly-attribute.class="${'static-class ' + (properties.cssClass || '')}">
    内容
</div>
```

## 5. Use API 陷阱

### 陷阱 10: Use API 对象为 null

**错误:**
```html
<sly data-sly-use.model="${'com.example.Model'}"></sly>
<div>${model.value}</div>
<!-- 如果 Model 初始化失败，model 可能为 null -->
```

**正确:**
```html
<sly data-sly-use.model="${'com.example.Model'}"></sly>
<div data-sly-test="${model}">
    ${model.value}
</div>
<div data-sly-test="${!model}" class="error">
    模型初始化失败
</div>
```

### 陷阱 11: 参数类型错误

**错误:**
```html
<sly data-sly-use.model="${'com.example.Model' @ limit=properties.limit}"></sly>
<!-- 如果 properties.limit 是字符串 "10"，而不是数字 10 -->
```

**正确:**
```html
<sly data-sly-use.model="${'com.example.Model' @ limit=parseInt(properties.limit) || 10}"></sly>
<!-- 或让 Model 处理类型转换 -->
```

## 6. 资源包含陷阱

### 陷阱 12: 资源不存在

**错误:**
```html
<div data-sly-resource="${'child' @ resourceType='myapp/components/child'}"></div>
<!-- 如果 child 资源不存在，可能报错 -->
```

**正确:**
```html
<div data-sly-test="${resource.getChild('child')}">
    <div data-sly-resource="${'child' @ resourceType='myapp/components/child'}"></div>
</div>
```

### 陷阱 13: 循环依赖

**错误:**
```html
<!-- component-a.html -->
<div data-sly-resource="${'b' @ resourceType='myapp/components/b'}"></div>

<!-- component-b.html -->
<div data-sly-resource="${'a' @ resourceType='myapp/components/a'}"></div>
<!-- 导致循环依赖 -->
```

**正确:**
避免循环依赖，重新设计组件结构。

## 7. 性能陷阱

### 陷阱 14: 在循环中计算

**错误:**
```html
<ul>
    <li data-sly-list="${properties.items}">
        ${model.expensiveCalculation(item)}
    </li>
</ul>
<!-- 每次循环都调用，性能差 -->
```

**正确:**
```html
<sly data-sly-use.model="${'com.example.Model' @ items=properties.items}"></sly>
<ul>
    <li data-sly-list="${model.processedItems}">
        ${item.result}
    </li>
</ul>
```

### 陷阱 15: 重复计算

**错误:**
```html
<div class="${properties.isActive ? 'active' : 'inactive'}">内容</div>
<button data-sly-attribute.disabled="${!properties.isActive}">提交</button>
<!-- 重复计算 properties.isActive -->
```

**正确:**
```html
<sly data-sly-set.isActive="${properties.isActive == true}"></sly>
<div class="${isActive ? 'active' : 'inactive'}">内容</div>
<button data-sly-attribute.disabled="${!isActive}">提交</button>
```

## 8. 转义陷阱

### 陷阱 16: 不必要的 unescape

**错误:**
```html
<div data-sly-unescape="${properties.userInput}"></div>
<!-- 如果 userInput 包含恶意脚本，会导致 XSS -->
```

**正确:**
```html
<!-- 使用自动转义 -->
<div>${properties.userInput}</div>

<!-- 或使用 Use API 清理 -->
<sly data-sly-use.sanitizer="${'com.example.HtmlSanitizer' @ html=properties.htmlContent}"></sly>
<div data-sly-unescape="${sanitizer.sanitizedHtml}"></div>
```

## 9. 变量作用域陷阱

### 陷阱 17: 变量作用域错误

**错误:**
```html
<div data-sly-set.var="${'value'}">
    ${var}
</div>
<div>${var}</div>
<!-- 第二个 div 中 var 不在作用域内 -->
```

**正确:**
```html
<sly data-sly-set.var="${'value'}"></sly>
<div>${var}</div>
<div>${var}</div>
```

## 10. 客户端库陷阱

### 陷阱 18: 忘记引入 clientlib 模板

**错误:**
```html
<sly data-sly-call="${clientlib.css @ categories='myapp.components'}"></sly>
<!-- clientlib 未定义 -->
```

**正确:**
```html
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components'}"></sly>
```

## 避免陷阱的最佳实践

1. **始终检查空值**: 使用可选链或条件检查
2. **使用变量**: 避免重复计算
3. **在 Use API 中处理复杂逻辑**: 保持模板简洁
4. **验证输入**: 在 Use API 中验证数据
5. **测试边界情况**: 测试空值、null、undefined
6. **使用类型安全**: 使用 Sling Models
7. **代码审查**: 让其他人审查代码
8. **文档化**: 记录复杂逻辑

## 调试技巧

1. **启用开发者模式**: 查看详细错误信息
2. **使用临时调试输出**: 输出变量值
3. **检查浏览器控制台**: 查看 JavaScript 错误
4. **查看 AEM 日志**: 检查服务器端错误
5. **使用断点**: 在 Use API 中使用断点

## 总结

避免这些常见陷阱可以：
- 提高代码质量
- 减少错误
- 改善性能
- 增强安全性
- 提高可维护性

记住：**预防胜于治疗**，在编写代码时就避免这些陷阱。

