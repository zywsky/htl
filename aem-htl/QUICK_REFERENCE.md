# HTL 快速参考手册

## 核心指令

### 表达式
```html
${expression}
```
- 显示表达式结果（自动转义）

### 条件渲染
```html
<div data-sly-test="${condition}">内容</div>
```
- 条件为真时渲染元素

### 循环
```html
<!-- 重复元素 -->
<ul>
    <li data-sly-list="${items}">${item}</li>
</ul>

<!-- 只重复内容 -->
<div>
    <span data-sly-repeat="${items}">${item}, </span>
</div>
```

### 变量
```html
<sly data-sly-set.variable="${value}"></sly>
<div>${variable}</div>
```

### 属性
```html
<div data-sly-attribute.class="${cssClass}"></div>
<div data-sly-attribute="${{
    'class': 'foo',
    'id': 'bar'
}}"></div>
```

### 文本
```html
<div data-sly-text="${properties.text}">默认文本会被替换</div>
```

### 元素
```html
<sly data-sly-element="${tagName}">内容</sly>
```

## 组件相关

### Resource Include
```html
<div data-sly-resource="${'child' @ resourceType='myapp/components/child'}"></div>
```

### Use API
```html
<sly data-sly-use.model="${'com.example.Model'}"></sly>
<div>${model.value}</div>
```

### 客户端库
```html
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components'}"></sly>
<sly data-sly-call="${clientlib.js @ categories='myapp.components'}"></sly>
```

## 模板

### 定义模板
```html
<template data-sly-template.name="${@ param1, param2='default'}">
    内容
</template>
```

### 调用模板
```html
<div data-sly-call="${name @ param1='value'}"></div>
```

## 内置对象

- `properties`: 当前资源的属性
- `currentPage`: 当前页面对象
- `resource`: 当前资源对象
- `component`: 组件对象
- `request`: 请求对象
- `response`: 响应对象

## 常用表达式

### 字符串操作
- `string.length`: 长度
- `string.toUpperCase()`: 转大写
- `string.toLowerCase()`: 转小写
- `string.trim()`: 去除空格

### 数组操作
- `array.length`: 长度
- `list.size`: 大小
- `list.isEmpty`: 是否为空

### 条件表达式
- `condition ? trueValue : falseValue`: 三元运算符
- `value || defaultValue`: 默认值
- `value && otherValue`: 逻辑与
- `!value`: 逻辑非

### 循环变量
- `item`: 当前项
- `itemList.index`: 索引（0-based）
- `itemList.count`: 计数（1-based）
- `itemList.first`: 是否第一项
- `itemList.last`: 是否最后一项
- `itemList.odd`: 是否奇数
- `itemList.even`: 是否偶数
- `itemList.size`: 列表大小

## 最佳实践

1. **使用 Sling Models 处理业务逻辑**
2. **使用 data-sly-test 避免渲染空元素**
3. **使用变量避免重复计算**
4. **使用语义化 HTML**
5. **使用客户端库管理 CSS/JS**
6. **处理空状态**
7. **使用条件渲染优化性能**
8. **使用国际化 API**

## 常见模式

### 空状态处理
```html
<div data-sly-test="${properties.items && properties.items.size > 0}">
    <!-- 有内容 -->
</div>
<div data-sly-test="${!properties.items || properties.items.size == 0}">
    <!-- 空状态 -->
</div>
```

### 条件类名
```html
<div class="base-class ${properties.isActive ? 'active' : 'inactive'}"></div>
```

### 默认值
```html
<h1>${properties.title || '默认标题'}</h1>
```

### 安全属性访问
```html
<div>${properties.author?.name || '匿名'}</div>
```

