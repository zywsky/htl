# HTL 性能优化完整指南

## 性能优化策略

### 1. 表达式优化

#### 避免重复计算

**不好:**
```html
<div class="${properties.isActive ? 'active' : 'inactive'}">内容</div>
<button data-sly-attribute.disabled="${!properties.isActive}">提交</button>
```

**好:**
```html
<sly data-sly-set.isActive="${properties.isActive == true}"></sly>
<div class="${isActive ? 'active' : 'inactive'}">内容</div>
<button data-sly-attribute.disabled="${!isActive}">提交</button>
```

### 2. 使用 Use API 缓存

#### 在 Sling Model 中缓存计算结果

```java
@Model(adaptables = Resource.class)
public class CachedModel {
    private String cachedValue;
    
    @PostConstruct
    protected void init() {
        // 只计算一次
        cachedValue = expensiveCalculation();
    }
    
    public String getCachedValue() {
        return cachedValue;
    }
}
```

### 3. 条件渲染优化

#### 使用 data-sly-test 避免不必要的渲染

**不好:**
```html
<div>
    <h1>${properties.title}</h1>
    <p>${properties.description}</p>
</div>
```

**好:**
```html
<div data-sly-test="${properties.title || properties.description}">
    <h1 data-sly-test="${properties.title}">${properties.title}</h1>
    <p data-sly-test="${properties.description}">${properties.description}</p>
</div>
```

### 4. 循环优化

#### 避免在循环中进行复杂计算

**不好:**
```html
<ul>
    <li data-sly-list="${properties.items}">
        ${model.expensiveCalculation(item)}
    </li>
</ul>
```

**好:**
```html
<!-- 在 Use API 中预处理 -->
<sly data-sly-use.model="${'com.example.Model' @ items=properties.items}"></sly>
<ul>
    <li data-sly-list="${model.processedItems}">
        ${item.result}
    </li>
</ul>
```

### 5. 资源包含优化

#### 限制嵌套深度

```html
<!-- 避免过度嵌套 -->
<div data-sly-resource="${'child1' @ resourceType='myapp/components/child'}">
    <!-- 子组件内部又包含子组件... -->
</div>
```

#### 使用缓存

```java
@Model(adaptables = Resource.class)
@Cache
public interface CachedModel {
    // 使用 @Cache 注解缓存 Model
}
```

### 6. 客户端库优化

#### 使用 async 和 defer

```html
<!-- 非关键脚本异步加载 -->
<sly data-sly-call="${clientlib.js @ categories='myapp.non-critical', async=true}"></sly>

<!-- 关键脚本延迟加载 -->
<sly data-sly-call="${clientlib.js @ categories='myapp.critical', defer=true}"></sly>
```

#### 条件加载

```html
<div data-sly-test="${properties.loadAdvancedFeatures}">
    <sly data-sly-call="${clientlib.all @ categories='myapp.advanced'}"></sly>
</div>
```

### 7. 图片优化

#### 使用响应式图片

```html
<img src="${properties.imagePath}"
     srcset="${properties.imageSrcset}"
     sizes="${properties.imageSizes}"
     loading="lazy"
     alt="${properties.altText}">
```

#### 懒加载

```html
<img src="${properties.imagePath}"
     loading="lazy"
     alt="${properties.altText}">
```

### 8. 模板优化

#### 避免过度嵌套模板

**不好:**
```html
<template data-sly-template.level1="${@ data}">
    <sly data-sly-call="${level2 @ data=data}"></sly>
</template>
<template data-sly-template.level2="${@ data}">
    <sly data-sly-call="${level3 @ data=data}"></sly>
</template>
<!-- ... -->
```

**好:**
```html
<template data-sly-template.combined="${@ data}">
    <!-- 直接在模板中处理，避免过度嵌套 -->
</template>
```

### 9. 数据获取优化

#### 批量获取数据

```java
@Model(adaptables = Resource.class)
public class BatchModel {
    private List<Item> items;
    
    @PostConstruct
    protected void init() {
        // 一次性获取所有数据，而不是逐个获取
        items = batchFetchItems();
    }
    
    public List<Item> getItems() {
        return items;
    }
}
```

### 10. 缓存策略

#### 使用 Dispatcher 缓存

```html
<!-- 在组件上设置缓存属性 -->
<div class="component" 
     data-sly-attribute.data-cacheable="${properties.cacheable}">
</div>
```

#### 使用浏览器缓存

```html
<!-- 设置适当的缓存头 -->
<link rel="stylesheet" href="${clientlib.css.url}" />
```

## 性能监控

### 1. 使用性能工具

- AEM 性能监控
- 浏览器开发者工具
- 性能分析工具

### 2. 关键指标

- 页面加载时间
- 首次内容绘制 (FCP)
- 最大内容绘制 (LCP)
- 交互延迟 (TTI)

### 3. 日志和监控

```java
@Model(adaptables = Resource.class)
public class PerformanceModel {
    private static final Logger LOG = LoggerFactory.getLogger(PerformanceModel.class);
    
    @PostConstruct
    protected void init() {
        long startTime = System.currentTimeMillis();
        // 执行操作
        long duration = System.currentTimeMillis() - startTime;
        LOG.debug("Operation took {} ms", duration);
    }
}
```

## 性能最佳实践

1. **测量优先**: 在优化前先测量性能
2. **使用缓存**: 合理使用缓存策略
3. **减少计算**: 在 Use API 中预处理数据
4. **优化循环**: 避免在循环中进行复杂操作
5. **条件渲染**: 使用条件渲染减少 DOM 操作
6. **异步加载**: 非关键资源异步加载
7. **图片优化**: 使用响应式图片和懒加载
8. **代码拆分**: 按需加载代码
9. **监控性能**: 持续监控性能指标
10. **定期审查**: 定期审查和优化代码

## 性能检查清单

- [ ] 表达式已优化，避免重复计算
- [ ] 使用 Use API 缓存计算结果
- [ ] 条件渲染已优化
- [ ] 循环中避免复杂计算
- [ ] 资源包含深度合理
- [ ] 客户端库使用 async/defer
- [ ] 图片已优化（响应式、懒加载）
- [ ] 模板嵌套深度合理
- [ ] 数据批量获取
- [ ] 缓存策略已配置
- [ ] 性能指标已监控

