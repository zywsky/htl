# AEM 高级主题：第一部分 - 客户端库 (Client Libraries)

## 📖 什么是客户端库?

客户端库（Client Libraries，简称 clientlibs）是 AEM 管理和提供前端资源（CSS、JavaScript）的机制。它提供资源合并、压缩、版本控制等功能。

## 🏗️ 客户端库结构

### 目录结构

```
/apps/myproject/clientlibs/
├── base/                              # 基础客户端库
│   ├── .content.xml                   # 客户端库定义
│   ├── css/
│   │   └── styles.css
│   └── js/
│       └── scripts.js
└── theme/                             # 主题客户端库
    ├── .content.xml
    ├── css/
    └── js/
```

### 客户端库定义 (.content.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          jcr:title="MyProject Base"
          categories="[myproject.base]"
          allowProxy="{Boolean}true">
    
    <!--
    jcr:primaryType: 必须是 cq:ClientLibraryFolder
    jcr:title: 客户端库名称
    categories: 客户端库类别（数组）
    allowProxy: 允许通过代理访问（推荐设置为 true）
    -->
</jcr:root>
```

## 💻 基础客户端库示例

### 示例 1：基础客户端库

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          jcr:title="MyProject Base Library"
          categories="[myproject.base]"
          allowProxy="{Boolean}true"/>
```

#### CSS 文件 (css/styles.css)

```css
/* 基础样式 */
.myproject-component {
    padding: 20px;
    margin: 10px 0;
}

.myproject-component__title {
    font-size: 24px;
    font-weight: bold;
    color: #333;
}

.myproject-component__content {
    font-size: 16px;
    line-height: 1.6;
    color: #666;
}
```

#### JavaScript 文件 (js/scripts.js)

```javascript
/**
 * MyProject 基础脚本
 */
(function(window, document) {
    'use strict';

    // 避免全局污染，使用命名空间
    window.MyProject = window.MyProject || {};

    /**
     * 初始化函数
     */
    window.MyProject.init = function() {
        console.log('MyProject initialized');
        
        // 初始化组件
        initComponents();
    };

    /**
     * 初始化所有组件
     */
    function initComponents() {
        // 查找所有组件
        var components = document.querySelectorAll('[data-component-path]');
        
        components.forEach(function(component) {
            var componentPath = component.getAttribute('data-component-path');
            var resourceType = component.getAttribute('data-resource-type');
            
            // 根据资源类型初始化不同的组件
            if (resourceType === 'myproject/components/article') {
                initArticleComponent(component);
            }
        });
    }

    /**
     * 初始化文章组件
     */
    function initArticleComponent(element) {
        // 组件特定的初始化逻辑
        console.log('Article component initialized:', element);
    }

    // DOM 加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', window.MyProject.init);
    } else {
        window.MyProject.init();
    }

})(window, document);
```

### 示例 2：依赖其他客户端库

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          jcr:title="MyProject Enhanced Library"
          categories="[myproject.enhanced]"
          dependencies="[myproject.base,jquery]"
          allowProxy="{Boolean}true"/>
          
          <!--
          dependencies: 依赖的其他客户端库类别
          这里依赖 myproject.base 和 jquery
          -->
```

### 示例 3：嵌入其他客户端库

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          jcr:title="MyProject All-in-One"
          categories="[myproject.all]"
          embed="[myproject.base,myproject.theme]"
          allowProxy="{Boolean}true"/>
          
          <!--
          embed: 嵌入其他客户端库的内容
          这些库的资源会被合并到这个库中
          -->
```

## 📝 在 HTL 中使用客户端库

### 方法 1：使用 data-sly-call

```html
<!--
    在 HTL 模板中引用客户端库
-->
<sly data-sly-use.clientlib="/libs/granite/sightly/templates/clientlib.html"
     data-sly-call="${clientlib.css @ categories='myproject.base'}"/>

<sly data-sly-call="${clientlib.js @ categories='myproject.base'}"/>

<!--
    或者同时引用 CSS 和 JS
-->
<sly data-sly-call="${clientlib.all @ categories='myproject.base'}"/>
```

