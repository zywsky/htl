# HTL 组件开发最佳实践

## 1. 代码组织

### 文件结构
```
component-name/
├── .content.xml              # 组件定义
├── component-name.html       # HTL 模板
├── _cq_dialog/              # 编辑对话框
├── _cq_design_dialog/       # 设计对话框
├── _cq_htmlTag/             # HTML 标签配置
└── clientlibs/              # 客户端库
    ├── css/
    │   └── styles.css
    └── js/
        └── scripts.js
```

### 代码分离原则
- **模板（HTL）**: 只负责数据展示和结构
- **业务逻辑**: 放在 Sling Models 中
- **样式**: 使用客户端库管理
- **脚本**: 使用客户端库管理

## 2. HTL 模板最佳实践

### 使用语义化 HTML
```html
<!-- 好 -->
<article class="card-component">
    <header class="card-header">
        <h2>${properties.title}</h2>
    </header>
</article>

<!-- 不好 -->
<div class="card-component">
    <div class="card-header">
        <div class="title">${properties.title}</div>
    </div>
</div>
```

### 条件渲染
```html
<!-- 使用 data-sly-test 避免渲染空元素 -->
<div data-sly-test="${properties.content}">
    ${properties.content}
</div>
```

### 变量使用
```html
<!-- 使用变量避免重复计算 -->
<sly data-sly-set.isActive="${properties.status == 'active'}"></sly>
<div data-sly-test="${isActive}">活跃内容</div>
<button data-sly-attribute.disabled="${!isActive}">提交</button>
```

### 属性设置
```html
<!-- 使用 data-sly-attribute 动态设置属性 -->
<div data-sly-attribute="${{
    'class': 'component ' + properties.cssClass,
    'id': component.id,
    'data-type': properties.type
}}">
</div>
```

## 3. Sling Models 最佳实践

### 使用接口定义 Model
```java
@Model(adaptables = Resource.class)
public interface MyComponentModel {
    @Inject
    String getTitle();
    
    @Inject @Optional
    String getDescription();
}
```

### 使用依赖注入
```java
@Model(adaptables = Resource.class)
public class MyComponentModel {
    @Inject
    private MyService myService;
    
    @PostConstruct
    protected void init() {
        // 初始化逻辑
    }
}
```

### 提供默认值
```java
@Model(adaptables = Resource.class)
public interface MyComponentModel {
    @Inject @Optional
    String getTitle();
    
    default String getTitle() {
        return getTitle() != null ? getTitle() : "默认标题";
    }
}
```

## 4. 性能优化

### 使用 Use API 缓存
- 在 Sling Model 中缓存计算结果
- 避免在模板中进行复杂计算

### 客户端库优化
- 使用 `async` 加载非关键脚本
- 使用 `defer` 加载需要 DOM 的脚本
- 合并和压缩 CSS/JS

### 图片优化
- 使用响应式图片（srcset, sizes）
- 使用懒加载（loading="lazy"）
- 使用适当的图片格式

## 5. 可访问性

### 语义化 HTML
- 使用正确的 HTML 元素
- 使用 ARIA 属性

### 无障碍支持
```html
<button data-sly-attribute.aria-label="${properties.ariaLabel}">
    ${properties.label}
</button>
```

## 6. 安全性

### XSS 防护
- HTL 自动转义，无需手动处理
- 只在必要时使用 `data-sly-unescape`，并确保内容安全

### 数据验证
- 在 Sling Model 中验证数据
- 处理空值和边界情况

## 7. 国际化

### 使用 i18n API
```html
<sly data-sly-use.i18n="${'com.day.cq.i18n.I18n' @ request=request}"></sly>
<p>${i18n.get('welcome.message')}</p>
```

### 在 Sling Model 中处理
```java
@Model(adaptables = SlingHttpServletRequest.class)
public interface I18nModel {
    @Inject
    private I18n i18n;
    
    default String getTitle() {
        return i18n.get("component.title");
    }
}
```

## 8. 调试技巧

### 使用开发者工具
- 浏览器开发者工具
- AEM 开发者模式
- Sling Model 日志

### 临时调试输出
```html
<!-- 仅在开发时使用 -->
<div data-sly-test="${properties.debug}">
    <pre>${JSON.stringify(properties)}</pre>
</div>
```

## 9. 测试

### 单元测试
- 为 Sling Models 编写单元测试
- 测试数据验证逻辑

### 集成测试
- 测试组件渲染
- 测试对话框配置

## 10. 文档

### 代码注释
- 在模板中添加说明注释
- 文档化组件参数

### 使用说明
- 在组件 README 中说明用法
- 提供示例配置

