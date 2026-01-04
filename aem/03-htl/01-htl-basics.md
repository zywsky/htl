# HTL (Sightly) 模板语言：第一部分 - 基础语法

## 📖 什么是 HTL?

HTL (HTML Template Language)，之前称为 Sightly，是 AEM 推荐的模板语言。它提供了一种简洁、安全的模板语法，用于在 HTML 中嵌入动态内容。

## 🎯 HTL 的优势

1. **安全性**：自动进行 XSS 防护
2. **简洁性**：语法简洁易读
3. **上下文感知**：根据使用场景自动处理内容
4. **表达式语言**：强大的表达式支持

## 💻 HTL 基础语法

### 基本表达式

```html
<!--
    ${expression}: 输出表达式结果
    自动进行 HTML 转义
-->
<p>${properties.title}</p>
```

### 上下文（Context）

HTL 使用上下文来决定如何处理输出，防止 XSS 攻击：

```html
<!--
    @ context='html': HTML 上下文，允许 HTML 标签
    @ context='text': 文本上下文，转义所有 HTML（默认）
    @ context='attribute': 属性上下文，用于 HTML 属性
    @ context='uri': URI 上下文，用于链接
    @ context='script': JavaScript 上下文
    @ context='style': CSS 上下文
    @ context='json': JSON 上下文
-->

<!-- 文本内容（默认，转义 HTML） -->
<p>${properties.description}</p>

<!-- HTML 内容（允许 HTML 标签） -->
<div>${properties.content @ context='html'}</div>

<!-- HTML 属性 -->
<img alt="${properties.altText @ context='attribute'}" />

<!-- URI -->
<a href="${properties.link @ context='uri'}">链接</a>
```

### 条件渲染 (data-sly-test)

```html
<!--
    data-sly-test: 如果条件为真，渲染元素；否则不渲染
    可用于条件性地显示元素
-->

<!-- 如果标题存在，显示 h1 -->
<h1 data-sly-test="${properties.title}">${properties.title}</h1>

<!-- 使用 else 逻辑 -->
<div data-sly-test="${properties.showContent}">
    <!-- 内容存在时显示 -->
    <p>${properties.content}</p>
</div>
<div data-sly-test="${!properties.showContent}">
    <!-- 内容不存在时显示 -->
    <p>暂无内容</p>
</div>
```

### 循环 (data-sly-list)

```html
<!--
    data-sly-list: 遍历集合
    语法: data-sly-list="variableName in ${collection}"
-->

<!-- 遍历数组 -->
<ul data-sly-list.item="${properties.items}">
    <li>${item}</li>
</ul>

<!-- 使用循环状态（index, count, first, middle, last, odd, even） -->
<ul data-sly-list.item="${properties.items}">
    <li class="${itemList.index % 2 == 0 ? 'even' : 'odd'}">
        项目 ${itemList.count}: ${item}
        <span data-sly-test="${itemList.first}">（第一个）</span>
        <span data-sly-test="${itemList.last}">（最后一个）</span>
    </li>
</ul>
```

### 引入对象 (data-sly-use)

```html
<!--
    data-sly-use: 引入 Java 对象（通常是 Sling Model）
    语法: data-sly-use="variableName=package.ClassName"
-->

<sly data-sly-use.model="com.example.core.models.MyModel">
    <p>${model.title}</p>
    <p>${model.description}</p>
</sly>

<!--
    也可以适配资源为模型
-->
<sly data-sly-use.model="${'com.example.core.models.MyModel' @ context='script'}">
    <p>${model.title}</p>
</sly>
```

### 包含/模板 (data-sly-include, data-sly-call)

```html
<!--
    data-sly-include: 包含其他模板
    语法: data-sly-include="${'path/to/template.html'}"
-->

<!-- 包含其他组件 -->
<div data-sly-include="${'./header.html'}"></div>

<!-- 包含其他资源 -->
<div data-sly-include="${resource.path + '/template.html'}"></div>

<!--
    data-sly-call: 调用已定义的模板片段
-->
<template data-sly-template.button="${@ text, link}">
    <a href="${link @ context='uri'}" class="button">${text}</a>
</template>

<!-- 使用模板 -->
<div data-sly-call="${button @ text='点击我', link='/page.html'}"></div>
```

### 属性操作 (data-sly-attribute, data-sly-set, data-sly-unwrap)