### 方法 2：在页面模板中使用

```html
<!DOCTYPE html>
<html>
<head>
    <title>${properties.jcr:title}</title>
    
    <!-- 引用客户端库 CSS -->
    <sly data-sly-use.clientlib="/libs/granite/sightly/templates/clientlib.html"/>
    <sly data-sly-call="${clientlib.css @ categories='myproject.base'}"/>
</head>
<body>
    
    <!-- 页面内容 -->
    <div class="page">
        <sly data-sly-resource="${resource.path + '/content'}"/>
    </div>
    
    <!-- 引用客户端库 JavaScript -->
    <sly data-sly-call="${clientlib.js @ categories='myproject.base'}"/>
</body>
</html>
```

## 🎨 组件特定的客户端库

### 在组件目录中创建客户端库

```
/apps/myproject/components/article/
├── .content.xml
├── article.html
├── _cq_dialog/
│   └── .content.xml
└── clientlibs/                        # 组件特定的客户端库
    ├── .content.xml
    ├── css/
    │   └── article.css
    └── js/
        └── article.js
```

### 组件客户端库定义

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          jcr:title="Article Component"
          categories="[myproject.components.article]"
          allowProxy="{Boolean}true"/>
```

### 在组件 HTL 中使用

```html
<!-- article.html -->
<sly data-sly-use.clientlib="/libs/granite/sightly/templates/clientlib.html"/>

<!-- 引用组件特定的 CSS 和 JS -->
<sly data-sly-call="${clientlib.all @ categories='myproject.components.article'}"/>

<div class="article-component">
    <!-- 组件内容 -->
</div>
```

## 🔧 高级配置

### 示例 4：压缩和合并配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          jcr:title="MyProject Optimized"
          categories="[myproject.optimized]"
          allowProxy="{Boolean}true"
          jsProcessor="[default:min:gcc]"
          cssProcessor="[default:min]">
    
    <!--
    jsProcessor: JavaScript 处理器（压缩）
    cssProcessor: CSS 处理器（压缩）
    这些会在生产环境中自动应用
    -->
</jcr:root>
```

### 示例 5：条件加载（只在编辑模式下加载）

```html
<!-- 只在作者模式下加载编辑相关的客户端库 -->
<sly data-sly-test="${wcmmode.edit || wcmmode.design}">
    <sly data-sly-use.clientlib="/libs/granite/sightly/templates/clientlib.html"/>
    <sly data-sly-call="${clientlib.css @ categories='myproject.author'}"/>
    <sly data-sly-call="${clientlib.js @ categories='myproject.author'}"/>
</sly>
```

## 📦 组织最佳实践

### 推荐的客户端库结构

```
/apps/myproject/clientlibs/
├── base/                              # 基础样式和脚本
│   └── categories: myproject.base
├── theme/                             # 主题样式
│   └── categories: myproject.theme
├── components/                        # 组件通用样式
│   └── categories: myproject.components
└── author/                            # 作者模式专用
    └── categories: myproject.author
```

### 组件特定的客户端库

```
/apps/myproject/components/
├── article/
│   └── clientlibs/
│       └── categories: myproject.components.article
└── product/
    └── clientlibs/
        └── categories: myproject.components.product
```

## 🔑 关键要点

1. **类别系统**：使用 categories 标识客户端库
2. **依赖管理**：使用 dependencies 声明依赖
3. **嵌入资源**：使用 embed 合并其他库
4. **代理访问**：设置 allowProxy=true 以便通过 /etc.clientlibs 访问
5. **组织方式**：按功能和组件组织客户端库

## ➡️ 下一步

学习更多前端开发最佳实践，如 SPA 开发、前端构建工具集成等。

