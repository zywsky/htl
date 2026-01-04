# AEM 高级主题：第三部分 - 性能优化

## 📖 性能优化的重要性

性能直接影响用户体验和 SEO。AEM 性能优化涉及多个层面：
- **查询优化**
- **缓存策略**
- **资源优化**
- **代码优化**

## 🔍 查询优化

### 示例 1：避免在循环中查询

```java
// ❌ 错误：在循环中进行查询
public List<Page> getPages(List<String> paths) {
    List<Page> pages = new ArrayList<>();
    for (String path : paths) {
        // 每次循环都执行一次查询
        Resource resource = resourceResolver.getResource(path);
        if (resource != null) {
            pages.add(resource.adaptTo(Page.class));
        }
    }
    return pages;
}

// ✅ 正确：批量查询
public List<Page> getPages(List<String> paths) {
    List<Page> pages = new ArrayList<>();
    for (String path : paths) {
        Resource resource = resourceResolver.getResource(path);
        if (resource != null) {
            pages.add(resource.adaptTo(Page.class));
        }
    }
    return pages;
}

// ✅ 更好：使用单个查询
public List<Page> getPages(String parentPath) {
    List<Page> pages = new ArrayList<>();
    // 使用单个查询获取所有子节点
    String query = "SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE('" + parentPath + "')";
    Iterator<Resource> resources = resourceResolver.findResources(query, Query.JCR_SQL2);
    
    while (resources.hasNext()) {
        Resource resource = resources.next();
        Page page = resource.adaptTo(Page.class);
        if (page != null) {
            pages.add(page);
        }
    }
    return pages;
}
```

### 示例 2：使用索引优化查询

```java
// ✅ 使用索引的查询
public List<Page> searchPages(String keyword) {
    // 使用 jcr:title 属性查询（通常有索引）
    String query = "SELECT * FROM [cq:Page] WHERE [jcr:title] LIKE '%" + keyword + "%'";
    
    // 更好的方式：使用参数化查询（避免 SQL 注入）
    Map<String, String> params = new HashMap<>();
    params.put("keyword", "%" + keyword + "%");
    
    QueryManager queryManager = resourceResolver.adaptTo(Session.class)
        .getWorkspace().getQueryManager();
    
    // 使用 XPath 查询（支持参数）
    String xpath = "/jcr:root/content//element(*, cq:Page)[jcr:contains(@jcr:title, $keyword)]";
    
    // 执行查询
    Iterator<Resource> results = resourceResolver.findResources(xpath, Query.XPATH);
    
    // 处理结果...
    return convertToPages(results);
}
```

### 示例 3：限制查询结果数量

```java
public List<Page> getRecentPages(int limit) {
    // 限制查询结果数量
    String query = "SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE('/content/myproject') " +
                   "ORDER BY [jcr:created] DESC";
    
    QueryManager queryManager = resourceResolver.adaptTo(Session.class)
        .getWorkspace().getQueryManager();
    
    Query q = queryManager.createQuery(query, Query.JCR_SQL2);
    
    // 设置结果限制
    q.setLimit(limit);
    
    QueryResult result = q.execute();
    Iterator<Resource> resources = result.getNodes();
    
    return convertToPages(resources);
}
```

## 💾 缓存策略

### 示例 4：使用 Sling Model 缓存

```java
package com.example.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 带缓存的 Sling Model
 */
@Model(adaptables = Resource.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CachedDataModel {

    @Self
    private Resource resource;

    // 缓存数据
    private String cachedData;
    private long cacheTimestamp;
    private static final long CACHE_DURATION = TimeUnit.MINUTES.toMillis(5);

    @PostConstruct
    protected void init() {
        // 初始化时不加载数据，延迟加载
    }

    /**
     * 获取数据（带缓存）
     */
    public String getData() {
        // 检查缓存是否过期
        if (cachedData == null || isCacheExpired()) {
            // 从资源加载数据（可能涉及数据库查询等耗时操作）
            cachedData = loadDataFromResource();
            cacheTimestamp = System.currentTimeMillis();
        }
        return cachedData;
    }

    /**
     * 检查缓存是否过期
     */
    private boolean isCacheExpired() {
        return (System.currentTimeMillis() - cacheTimestamp) > CACHE_DURATION;
    }

    /**
     * 从资源加载数据
     */
    private String loadDataFromResource() {
        // 模拟耗时操作
        return resource.getValueMap().get("data", String.class);
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cachedData = null;
        cacheTimestamp = 0;
    }
}
```

### 示例 5：使用 OSGi 缓存服务

```java
package com.example.core.services.impl;

import com.example.core.services.CacheService;
import org.apache.sling.commons.cache.api.Cache;
import org.apache.sling.commons.cache.api.CacheManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * 缓存服务实现
 */
@Component(service = CacheService.class, immediate = true)
public class CacheServiceImpl implements CacheService {

    @Reference
    private CacheManager cacheManager;

    private Cache<String, Object> cache;

    @Activate
    protected void activate() {
        // 创建缓存
        cache = cacheManager.getCache("myproject-cache");
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        Object value = cache.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
```