```html
<!--
    data-sly-attribute: 动态设置 HTML 属性
-->
<img src="${properties.imagePath}" 
     data-sly-attribute.alt="${properties.altText}"
     data-sly-attribute.class="${properties.cssClass}"/>

<!--
    条件性地设置属性
-->
<div data-sly-attribute.class="${properties.highlighted ? 'highlight' : ''}">
    内容
</div>

<!--
    data-sly-set: 设置变量
-->
<sly data-sly-set.title="${properties.title ? properties.title : '默认标题'}">
    <h1>${title}</h1>
</sly>

<!--
    data-sly-unwrap: 移除包装元素（只渲染内容）
-->
<ul>
    <li data-sly-list.item="${properties.items}" data-sly-unwrap>
        ${item}
    </li>
</ul>
<!-- 输出: <ul><li>item1</li><li>item2</li></ul> -->
<!-- 如果没有 unwrap，会多一层包装 -->
```

### 元素操作 (data-sly-element, data-sly-text, data-sly-replace)

```html
<!--
    data-sly-element: 动态改变 HTML 元素标签
-->
<div data-sly-element="${properties.tagName}">
    内容
</div>
<!-- 如果 tagName = 'h1'，输出: <h1>内容</h1> -->

<!--
    data-sly-text: 只输出文本内容，不包含 HTML 标签
-->
<div data-sly-text="${properties.htmlContent}"></div>
<!-- 即使 htmlContent 包含 HTML 标签，也会被转义为文本 -->

<!--
    data-sly-replace: 替换整个元素内容
-->
<div data-sly-replace="${'./replacement.html'}"></div>
<!-- 用其他模板的内容替换整个 div -->
```

### 资源操作

```html
<!--
    访问资源信息
-->
<sly data-sly-use.page="com.adobe.cq.wcm.core.components.models.Page">
    <h1>${page.title}</h1>
    <p>当前页面路径: ${resource.path}</p>
    <p>资源类型: ${resource.resourceType}</p>
    
    <!-- 获取子资源 -->
    <div data-sly-list.child="${resource.children}">
        <p>${child.name}</p>
    </div>
</sly>
```

### 实用表达式

```html
<!--
    字符串操作
-->
<p>${properties.title ? properties.title : '默认标题'}</p>
<p>${properties.title || '默认标题'}</p>

<!--
    数学运算
-->
<p>总计: ${properties.quantity * properties.price}</p>

<!--
    条件表达式（三元运算符）
-->
<p>${properties.status == 'active' ? '激活' : '未激活'}</p>

<!--
    数组操作
-->
<p>项目数量: ${properties.items.size()}</p>
<p>第一个项目: ${properties.items[0]}</p>
```

## 📝 完整示例：组件模板

```html
<!--
    完整的组件 HTL 模板示例
-->
<sly data-sly-use.component="com.example.core.models.ArticleComponentModel"
     data-sly-use.page="com.adobe.cq.wcm.core.components.models.Page">
    
    <article class="article-component" data-component-path="${resource.path}">
        
        <!-- 标题 -->
        <header class="article-component__header">
            <h1 data-sly-test="${component.title}">${component.title}</h1>
            
            <!-- 作者和日期 -->
            <div class="article-component__meta" 
                 data-sly-test="${component.author || component.publishDate}">
                <span data-sly-test="${component.author}">
                    作者: ${component.author @ context='text'}
                </span>
                <span data-sly-test="${component.publishDate}">
                    发布时间: ${component.publishDate}
                </span>
            </div>
        </header>
        
        <!-- 内容 -->
        <div class="article-component__content">
            ${component.content @ context='html'}
        </div>
        
        <!-- 标签列表 -->
        <footer class="article-component__tags" 
                data-sly-test="${component.tags && component.tags.size() > 0}">
            <span class="tags-label">标签:</span>
            <ul class="tags-list">
                <li data-sly-list.tag="${component.tags}">
                    <span class="tag">${tag @ context='text'}</span>
                </li>
            </ul>
        </footer>
        
        <!-- 相关文章 -->
        <aside class="article-component__related" 
               data-sly-test="${component.relatedArticles}">
            <h3>相关文章</h3>
            <ul>
                <li data-sly-list.article="${component.relatedArticles}">
                    <a href="${article.link @ context='uri'}">
                        ${article.title @ context='text'}
                    </a>
                </li>
            </ul>
        </aside>
        
    </article>
</sly>
```

## 🔑 关键要点

1. **始终使用上下文**：根据使用场景选择合适的上下文
2. **避免在模板中写逻辑**：业务逻辑应该放在 Sling Model 中
3. **使用 data-sly-use**：引入 Sling Model 处理数据
4. **条件渲染**：使用 data-sly-test 进行条件判断
5. **循环遍历**：使用 data-sly-list 遍历集合

## ➡️ 下一步

在下一节中，我们将学习 **HTL 高级特性和表达式**。