## 🚀 资源优化

### 示例 6：延迟加载资源

```java
package com.example.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 延迟加载的 Model
 */
@Model(adaptables = Resource.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class LazyLoadingModel {

    @Self
    private Resource resource;

    private List<Resource> children;
    private boolean loaded = false;

    @PostConstruct
    protected void init() {
        // 不在初始化时加载，延迟到真正需要时
    }

    /**
     * 获取子资源（延迟加载）
     */
    public List<Resource> getChildren() {
        if (!loaded) {
            loadChildren();
            loaded = true;
        }
        return children;
    }

    /**
     * 加载子资源
     */
    private void loadChildren() {
        children = new ArrayList<>();
        resource.listChildren().forEachRemaining(children::add);
    }

    /**
     * 检查是否已加载
     */
    public boolean isLoaded() {
        return loaded;
    }
}
```

### 示例 7：限制资源遍历深度

```java
public List<Resource> getChildren(Resource resource, int maxDepth) {
    List<Resource> result = new ArrayList<>();
    getChildrenRecursive(resource, result, 0, maxDepth);
    return result;
}

private void getChildrenRecursive(Resource resource, List<Resource> result, 
                                 int currentDepth, int maxDepth) {
    // 限制深度
    if (currentDepth >= maxDepth) {
        return;
    }

    resource.listChildren().forEachRemaining(child -> {
        result.add(child);
        // 递归处理子节点
        getChildrenRecursive(child, result, currentDepth + 1, maxDepth);
    });
}
```

## 📊 代码优化

### 示例 8：避免不必要的适配

```java
// ❌ 错误：重复适配
public void processResource(Resource resource) {
    Page page = resource.adaptTo(Page.class);
    String title = page != null ? page.getTitle() : "";
    
    // 如果前面已经适配过，这里再次适配是浪费
    ValueMap valueMap = resource.adaptTo(ValueMap.class);
    String description = valueMap.get("jcr:description", "");
}

// ✅ 正确：重用适配结果
public void processResource(Resource resource) {
    // 一次性适配
    ValueMap valueMap = resource.getValueMap();
    
    String title = valueMap.get("jcr:title", "");
    String description = valueMap.get("jcr:description", "");
    
    // 如果需要 Page 对象，再适配
    if (needsPageObject) {
        Page page = resource.adaptTo(Page.class);
        // 使用 page...
    }
}
```

### 示例 9：使用 ValueMap 而不是 Node

```java
// ❌ 错误：使用 Node（更重）
public String getProperty(Resource resource) {
    Node node = resource.adaptTo(Node.class);
    if (node != null && node.hasProperty("title")) {
        return node.getProperty("title").getString();
    }
    return null;
}

// ✅ 正确：使用 ValueMap（更轻量）
public String getProperty(Resource resource) {
    ValueMap valueMap = resource.getValueMap();
    return valueMap.get("title", String.class);
}
```

## 🔑 性能优化最佳实践

### 1. 查询优化
- ✅ 使用索引属性进行查询
- ✅ 限制查询结果数量
- ✅ 避免在循环中查询
- ✅ 使用参数化查询

### 2. 缓存策略
- ✅ 缓存计算结果
- ✅ 设置合理的缓存过期时间
- ✅ 在内容更新时清除相关缓存

### 3. 资源优化
- ✅ 延迟加载不必要的数据
- ✅ 限制资源遍历深度
- ✅ 使用 ValueMap 而不是 Node

### 4. 代码优化
- ✅ 避免重复适配
- ✅ 使用合适的数据结构
- ✅ 减少不必要的对象创建

## 📈 性能监控

### 示例 10：性能日志

```java
public class PerformanceLogger {
    
    public static void logExecutionTime(String operation, Runnable task) {
        long startTime = System.currentTimeMillis();
        
        try {
            task.run();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            if (duration > 1000) { // 超过 1 秒记录警告
                Logger.warn("Slow operation: {} took {}ms", operation, duration);
            } else {
                Logger.debug("Operation: {} took {}ms", operation, duration);
            }
        }
    }
}

// 使用示例
PerformanceLogger.logExecutionTime("loadPages", () -> {
    List<Page> pages = pageService.getPages();
    // 处理页面...
});
```

## 🔑 关键要点

1. **查询优化**：使用索引、限制结果、避免循环查询
2. **缓存策略**：合理使用缓存减少重复计算
3. **资源优化**：延迟加载、限制深度
4. **代码优化**：避免不必要的适配和对象创建
5. **性能监控**：记录和分析性能指标

## ➡️ 下一步

学习更多性能优化技术，如 CDN 配置、图像优化等。

