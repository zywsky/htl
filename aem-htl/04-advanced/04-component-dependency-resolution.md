# AEM 组件依赖解析机制详解

## 概述

在 AEM 中，当一个 HTL 组件需要渲染时，Sling 框架负责找到并加载该组件的所有依赖项，包括：
- CSS 和 JavaScript (ClientLibs)
- 子组件（通过 data-sly-resource）
- Java 类（Sling Models、Use API）
- 模板和其他资源

本文档详细解释 Sling 如何发现和解析这些依赖。

---

## 1. AEM 组件解析和渲染的完整系统流程

### 1.0 从请求到渲染：系统级视角

当浏览器请求一个 AEM 页面时，系统会经历一个复杂的多阶段处理流程。本节深入解析这个完整的系统级流程。

#### 完整的请求处理管道

```
HTTP 请求
  ↓
Dispatcher (可选缓存检查)
  ├── 检查缓存键 (URL + 查询参数 + 请求头)
  ├── 检查缓存规则 (可缓存 vs 不可缓存)
  ├── 缓存命中 → 直接返回缓存的 HTML (不访问 AEM)
  └── 缓存未命中 → 转发到 AEM Publisher
  ↓
Apache Sling Engine
  ↓
Request Processing Pipeline
  ├── Authentication (Sling Authentication) - 用户身份验证
  ├── Authorization (ACLs 检查) - 权限验证
  ├── Sling Mappings (URL 重写) - 路径映射
  ├── Resource Resolution (JCR 查找)
  ├── Script Resolution (脚本查找)
  ├── Rendering Execution (渲染执行)
  └── Response Generation (响应生成)
  ↓
Dispatcher 缓存层 (如果可缓存)
  ├── 根据缓存规则决定是否缓存
  ├── 设置缓存头 (TTL, Cache-Control)
  └── 存储到文件系统或内存
  ↓
HTTP 响应
```

#### 阶段 0: Dispatcher 缓存机制（前置阶段）

**Dispatcher 的工作流程**:

Dispatcher 是 AEM 架构中的反向代理和缓存层，位于 AEM 实例之前：

```apache
# Dispatcher 配置文件示例 (dispatcher.any)

/farms
{
  /website
  {
    /clientheaders
    {
      "Accept-Language"
      "Authorization"
      "Cache-Control"
    }
    
    /virtualhosts
    {
      "www.example.com"
      "example.com"
    }
    
    /renders
    {
      /render1
      {
        /hostname "localhost"
        /port "4503"
      }
    }
    
    /filter
    {
      /0001 { /type "deny" /url "*" }
      /0002 { /type "allow" /url "/content/*" }
      /0003 { /type "allow" /url "/etc.clientlibs/*" }
      /0004 { /type "deny" /url "*.html" /method "POST" }
    }
    
    /cache
    {
      /docroot "/var/cache/dispatcher"
      /rules
      {
        /0000 { /type "deny" /glob "*" }
        /0001 { /type "allow" /glob "/content/*" }
      }
      
      /invalidate
      {
        /0000 { /type "allow" /glob "*.html" }
        /0001 { /type "deny" /glob "*" }
      }
      
      /allowedClients
      {
        /0001 { /type "allow" /glob "127.0.0.1" }
      }
    }
  }
}
```

**缓存键的生成**:

```java
// Dispatcher 缓存键生成逻辑（概念性）
public String generateCacheKey(HttpServletRequest request) {
    StringBuilder key = new StringBuilder();
    
    // 1. URL 路径
    key.append(request.getRequestURI());
    
    // 2. 查询参数（如果允许缓存）
    if (isQueryStringCacheable(request)) {
        String queryString = request.getQueryString();
        if (queryString != null) {
            key.append("?").append(queryString);
        }
    }
    
    // 3. 请求头（特定头部，如 Accept-Language）
    String acceptLanguage = request.getHeader("Accept-Language");
    if (acceptLanguage != null) {
        key.append("|lang:").append(acceptLanguage);
    }
    
    // 4. 生成哈希（可选）
    return hash(key.toString());
}
```

**缓存规则的判断**:

```java
// 判断请求是否可缓存
public boolean isCacheable(HttpServletRequest request) {
    String path = request.getRequestURI();
    
    // 1. 检查 HTTP 方法（只有 GET 和 HEAD 可缓存）
    String method = request.getMethod();
    if (!"GET".equals(method) && !"HEAD".equals(method)) {
        return false;
    }
    
    // 2. 检查路径规则
    if (!matchesCacheRules(path)) {
        return false;
    }
    
    // 3. 检查查询参数（某些查询参数使请求不可缓存）
    if (hasNonCacheableQueryParams(request)) {
        return false;
    }
    
    // 4. 检查请求头（某些头部使请求不可缓存）
    if (hasNonCacheableHeaders(request)) {
        return false;
    }
    
    return true;
}
```

**缓存失效机制**:

```java
// AEM 内容更新时的缓存失效
@Component(service = {})
public class CacheInvalidationService {
    
    @Reference
    private FlushAgent flushAgent;
    
    @Activate
    protected void activate() {
        // 监听内容更新事件
        resourceChangeListener.onChange(events -> {
            for (ResourceChangeEvent event : events) {
                if (event.getType() == ResourceChangeEvent.Type.CHANGED) {
                    invalidateCache(event.getPath());
                }
            }
        });
    }
    
    private void invalidateCache(String path) {
        // 构建失效路径列表
        List<String> invalidationPaths = buildInvalidationPaths(path);
        
        // 发送失效请求到 Dispatcher
        for (String invalidationPath : invalidationPaths) {
            flushAgent.flush(invalidationPath);
        }
    }
    
    private List<String> buildInvalidationPaths(String path) {
        List<String> paths = new ArrayList<>();
        
        // 1. 页面路径
        paths.add(path + ".html");
        
        // 2. 父页面路径（如果组件在页面中）
        String parentPage = getParentPagePath(path);
        if (parentPage != null) {
            paths.add(parentPage + ".html");
        }
        
        // 3. 相关页面（如果有引用关系）
        paths.addAll(getReferencedPages(path));
        
        return paths;
    }
}
```

#### 阶段 1.1: Authentication 和 Authorization 机制

**Authentication（身份验证）流程**:

在资源解析之前，Sling 首先验证用户身份：

```java
// Sling Authentication 的处理流程（概念性）
public class SlingAuthenticationHandler {
    
    public AuthenticationInfo authenticate(HttpServletRequest request) {
        // 1. 从请求中提取认证信息
        //    - Cookie (登录令牌)
        //    - Authorization Header (Basic Auth)
        //    - 请求参数
        
        String authToken = extractAuthToken(request);
        
        // 2. 验证令牌
        if (authToken != null) {
            UserInfo userInfo = validateToken(authToken);
            if (userInfo != null) {
                return new AuthenticationInfo(
                    userInfo.getUserId(),
                    userInfo.getCredentials(),
                    request
                );
            }
        }
        
        // 3. 匿名用户
        return AuthenticationInfo.ANONYMOUS;
    }
}
```

**Authorization（授权）检查**:

资源解析后，检查用户是否有权限访问：

```java
// ACL (Access Control List) 检查
public class AuthorizationChecker {
    
    public boolean isAuthorized(Resource resource, AuthenticationInfo authInfo) {
        // 1. 获取资源的 ACL 信息
        AccessControlList acl = getAccessControlList(resource);
        
        // 2. 检查用户的权限
        Principal principal = authInfo.getPrincipal();
        
        // 3. 检查读取权限
        return acl.hasPermission(principal, Permission.READ);
    }
    
    private AccessControlList getAccessControlList(Resource resource) {
        // 从 JCR 节点的 rep:policy 获取 ACL
        // 或从父节点继承 ACL
        Node node = resource.adaptTo(Node.class);
        return accessControlManager.getPolicies(node.getPath());
    }
}
```

**权限检查对组件渲染的影响**:

```java
// 如果用户没有权限，组件渲染会被阻止
Resource resource = resourceResolver.resolve(path);

// 检查权限
if (!authorizationChecker.isAuthorized(resource, authenticationInfo)) {
    // 返回 403 Forbidden 或隐藏组件
    return null;  // 或不渲染
}

// 继续渲染流程
```

#### 阶段 1: 请求接收和预处理

**Apache Sling 的请求处理入口**:

```java
// Sling 请求处理的入口点（简化示例）
public class SlingMainServlet extends HttpServlet {
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        // 1. 创建 Sling 请求对象
        SlingHttpServletRequest slingRequest = 
            new SlingHttpServletRequestImpl(request, servletContext);
        
        // 2. 获取 ResourceResolver（关键对象）
        ResourceResolver resourceResolver = 
            slingRequest.getResourceResolver();
        
        // 3. 开始资源解析流程
        Resource resource = resourceResolver.resolve(request);
        
        // 4. 执行渲染
        // ...
    }
}
```

**ResourceResolver 的创建**:

```java
// ResourceResolver 通过 ResourceResolverFactory 创建
// 它维护了 JCR Session 和资源查找的上下文

ResourceResolver resolver = resourceResolverFactory.getResourceResolver(
    authenticationInfo  // 包含用户认证信息
);

// ResourceResolver 内部维护：
// - JCR Session: 用于访问 JCR 仓库
// - Mappings: URL 到 JCR 路径的映射
// - Search Paths: 资源查找的搜索路径
// - Cache: 资源对象缓存
```

**ResourceResolver 的生命周期管理**:

ResourceResolver 是一个有状态的对象，需要正确管理其生命周期：

```java
// ✅ 正确使用 ResourceResolver
try (ResourceResolver resolver = resourceResolverFactory.getResourceResolver(authInfo)) {
    Resource resource = resolver.resolve(path);
    // 使用 resource
    // ResourceResolver 会在 try-with-resources 结束时自动关闭
}

// ❌ 错误：忘记关闭 ResourceResolver
ResourceResolver resolver = resourceResolverFactory.getResourceResolver(authInfo);
Resource resource = resolver.resolve(path);
// 如果没有关闭，会导致 JCR Session 泄漏
```

**ResourceResolver.close() 的重要性**:

```java
// ResourceResolver.close() 的内部操作
public void close() {
    // 1. 关闭 JCR Session
    if (session != null && session.isLive()) {
        session.logout();
    }
    
    // 2. 清理缓存
    resourceCache.clear();
    
    // 3. 释放其他资源
    cleanup();
}
```

**在生产环境中，ResourceResolver 泄漏会导致**:
- JCR Session 耗尽
- 内存泄漏
- 性能下降
- 最终导致 OutOfMemoryError

```

#### 阶段 2: Resource Resolution（资源解析）

**URL 分解机制**:

Sling 将 HTTP 请求的 URL 分解为多个部分：

```
完整 URL: /content/my-site/en/page.print.html
  ↓
分解为：
  ├── 路径 (Path): /content/my-site/en/page
  ├── 选择器 (Selector): print
  ├── 扩展 (Extension): html
  └── 方法 (Method): GET (从 HTTP 方法获取)
```

**URL 分解的详细过程**:

```java
// Sling URL 分解的实现（概念性）
public class SlingUrlParser {
    
    public ParsedUrl parseUrl(String requestUri, String method) {
        ParsedUrl parsed = new ParsedUrl();
        
        // 1. 分离路径和扩展
        int dotIndex = requestUri.lastIndexOf('.');
        if (dotIndex > 0) {
            String beforeDot = requestUri.substring(0, dotIndex);
            String afterDot = requestUri.substring(dotIndex + 1);
            
            // 2. 检查是否有选择器（多个点的情况）
            int selectorIndex = beforeDot.lastIndexOf('.');
            if (selectorIndex > 0) {
                parsed.path = beforeDot.substring(0, selectorIndex);
                parsed.selector = beforeDot.substring(selectorIndex + 1);
            } else {
                parsed.path = beforeDot;
            }
            
            parsed.extension = afterDot;
        } else {
            parsed.path = requestUri;
        }
        
        parsed.method = method;
        
        return parsed;
    }
}

// 示例：
// /content/page.print.html → path: /content/page, selector: print, extension: html
// /content/page.html → path: /content/page, selector: null, extension: html
// /content/page → path: /content/page, selector: null, extension: null
```

**选择器的作用**:

选择器用于请求同一资源的不同表示形式：

```
/content/page.html          → 标准 HTML 视图
/content/page.print.html    → 打印版本
/content/page.json         → JSON 数据
/content/page.model.json   → Sling Model 导出的 JSON
```

**路径解析流程**:

```
请求路径: /content/my-site/en/page/jcr:content/hero
  ↓
1. 检查 ResourceResolver 的映射规则
  ├── /content/my-site/en → /content/my-site/en (直接映射)
  ├── /libs → /libs (默认映射)
  └── /apps → /apps (默认映射)
  ↓
2. 在 JCR 仓库中查找节点
  ├── 从根路径开始: /content/my-site/en
  ├── 查找子节点: page
  ├── 查找子节点: jcr:content
  └── 查找子节点: hero
  ↓
3. 找到 JCR 节点后创建 Resource 对象
  └── Resource resource = new JcrResource(jcrNode, resourceResolver)
```

**资源查找的回退机制**:

如果找不到精确匹配的资源，Sling 会尝试回退：

```
1. 尝试完整路径: /content/page.print.html
   ↓ (如果失败)
2. 移除选择器: /content/page.html
   ↓ (如果失败)
3. 移除扩展: /content/page
   ↓ (如果失败)
4. 查找父节点: /content
   ↓ (如果失败)
5. 返回 404
```

**ResourceResolver.resolve() 的内部实现**（概念性）：

```java
public Resource resolve(HttpServletRequest request) {
    String requestPath = request.getPathInfo();
    
    // 1. 应用 Sling Mappings
    String mappedPath = applyMappings(requestPath);
    
    // 2. 查找 JCR 节点
    Node node = findJcrNode(mappedPath);
    
    if (node == null) {
        // 3. 检查虚拟资源（如 /etc/map）
        Resource virtualResource = checkVirtualResources(mappedPath);
        if (virtualResource != null) {
            return virtualResource;
        }
        return null;  // 404
    }
    
    // 4. 创建 Resource 对象包装 JCR 节点
    Resource resource = createResource(node);
    
    // 5. 缓存 Resource 对象（避免重复查找）
    cacheResource(mappedPath, resource);
    
    return resource;
}
```

**Resource 对象的关键属性**:

```java
// Resource 对象包含的信息
public interface Resource {
    String getPath();                    // JCR 路径
    String getName();                    // 节点名称
    ResourceResolver getResourceResolver(); // 解析器引用
    
    // 关键方法：获取 Resource Type
    String getResourceType();            // 从 jcr:primaryType 或 sling:resourceType 获取
    
    // 获取属性值
    <T> T getValueMap(Class<T> type);   // 获取节点属性为 ValueMap
    
    // 子资源
    Iterable<Resource> getChildren();    // 获取子节点
    Resource getChild(String relPath);   // 获取指定子节点
    
    // 适配
    <T> T adaptTo(Class<T> type);       // 适配到其他类型（如 Node, ValueMap）
}
```

#### 阶段 3: Resource Type 解析

**Resource Type 的确定顺序**:

```
1. 检查节点的 sling:resourceType 属性
   ↓ (如果存在，使用此值)
   └── 例如: sling:resourceType = "myapp/components/hero"

2. 如果不存在，使用 jcr:primaryType
   ↓ (对于组件节点)
   └── 例如: jcr:primaryType = "cq:Component"

3. 如果还找不到，检查 Resource Super Type
   ↓ (如果设置了 sling:resourceSuperType)
   └── 递归查找 Super Type 的 Resource Type

4. 如果还找不到，检查父节点的 resourceType
   ↓ (向上遍历父节点)
   └── 递归向上查找

5. 最终确定 Resource Type
   └── resourceType = "myapp/components/hero"
```

**Resource Super Type 的递归查找机制**:

```java
// Resource Super Type 的递归解析（概念性）
public String getResourceType(Resource resource, Set<String> visited) {
    String path = resource.getPath();
    
    // 防止循环引用
    if (visited.contains(path)) {
        return null;
    }
    visited.add(path);
    
    ValueMap properties = resource.getValueMap();
    
    // 1. 检查 sling:resourceType
    String resourceType = properties.get("sling:resourceType", String.class);
    if (resourceType != null && !resourceType.isEmpty()) {
        return resourceType;
    }
    
    // 2. 检查 Resource Super Type
    String resourceSuperType = properties.get("sling:resourceSuperType", String.class);
    if (resourceSuperType != null) {
        // 递归查找 Super Type 的 Resource Type
        Resource superTypeResource = resourceResolver.resolve("/apps/" + resourceSuperType);
        if (superTypeResource != null) {
            String superType = getResourceType(superTypeResource, visited);
            if (superType != null) {
                return superType;
            }
        }
    }
    
    // 3. 检查 jcr:primaryType
    String primaryType = properties.get("jcr:primaryType", String.class);
    if (primaryType != null) {
        // 对于组件节点，使用路径作为 Resource Type
        if ("cq:Component".equals(primaryType)) {
            return resource.getPath();
        }
    }
    
    // 4. 向上遍历父节点
    Resource parent = resource.getParent();
    if (parent != null) {
        return getResourceType(parent, visited);
    }
    
    return "nt:unstructured";  // 默认值
}
```

**Resource Super Type 的查找示例**:

```
组件路径: /content/my-site/en/page/jcr:content/hero
  ↓
1. 检查 hero 节点的 sling:resourceType
   └── 不存在
  ↓
2. 检查 hero 节点的 sling:resourceSuperType
   └── "myapp/components/base"
  ↓
3. 查找 Super Type 的 Resource Type
   ├── 路径: /apps/myapp/components/base
   ├── 检查 base 节点的 sling:resourceType
   │   └── "myapp/components/base" (找到)
   └── 返回: "myapp/components/base"
  ↓
4. 最终 Resource Type: "myapp/components/base"
   └── 但脚本查找时会先检查 hero.html，再检查 base.html
```

**Resource Type 解析代码示例**:

```java
public String getResourceType(Resource resource) {
    // 1. 检查 sling:resourceType 属性
    String resourceType = resource.getValueMap().get("sling:resourceType", String.class);
    if (resourceType != null && !resourceType.isEmpty()) {
        return resourceType;
    }
    
    // 2. 检查 jcr:primaryType
    String primaryType = resource.getValueMap().get("jcr:primaryType", String.class);
    if (primaryType != null) {
        // 对于组件节点，可能需要特殊处理
        if ("cq:Component".equals(primaryType)) {
            // 组件节点的默认 resourceType 就是其路径
            return resource.getPath();
        }
    }
    
    // 3. 检查 Resource Super Type
    String resourceSuperType = resource.getValueMap().get("sling:resourceSuperType", String.class);
    if (resourceSuperType != null) {
        return resourceSuperType;
    }
    
    // 4. 默认值
    return "nt:unstructured";  // 默认节点类型
}
```

#### 阶段 4: Script Resolution（脚本解析）

**脚本查找的完整算法**:

```
Resource Type: "myapp/components/hero"
  ↓
Script 查找路径（按优先级）:

1. /apps/myapp/components/hero/hero.html
2. /apps/myapp/components/hero/hero.jsp
3. /apps/myapp/components/hero/hero.esp
  ↓ (如果都找不到，检查 Super Type)
4. /apps/myapp/components/base/base.html  (如果 resourceSuperType = "myapp/components/base")
5. /libs/myapp/components/base/base.html
  ↓ (如果还找不到，使用默认)
6. /libs/cq/Page/Page.jsp  (对于页面)
7. /libs/sling/servlet/default  (默认 Servlet)
```

**ScriptResolver 的内部实现**（概念性）：

```java
public Script resolveScript(Resource resource, String method) {
    String resourceType = resource.getResourceType();
    
    // 1. 构建脚本路径列表
    List<String> scriptPaths = buildScriptPaths(resourceType, method);
    
    // 2. 按优先级查找脚本
    for (String scriptPath : scriptPaths) {
        Resource scriptResource = resourceResolver.resolve(scriptPath);
        if (scriptResource != null) {
            // 3. 检查脚本是否存在且可访问
            if (canAccess(scriptResource, resource)) {
                return createScript(scriptResource);
            }
        }
    }
    
    // 4. 如果找不到，返回 null 或默认脚本
    return getDefaultScript(resource);
}

private List<String> buildScriptPaths(String resourceType, String method) {
    List<String> paths = new ArrayList<>();
    
    // 优先级 1: /apps/{resourceType}/{method}.html
    paths.add("/apps/" + resourceType + "/" + method + ".html");
    
    // 优先级 2: /apps/{resourceType}/{method}.jsp
    paths.add("/apps/" + resourceType + "/" + method + ".jsp");
    
    // 优先级 3: 检查 Super Type
    // (需要递归查找)
    
    // 优先级 4: /libs/{resourceType}/{method}.html
    paths.add("/libs/" + resourceType + "/" + method + ".html");
    
    return paths;
}
```

**脚本查找的搜索路径（Search Paths）**:

```java
// ResourceResolver 维护搜索路径列表
// 默认搜索路径：
List<String> searchPaths = Arrays.asList(
    "/apps",    // 应用程序层（最高优先级）
    "/libs"     // 核心库层
);

// 查找脚本时会按搜索路径顺序查找
for (String searchPath : searchPaths) {
    String scriptPath = searchPath + "/" + resourceType + "/" + method + ".html";
    Resource script = resourceResolver.resolve(scriptPath);
    if (script != null) {
        return script;
    }
}
```

**脚本查找的完整回退机制**:

```
Resource Type: "myapp/components/hero"
Method: "GET"
Selector: null
Extension: "html"

查找顺序：
1. /apps/myapp/components/hero/GET.html
2. /apps/myapp/components/hero/hero.html
3. /apps/myapp/components/hero/html.html
4. /apps/myapp/components/hero/hero.jsp
5. /apps/myapp/components/hero/html.jsp
  ↓ (如果都找不到，检查 Resource Super Type)
6. /apps/myapp/components/base/base.html (如果 resourceSuperType = "myapp/components/base")
7. /libs/myapp/components/base/base.html
  ↓ (如果还找不到，使用默认)
8. /libs/cq/Page/Page.jsp (对于页面)
9. /libs/sling/servlet/default (默认 Servlet)
```

**脚本文件名的确定规则**:

```java
// 脚本文件名的确定逻辑
public String determineScriptName(Resource resource, String method, String selector, String extension) {
    String componentName = resource.getName();
    
    // 1. 优先使用组件名称
    // 例如: hero.html
    
    // 2. 如果有选择器，尝试带选择器的脚本
    // 例如: hero.print.html
    
    // 3. 使用扩展名
    // 例如: hero.html, hero.json
    
    // 4. 使用 HTTP 方法
    // 例如: GET.html, POST.html
    
    // 5. 默认脚本名
    // 例如: component.html, template.html
    
    List<String> candidates = new ArrayList<>();
    
    if (selector != null) {
        candidates.add(componentName + "." + selector + "." + extension);
    }
    candidates.add(componentName + "." + extension);
    candidates.add(method + "." + extension);
    candidates.add("component." + extension);
    
    return candidates;
}
```

#### 阶段 5: Script 编译和缓存

**HTL 脚本的编译过程**:

```
HTL 模板文件 (hero.html)
  ↓
1. 语法解析 (HTL Compiler)
  ├── 解析 data-sly-* 指令
  ├── 解析 ${} 表达式
  └── 构建抽象语法树 (AST)
  ↓
2. Java 代码生成
  ├── 生成 Java 类
  ├── 实现 Use API 调用
  └── 生成渲染方法
  ↓
3. Java 编译
  ├── 编译为 .class 文件
  └── 加载到 JVM
  ↓
4. 缓存编译结果
  └── ScriptCache 中缓存编译后的类
```

**脚本缓存机制**:

```java
// ScriptCache 维护编译后的脚本
public class ScriptCache {
    private Map<String, CompiledScript> cache = new ConcurrentHashMap<>();
    
    public CompiledScript getScript(String scriptPath, long lastModified) {
        String cacheKey = scriptPath + ":" + lastModified;
        
        CompiledScript script = cache.get(cacheKey);
        if (script != null) {
            return script;  // 缓存命中
        }
        
        // 缓存未命中，编译脚本
        script = compileScript(scriptPath);
        cache.put(cacheKey, script);
        
        return script;
    }
}
```

#### 阶段 6: 渲染执行（Rendering Execution）

**Sling Scripting 的执行流程**:

```
1. 创建 Binding（绑定上下文）
   ├── request: SlingHttpServletRequest
   ├── response: SlingHttpServletResponse
   ├── resource: Resource (当前资源)
   ├── resourceResolver: ResourceResolver
   ├── properties: ValueMap (节点属性，来自 _cq_dialog 配置的字段)
   ├── currentNode: Node (JCR 节点)
   ├── pageManager: PageManager
   ├── component: Component (组件对象)
   ├── currentPage: Page (当前页面)
   └── ... (其他绑定对象)

2. 执行脚本
   ├── HTL: 调用编译后的 Java 类的 doProcess 方法
   ├── JSP: 通过 JSP Engine 执行
   └── ESP: 通过 Rhino 执行 JavaScript

3. 处理 Use API 调用
   ├── data-sly-use → 实例化 Java 对象
   ├── 注入依赖（Resource, Request 等）
   └── 缓存 Use API 对象（如果可能）

4. 处理 ClientLib 引用
   ├── 解析 categories
   ├── 查找 ClientLib 配置
   ├── 收集 CSS/JS 文件
   └── 生成 <link> 和 <script> 标签

5. 处理子组件（data-sly-resource）
   ├── 递归解析子组件
   ├── 创建子请求（SlingRequestDispatcher）
   └── 包含子组件的输出

6. 输出 HTML
   └── 写入 Response
```

**Parsys（段落系统）的顺序渲染**:

Parsys 是 AEM 中的容器组件，按 JCR 节点顺序渲染子组件：

```html
<!-- Parsys 组件的 HTL 模板（简化示例） -->
<div class="parsys">
    <div data-sly-list="${resource.children}"
         data-sly-resource="${item.path}">
        <!-- 每个子组件按 JCR 顺序渲染 -->
    </div>
</div>
```

**Parsys 渲染的详细过程**:

```
1. 获取 Parsys 资源
   └── Resource parsys = resource.getChild("par")

2. 获取子节点列表（按 JCR 顺序）
   └── List<Resource> children = getChildren(parsys)

3. 按顺序渲染每个子组件
   ├── 子组件 1: /content/page/jcr:content/par/text
   │   ├── 解析 Resource Type
   │   ├── 查找脚本
   │   ├── 执行渲染
   │   └── 输出 HTML
   ├── 子组件 2: /content/page/jcr:content/par/image
   │   └── (相同流程)
   └── 子组件 3: /content/page/jcr:content/par/hero
       └── (相同流程)

4. 合并所有子组件的输出
   └── 按顺序插入到 Parsys 的 HTML 中
```

**JCR 节点顺序的重要性**:

```java
// Parsys 获取子节点的顺序（按 JCR 索引）
public List<Resource> getChildren(Resource parsys) {
    List<Resource> children = new ArrayList<>();
    
    // JCR 节点的顺序由节点的索引决定
    // 在 AEM 编辑器中，作者可以拖拽调整顺序
    // 顺序存储在节点的 jcr:primaryType 和索引中
    
    Node parsysNode = parsys.adaptTo(Node.class);
    NodeIterator childNodes = parsysNode.getNodes();
    
    while (childNodes.hasNext()) {
        Node childNode = childNodes.nextNode();
        Resource childResource = resourceResolver.getResource(childNode.getPath());
        children.add(childResource);
    }
    
    return children;  // 按 JCR 顺序返回
}
```

**渲染模式的影响**:

AEM 支持不同的渲染模式，影响组件的输出：

```
1. 编辑模式 (Edit Mode)
   ├── 显示编辑占位符
   ├── 添加编辑工具栏
   ├── 显示空状态提示
   └── 启用拖放功能

2. 预览模式 (Preview Mode)
   ├── 正常渲染内容
   ├── 不显示编辑工具
   └── 接近发布模式

3. 发布模式 (Publish Mode)
   ├── 最终用户看到的版本
   ├── 无编辑功能
   └── 优化的输出
```

**模式检测**:

```html
<!-- HTL 中检测渲染模式 -->
<sly data-sly-use.wcmmode="${'com.adobe.cq.wcm.core.components.util.WCMUtils' @ request=request}"></sly>

<div data-sly-test="${wcmmode.isEditMode(request)}" class="edit-mode">
    <!-- 编辑模式特定内容 -->
</div>

<div data-sly-test="${wcmmode.isPreviewMode(request)}" class="preview-mode">
    <!-- 预览模式特定内容 -->
</div>

<div data-sly-test="${wcmmode.isDisabled(request)}" class="publish-mode">
    <!-- 发布模式内容 -->
</div>
```

**HTL 渲染执行的详细过程**:

```java
// HTL 编译后的 Java 类（简化示例）
public class hero_html extends AbstractHTLTemplate {
    
    public void doProcess(SlingHttpServletRequest request,
                         SlingHttpServletResponse response,
                         Bindings bindings) throws IOException {
        
        // 1. 从 bindings 获取上下文对象
        Resource resource = (Resource) bindings.get("resource");
        ValueMap properties = resource.getValueMap();
        
        // 2. 处理 data-sly-use
        // <sly data-sly-use.model="${'com.myapp.HeroModel'}" />
        UseAPI model = getUseAPI("com.myapp.HeroModel", resource, request);
        bindings.put("model", model);
        
        // 3. 处理 data-sly-use.clientlib
        // <sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}" />
        UseAPI clientlib = getUseAPI("/libs/granite/sightly/templates/clientlib.html", resource, request);
        bindings.put("clientlib", clientlib);
        
        // 4. 开始输出 HTML
        response.getWriter().write("<div class=\"hero\">");
        
        // 5. 处理表达式
        // ${model.title}
        String title = model.get("title");
        response.getWriter().write("<h1>" + escapeHtml(title) + "</h1>");
        
        // 6. 处理 data-sly-resource
        // <div data-sly-resource="${'content' @ resourceType='...'}" />
        Resource childResource = resource.getChild("content");
        if (childResource != null) {
            RequestDispatcher dispatcher = request.getRequestDispatcher(childResource);
            dispatcher.include(request, response);
        }
        
        // 7. 处理 ClientLib
        // <sly data-sly-call="${clientlib.css @ categories='myapp.hero'}" />
        clientlib.call("css", "categories", "myapp.hero");
        
        response.getWriter().write("</div>");
    }
}
```

#### 阶段 7: Use API 对象实例化

**Use API 的实例化过程**:

```
data-sly-use.model="${'com.myapp.HeroModel'}"
  ↓
1. 解析类名: "com.myapp.HeroModel"
  ↓
2. 检查是否为 OSGi 服务
  ├── 如果是 @ProviderType → 从 Service Registry 获取
  └── 如果不是 → 继续步骤 3
  ↓
3. 类加载
  ├── 在当前 Bundle 的类路径中查找
  ├── 加载类文件
  └── 检查是否为 Sling Model
  ↓
4. Sling Model 适配
  ├── 如果使用 @Model 注解
  ├── 从 Resource 或 Request 适配
  ├── 执行依赖注入 (@Inject)
  └── 调用 @PostConstruct 方法
  ↓
5. 创建实例
  └── 缓存实例（如果可能）
```

**Sling Model 适配的详细过程**:

```java
// Sling Models 的适配器工厂（简化示例）
public class SlingModelAdapterFactory implements AdapterFactory {
    
    @Override
    public <T> T getAdapter(Object adaptable, Class<T> type) {
        // 1. 检查类型是否匹配
        if (!(adaptable instanceof Resource || adaptable instanceof SlingHttpServletRequest)) {
            return null;
        }
        
        // 2. 查找对应的 Model 实现类
        ModelImplementation modelImpl = findModelImplementation(type);
        if (modelImpl == null) {
            return null;
        }
        
        // 3. 创建实例
        T instance = createInstance(modelImpl, adaptable);
        
        // 4. 依赖注入
        injectDependencies(instance, adaptable);
        
        // 5. 调用 @PostConstruct
        callPostConstruct(instance);
        
        return instance;
    }
    
    private void injectDependencies(Object instance, Object adaptable) {
        Class<?> clazz = instance.getClass();
        
        // 扫描所有字段和方法
        for (Field field : clazz.getDeclaredFields()) {
            // @Inject 注解的字段
            if (field.isAnnotationPresent(Inject.class)) {
                Object value = getInjectedValue(field, adaptable);
                field.setAccessible(true);
                field.set(instance, value);
            }
            
            // @OSGiService 注解的字段
            if (field.isAnnotationPresent(OSGiService.class)) {
                Object service = getOSGiService(field.getType());
                field.setAccessible(true);
                field.set(instance, service);
            }
        }
    }
}
```

#### 阶段 8: ClientLib 处理

**ClientLib 的查找和生成过程**:

```
clientlib.css @ categories='myapp.hero'
  ↓
1. 解析 categories: "myapp.hero"
  ↓
2. 查找 ClientLib 配置
   ├── 查询: SELECT * FROM [cq:ClientLibraryFolder] WHERE categories = 'myapp.hero'
   ├── 搜索路径: /apps, /libs
   └── 找到: /apps/myapp/clientlibs/components/hero
  ↓
3. 读取 .content.xml 配置
   ├── categories: [myapp.hero]
   ├── dependencies: [myapp.base]
   └── allowProxy: true
  ↓
4. 递归处理依赖
   ├── 处理 myapp.base
   └── 递归处理 myapp.base 的依赖
  ↓
5. 收集 CSS 文件
   ├── /apps/myapp/clientlibs/components/hero/css/hero.css
   └── /apps/myapp/clientlibs/base/css/base.css
  ↓
6. 合并和压缩（如果启用）
   └── 生成单个 CSS 文件
  ↓
7. 生成代理路径
   └── /etc.clientlibs/myapp/clientlibs/components/hero.css
  ↓
8. 输出 <link> 标签
   └── <link rel="stylesheet" href="/etc.clientlibs/.../hero.css" />
```

**ClientLib 查找的 JCR 查询**:

```java
// ClientLib 查找的 JCR 查询（概念性）
String query = "SELECT * FROM [cq:ClientLibraryFolder] " +
               "WHERE categories = '" + category + "' " +
               "OR categories LIKE '%" + category + "%'";

QueryManager queryManager = session.getWorkspace().getQueryManager();
Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
QueryResult result = jcrQuery.execute();

NodeIterator nodes = result.getNodes();
while (nodes.hasNext()) {
    Node clientLibNode = nodes.nextNode();
    // 处理找到的 ClientLib
    processClientLibrary(clientLibNode);
}
```

#### 阶段 9: 子组件递归渲染

**data-sly-resource 的处理**:

```
data-sly-resource="${'content' @ resourceType='myapp/components/text'}"
  ↓
1. 解析相对路径: "content"
  ├── 相对于当前 Resource
  └── 完整路径: /content/.../hero/content
  ↓
2. 获取子资源
   ├── Resource childResource = resource.getChild("content")
   └── 如果指定了 resourceType，覆盖默认类型
  ↓
3. 创建子请求（SlingRequestDispatcher）
   ├── RequestDispatcher dispatcher = 
   │      request.getRequestDispatcher(childResource)
   └── 设置 resourceType（如果指定）
  ↓
4. 递归执行渲染流程
   ├── 解析子组件的 Resource Type
   ├── 查找子组件的脚本
   ├── 执行子组件的脚本
   └── 输出子组件的 HTML
  ↓
5. 包含输出
   └── dispatcher.include(request, response)
```

**RequestDispatcher.include() 的内部实现**（概念性）：

```java
public void include(SlingHttpServletRequest request,
                   SlingHttpServletResponse response) {
    
    // 1. 保存当前上下文
    Resource originalResource = request.getResource();
    String originalResourceType = request.getResourceType();
    
    try {
        // 2. 设置新的资源上下文
        request.setResource(childResource);
        if (resourceType != null) {
            request.setResourceType(resourceType);
        }
        
        // 3. 解析子组件的脚本
        Script script = scriptResolver.resolveScript(childResource, "GET");
        
        // 4. 创建新的绑定上下文
        Bindings bindings = createBindings(request, response, childResource);
        
        // 5. 执行脚本
        script.eval(bindings);
        
    } finally {
        // 6. 恢复原始上下文
        request.setResource(originalResource);
        request.setResourceType(originalResourceType);
    }
}
```

#### 阶段 10: 响应生成和输出

**最终 HTML 输出的组装**:

```
1. 主组件的 HTML 输出
   ├── 开始标签
   ├── 属性（data-sly-attribute）
   └── 内容
  ↓
2. 子组件的 HTML 输出（递归包含）
   └── 插入到主组件中
  ↓
3. ClientLib 的 <link> 和 <script> 标签
   ├── 收集所有需要的 ClientLibs
   ├── 去重
   └── 按依赖顺序排序
  ↓
4. 最终 HTML 文档
   └── 写入 HttpServletResponse
```

**响应输出的代码示例**（概念性）：

```java
// 最终输出过程
PrintWriter writer = response.getWriter();

// 1. 输出 HTML 头部（如果还没有输出）
if (!response.isCommitted()) {
    writer.write("<!DOCTYPE html>\n<html>\n<head>\n");
}

// 2. 输出 ClientLib CSS
for (String cssUrl : collectedCssUrls) {
    writer.write("<link rel=\"stylesheet\" href=\"" + cssUrl + "\" />\n");
}

// 3. 输出组件 HTML
writer.write(componentHtml);

// 4. 输出 ClientLib JS（在 body 末尾）
for (String jsUrl : collectedJsUrls) {
    writer.write("<script src=\"" + jsUrl + "\"></script>\n");
}

writer.flush();
```

### 1.1 AEM 组件的完整结构（cq:Component）

**组件节点的完整定义**:

AEM 组件在 JCR 中定义为 `cq:Component` 类型的节点，位于 `/apps/<your-app>/components/<component-name>` 下。这个节点包含了组件的所有信息：

```
/apps/myapp/components/hero/
  ├── .content.xml              # 组件定义（jcr:primaryType = "cq:Component"）
  ├── hero.html                 # HTL 渲染脚本
  ├── _cq_dialog/               # 编辑对话框（内容作者使用）
  │   └── .content.xml
  ├── _cq_design_dialog/        # 设计对话框（模板设计者使用）
  │   └── .content.xml
  ├── _cq_editConfig/           # 编辑配置（编辑模式行为）
  │   └── .content.xml
  ├── _cq_htmlTag/              # HTML 标签配置
  │   └── .content.xml
  └── _cq_childEditConfig/      # 子组件编辑配置（容器组件）
      └── .content.xml
```

**组件节点的关键属性**:

```xml
<!-- .content.xml 示例 -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          jcr:primaryType="cq:Component"
          jcr:title="Hero Component"
          jcr:description="Hero banner component with image and text"
          sling:resourceSuperType="myapp/components/base"
          componentGroup="MyApp - Content"
          cq:isContainer="{Boolean}false"/>
```

**关键属性说明**:

| 属性 | 类型 | 说明 | React 迁移映射 |
|------|------|------|---------------|
| `jcr:title` | String | 组件显示名称 | 组件文档中的名称 |
| `jcr:description` | String | 组件描述 | 组件文档说明 |
| `sling:resourceType` | String | 组件类型标识（通常等于路径） | React 组件 ID/路径 |
| `sling:resourceSuperType` | String | 父组件路径（继承） | React 组件继承/组合 |
| `componentGroup` | String | 组件分组 | React 组件分类 |
| `cq:isContainer` | Boolean | 是否为容器组件 | React 容器组件标识 |
| `allowParents` | String[] | 允许的父组件路径列表（限制哪些父组件可以包含此组件） | React 组件的父组件类型限制 |

**子节点详解**:

#### 1. _cq_dialog（编辑对话框）

定义内容作者可以编辑的字段：

```xml
<!-- _cq_dialog/.content.xml 示例 -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          jcr:primaryType="nt:unstructured"
          jcr:title="Hero"
          sling:resourceType="cq/gui/components/authoring/dialog">
    <content jcr:primaryType="nt:unstructured"
             sling:resourceType="granite/ui/components/coral/foundation/container">
        <items jcr:primaryType="nt:unstructured">
            <tabs jcr:primaryType="nt:unstructured"
                  sling:resourceType="granite/ui/components/coral/foundation/tabs">
                <items jcr:primaryType="nt:unstructured">
                    <text jcr:primaryType="nt:unstructured"
                          jcr:title="Text"
                          sling:resourceType="granite/ui/components/coral/foundation/container">
                        <items jcr:primaryType="nt:unstructured">
                            <title jcr:primaryType="nt:unstructured"
                                   sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                                   fieldLabel="Title"
                                   name="./title"/>
                            <description jcr:primaryType="nt:unstructured"
                                         sling:resourceType="granite/ui/components/coral/foundation/form/textarea"
                                         fieldLabel="Description"
                                         name="./description"/>
                        </items>
                    </text>
                </items>
            </tabs>
        </items>
    </content>
</jcr:root>
```

**对话框字段到 React Props 的映射**:

```javascript
// 对话框字段定义
{
  "title": {
    "sling:resourceType": "granite/ui/components/coral/foundation/form/textfield",
    "name": "./title",
    "fieldLabel": "Title"
  },
  "description": {
    "sling:resourceType": "granite/ui/components/coral/foundation/form/textarea",
    "name": "./description",
    "fieldLabel": "Description"
  }
}

// 映射到 React Props
interface HeroProps {
  title?: string;        // 来自 ./title 字段
  description?: string;  // 来自 ./description 字段
}
```

#### 2. _cq_design_dialog（设计对话框）

用于模板设计时配置，影响组件的样式和行为：

```xml
<!-- _cq_design_dialog/.content.xml 示例 -->
<jcr:root jcr:primaryType="nt:unstructured"
          jcr:title="Hero Design"
          sling:resourceType="cq/gui/components/authoring/dialog">
    <content>
        <items>
            <styleSystem jcr:primaryType="nt:unstructured"
                         sling:resourceType="granite/ui/components/coral/foundation/form/checkbox"
                         name="./styleSystem"
                         text="Enable Style System"/>
        </items>
    </content>
</jcr:root>
```

#### 3. _cq_editConfig（编辑配置）

控制编辑模式下的行为：

```xml
<!-- _cq_editConfig/.content.xml 示例 -->
<jcr:root jcr:primaryType="cq:EditConfig">
    <cq:dropTargets jcr:primaryType="nt:unstructured">
        <image jcr:primaryType="cq:DropTargetConfig"
               accept="[image/.*]"
               groups="[media]"
               propertyName="./imagePath"/>
    </cq:dropTargets>
    <cq:inplaceEditing jcr:primaryType="cq:InplaceEditingConfig"
                       active="{Boolean}true"
                       editorType="plaintext"
                       propertyName="./text"/>
</jcr:root>
```

**编辑配置的关键功能**:
- **拖放目标** (`cq:dropTargets`): 允许从 Assets 浏览器拖放资源
- **就地编辑** (`cq:inplaceEditing`): 允许在页面上直接编辑内容
- **工具栏** (`cq:toolbar`): 自定义编辑工具栏

#### 4. _cq_childEditConfig（子组件编辑配置）

用于容器组件，定义子组件的编辑行为：

```xml
<!-- _cq_childEditConfig/.content.xml 示例 -->
<jcr:root jcr:primaryType="cq:ChildEditConfig">
    <cq:listeners jcr:primaryType="cq:EditListenersConfig"
                  afterdelete="REFRESH_PAGE"
                  afteredit="REFRESH_PAGE"
                  afterinsert="REFRESH_PAGE"/>
</jcr:root>
```

#### 5. allowParents 属性（组件父容器限制）

`allowParents` 属性用于限制当前组件只能被哪些父组件包含。这是 AEM 中控制组件嵌套规则的重要机制。

**核心概念**:

`allowParents` 定义了当前组件可以出现在哪些父组件内部。在 AEM 编辑器中，当作者尝试将组件拖放到某个容器中时，系统会检查该容器的类型是否在 `allowParents` 列表中。如果不在，组件将无法被添加到该容器中。

**配置示例**:

```xml
<!-- /apps/myapp/components/hero/.content.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          jcr:primaryType="cq:Component"
          jcr:title="Hero Component"
          sling:resourceSuperType="myapp/components/base"
          componentGroup="MyApp - Content"
          allowParents="[myapp/components/page,myapp/components/section]"/>
```

**属性说明**:
- **类型**: String[]（字符串数组）
- **格式**: 组件路径列表，使用方括号 `[]` 包裹，多个路径用逗号分隔
- **作用**: 限制当前组件只能被指定的父组件包含

**实际应用场景**:

**场景 1: 限制 Hero 组件只能在特定容器中使用**

```xml
<!-- Hero 组件只能放在 page 或 section 组件中 -->
<jcr:root jcr:primaryType="cq:Component"
          jcr:title="Hero Banner"
          allowParents="[myapp/components/page,myapp/components/section]"/>
```

**场景 2: 限制 Footer 组件只能在 Page 根组件中使用**

```xml
<!-- Footer 组件只能放在页面根组件中 -->
<jcr:root jcr:primaryType="cq:Component"
          jcr:title="Footer"
          allowParents="[myapp/components/page]"/>
```

**场景 3: 允许在多个容器类型中使用**

```xml
<!-- Card 组件可以在多种容器中使用 -->
<jcr:root jcr:primaryType="cq:Component"
          jcr:title="Card"
          allowParents="[myapp/components/section,myapp/components/container,foundation/components/parsys]"/>
```

**工作原理**:

```
1. 作者在 AEM 编辑器中拖放组件
   ↓
2. AEM 检查目标容器的 resourceType
   例如: /content/page/jcr:content/container
        → resourceType = "myapp/components/container"
   ↓
3. AEM 检查被拖放组件的 allowParents 属性
   例如: Hero 组件的 allowParents = ["myapp/components/page", "myapp/components/section"]
   ↓
4. 比较目标容器的 resourceType 是否在 allowParents 列表中
   ↓
5. 如果匹配 → 允许添加
   如果不匹配 → 禁止添加，显示错误提示
```

**Java 代码实现（概念性）**:

```java
public boolean canAddComponent(Resource parentContainer, Component childComponent) {
    String parentResourceType = parentContainer.getResourceType();
    String[] allowedParents = childComponent.getProperty("allowParents", String[].class);
    
    if (allowedParents == null || allowedParents.length == 0) {
        // 如果没有限制，允许添加
        return true;
    }
    
    // 检查父容器类型是否在允许列表中
    for (String allowedParent : allowedParents) {
        if (parentResourceType.equals(allowedParent)) {
            return true;
        }
    }
    
    return false;
}
```

**与 allowedChildren 的区别**:

| 属性 | 位置 | 作用 | 示例 |
|------|------|------|------|
| **allowParents** | 子组件上 | 限制当前组件只能被哪些父组件包含 | Hero 组件: `allowParents="[page,section]"` |
| **allowedChildren** | 父组件上（通过 Policy） | 限制容器组件可以包含哪些子组件 | Container 组件 Policy: `allowedChildren="[card,text]"` |

**与 sling:resourceSuperType 的区别**:

这两个属性虽然都涉及"父组件"的概念，但作用完全不同：

| 属性 | 作用方向 | 目的 | 作用时机 | 示例 |
|------|---------|------|---------|------|
| **sling:resourceSuperType** | 向上继承 | 从父组件继承功能（代码复用） | 组件定义和渲染时 | `sling:resourceSuperType="myapp/components/base"` |
| **allowParents** | 向下限制 | 限制当前组件只能被哪些父组件包含（使用限制） | 编辑器拖放组件时 | `allowParents="[myapp/components/page]"` |

**核心区别说明**:

1. **sling:resourceSuperType（组件继承）**:
   - **作用**: 让当前组件继承另一个组件的功能（脚本、对话框、配置等）
   - **关系**: "我继承谁" - 定义组件的功能来源
   - **时机**: 组件渲染时，Sling 会查找父组件的脚本、对话框等资源
   - **结果**: 代码复用，减少重复代码
   - **示例**: Hero 组件继承 Base 组件，可以使用 Base 组件的脚本和对话框

2. **allowParents（使用限制）**:
   - **作用**: 限制当前组件只能被哪些容器组件包含
   - **关系**: "我可以被谁包含" - 定义组件的使用限制
   - **时机**: 在 AEM 编辑器中拖放组件时，检查是否允许
   - **结果**: 控制页面结构，确保组件使用的合理性
   - **示例**: Hero 组件只能放在 Page 组件中，不能放在其他容器中

**实际应用示例**:

```xml
<!-- Hero 组件配置示例 -->
<jcr:root jcr:primaryType="cq:Component"
          jcr:title="Hero Component"
          sling:resourceSuperType="myapp/components/base"
          allowParents="[myapp/components/page]"/>
```

这个配置的含义：
- `sling:resourceSuperType="myapp/components/base"`: Hero 组件继承 Base 组件的所有功能
- `allowParents="[myapp/components/page]"`: Hero 组件只能被 Page 组件包含

**三个属性的完整对比**:

| 属性 | 位置 | 作用方向 | 目的 | 使用场景 |
|------|------|---------|------|---------|
| **sling:resourceSuperType** | 子组件上 | 向上继承 | 代码复用 | 创建组件变体，复用基础功能 |
| **allowParents** | 子组件上 | 向下限制 | 使用限制 | 限制组件只能用在特定容器中 |
| **allowedChildren** | 父组件上（Policy） | 向上限制 | 包含限制 | 限制容器只能包含特定子组件 |

**在现代 AEM（Editable Templates）中的使用**:

在 Editable Templates 中，组件限制通常通过 **模板策略（Policy）** 来配置，而不是直接在组件上设置 `allowParents`：

```
模板策略配置路径:
/content/my-site/templates/home-page/policies/
  └── myapp/components/container/
      └── default/
          └── policy.json
```

策略配置示例:

```json
{
  "allowedChildren": [
    "myapp/components/card",
    "myapp/components/text",
    "myapp/components/image"
  ]
}
```

**React 迁移映射**:

```typescript
// AEM 配置
// Hero 组件的 .content.xml
allowParents="[myapp/components/page,myapp/components/section]"

// React 映射方案 1: TypeScript 类型约束
type AllowedParentType = PageComponent | SectionComponent;

interface HeroProps {
  // ...
}

// 在父组件中定义子组件类型
interface PageComponentProps {
  children?: React.ReactElement<HeroProps> | React.ReactElement<HeroProps>[];
}

interface SectionComponentProps {
  children?: React.ReactElement<HeroProps> | React.ReactElement<HeroProps>[];
}

// React 映射方案 2: 运行时检查
function Hero(props: HeroProps) {
  const parentContext = useContext(ParentComponentContext);
  
  const allowedParents = ['PageComponent', 'SectionComponent'];
  if (parentContext && !allowedParents.includes(parentContext.type)) {
    console.warn(`Hero component should only be used in: ${allowedParents.join(', ')}`);
  }
  
  return <div className="hero">...</div>;
}
```

**最佳实践**:

1. **合理使用限制**: 只在必要时使用 `allowParents`，过度限制会影响组件复用性
2. **使用语义化的组件名**: 使用清晰的组件路径，如 `myapp/components/page` 而不是 `component1`
3. **文档化限制**: 在组件文档中说明组件的使用限制
4. **测试验证**: 在编辑器中测试组件限制是否按预期工作
5. **考虑 Editable Templates**: 在 AEM 6.3+ 中，优先使用模板策略（Policy）而不是组件属性

**常见问题**:

**Q: 如果不设置 `allowParents` 会怎样？**
A: 组件可以在任何容器中使用（除非父容器有 `allowedChildren` 限制）

**Q: `allowParents` 和模板策略（Policy）的 `allowedChildren` 哪个优先级高？**
A: 两者都生效，组件必须同时满足：
- 组件的 `allowParents` 包含父容器类型
- 父容器的 Policy `allowedChildren` 包含当前组件类型

**Q: 可以使用通配符吗？**
A: 不可以，必须指定完整的组件路径

**使用 CRX/DE Lite 查看组件结构**:

在 AEM 中，可以使用 CRX/DE Lite 工具查看组件的完整结构：

```
访问: http://localhost:4502/crx/de/index.jsp

导航到: /apps/myapp/components/hero

可以查看：
- 组件的所有属性（包括 allowParents）
- 子节点结构
- 对话框配置
- 脚本文件内容
```

**组件结构在 React 迁移中的重要性**:

```
AEM 组件结构          →  React 组件结构
─────────────────────────────────────────
cq:Component          →  Component 类/函数
_cq_dialog            →  Props 接口定义
hero.html             →  JSX 渲染逻辑
sling:resourceType    →  组件 ID/路径
sling:resourceSuperType →  组件继承/组合
```

### 1.1.1 组件的完整组成详解

一个 AEM 组件由多个不同类型的文件和资源组成。以下是完整的组件组成结构：

#### 完整的组件文件结构

```
/apps/myapp/components/hero/
  ├── .content.xml                    # 组件定义文件（必需）
  │
  ├── hero.html                        # HTL 渲染脚本（主要）
  ├── hero.jsp                         # JSP 脚本（备选）
  ├── hero.esp                         # ECMAScript 脚本（备选）
  │
  ├── _cq_dialog/                      # 编辑对话框配置
  │   └── .content.xml
  │
  ├── _cq_design_dialog/               # 设计对话框配置
  │   └── .content.xml
  │
  ├── _cq_editConfig/                  # 编辑模式配置
  │   └── .content.xml
  │
  ├── _cq_htmlTag/                     # HTML 标签配置
  │   └── .content.xml
  │
  ├── _cq_childEditConfig/             # 子组件编辑配置
  │   └── .content.xml
  │
  ├── _cq_template/                    # 组件模板（Editable Templates）
  │   └── .content.xml
  │
  ├── thumbnail.png                    # 组件缩略图（可选）
  │
  └── i18n/                            # 国际化资源（可选）
      ├── en.json
      └── zh-CN.json
```

#### 组件各部分的详细说明

**1. 组件定义文件（.content.xml）**

这是组件的核心配置文件，定义了组件的基本属性：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          jcr:primaryType="cq:Component"
          jcr:title="Hero Component"
          jcr:description="Hero banner component"
          sling:resourceSuperType="myapp/components/base"
          componentGroup="MyApp - Content"
          cq:isContainer="{Boolean}false"
          cq:noDecoration="{Boolean}false"/>
```

**查找机制**:
- 路径：`/apps/myapp/components/hero/.content.xml`
- 直接通过 JCR 路径访问
- 在组件节点创建时自动读取

**2. 渲染脚本（HTL/JSP/ESP）**

组件的渲染逻辑，按优先级查找：

**HTL 脚本（hero.html）**:
```html
<!-- /apps/myapp/components/hero/hero.html -->
<sly data-sly-use.model="${'com.myapp.HeroModel'}"></sly>
<div class="hero">
    <h1>${model.title}</h1>
</div>
```

**查找机制**:
```
1. 确定 Resource Type: "myapp/components/hero"
2. 构建脚本路径列表（按优先级）:
   a. /apps/myapp/components/hero/hero.html
   b. /apps/myapp/components/hero/hero.jsp
   c. /apps/myapp/components/hero/hero.esp
   d. /libs/myapp/components/hero/hero.html
3. 检查 resourceSuperType（如果当前组件未找到脚本）:
   a. /apps/myapp/components/base/hero.html
   b. /apps/myapp/components/base/base.html
4. 返回第一个找到的脚本
```

**查找代码实现**:
```java
// ScriptResolver 的查找逻辑
public Script resolveScript(Resource resource, String method) {
    String resourceType = resource.getResourceType();
    List<String> searchPaths = Arrays.asList("/apps", "/libs");
    List<String> scriptExtensions = Arrays.asList(".html", ".jsp", ".esp");
    
    // 1. 在当前组件中查找
    for (String searchPath : searchPaths) {
        for (String extension : scriptExtensions) {
            String scriptName = resource.getName() + extension;
            String scriptPath = searchPath + "/" + resourceType + "/" + scriptName;
            Resource scriptResource = resourceResolver.resolve(scriptPath);
            if (scriptResource != null) {
                return createScript(scriptResource);
            }
        }
    }
    
    // 2. 检查 resourceSuperType
    String superType = getResourceSuperType(resource);
    if (superType != null) {
        return resolveScriptForSuperType(superType, method);
    }
    
    return null;
}
```

**3. Java 类（Sling Models / Use API）**

**Sling Model 类**:
```java
// com.myapp.models.HeroModel.java
@Model(adaptables = Resource.class,
       resourceType = "myapp/components/hero")
public interface HeroModel {
    @Inject
    String getTitle();
    
    @Inject
    String getDescription();
}
```

**查找机制**:
```
1. HTL 中使用: data-sly-use.model="${'com.myapp.HeroModel'}"
2. 解析类名: "com.myapp.HeroModel"
3. 查找顺序:
   a. 检查是否为 OSGi 服务（@ProviderType）
   b. 在 Bundle 类路径中查找类
   c. 检查是否为 Sling Model（@Model 注解）
   d. 从 Resource 适配到 Model 接口
4. 创建实例并注入依赖
```

**查找代码实现**:
```java
// Use API 对象查找
public Object getUseAPIObject(String className, Resource resource, Request request) {
    // 1. 尝试作为 OSGi 服务
    ServiceReference<?>[] refs = bundleContext.getServiceReferences(className, null);
    if (refs != null && refs.length > 0) {
        return bundleContext.getService(refs[0]);
    }
    
    // 2. 加载类
    Class<?> modelClass = loadClass(className);
    
    // 3. 检查是否为 Sling Model
    if (modelClass.isAnnotationPresent(Model.class)) {
        Model modelAnnotation = modelClass.getAnnotation(Model.class);
        
        // 4. 检查 adaptables 类型
        Class<?>[] adaptables = modelAnnotation.adaptables();
        if (Arrays.asList(adaptables).contains(Resource.class)) {
            return resource.adaptTo(modelClass);
        }
        if (Arrays.asList(adaptables).contains(SlingHttpServletRequest.class)) {
            return request.adaptTo(modelClass);
        }
    }
    
    // 5. 使用反射创建实例
    return createInstance(modelClass, resource, request);
}
```

**4. CSS 样式（ClientLibs）**

CSS 文件通过 ClientLibs 机制管理，不直接存储在组件目录下：

```
/apps/myapp/clientlibs/components/hero/
  ├── .content.xml                    # ClientLib 配置
  ├── css/
  │   └── hero.css                    # CSS 文件
  └── js/
      └── hero.js                     # JavaScript 文件
```

**查找机制**:
```
1. HTL 中使用: data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"
2. 解析 categories: "myapp.components.hero"
3. JCR 查询查找 ClientLib:
   SELECT * FROM [cq:ClientLibraryFolder] 
   WHERE categories = 'myapp.components.hero'
4. 查找路径:
   a. /apps/myapp/clientlibs/**/.content.xml
   b. /libs/myapp/clientlibs/**/.content.xml
5. 读取 .content.xml 获取配置:
   - categories: [myapp.components.hero]
   - dependencies: [myapp.base]
   - 文件路径: css/hero.css
6. 递归处理依赖
7. 合并所有 CSS 文件
8. 生成代理路径: /etc.clientlibs/myapp/clientlibs/components/hero.css
```

**查找代码实现**:
```java
// ClientLib 查找
public ClientLibrary findClientLibrary(String category) {
    // 1. JCR 查询
    String query = "SELECT * FROM [cq:ClientLibraryFolder] " +
                   "WHERE categories = '" + category + "'";
    QueryResult result = executeQuery(query);
    
    // 2. 遍历结果
    NodeIterator nodes = result.getNodes();
    while (nodes.hasNext()) {
        Node clientLibNode = nodes.nextNode();
        
        // 3. 读取配置
        String[] categories = clientLibNode.getProperty("categories").getValues();
        if (Arrays.asList(categories).contains(category)) {
            // 4. 收集 CSS/JS 文件
            List<String> cssFiles = collectFiles(clientLibNode, "css");
            List<String> jsFiles = collectFiles(clientLibNode, "js");
            
            // 5. 处理依赖
            if (clientLibNode.hasProperty("dependencies")) {
                String[] deps = clientLibNode.getProperty("dependencies").getValues();
                for (String dep : deps) {
                    ClientLibrary depLib = findClientLibrary(dep);
                    // 合并依赖的文件
                }
            }
            
            return new ClientLibrary(categories, cssFiles, jsFiles);
        }
    }
    
    return null;
}
```

**5. JavaScript 文件（ClientLibs）**

JavaScript 文件也通过 ClientLibs 管理，查找机制与 CSS 相同。

**6. 对话框配置（_cq_dialog）**

**查找机制**:
```
1. 组件路径: /apps/myapp/components/hero
2. 对话框路径: /apps/myapp/components/hero/_cq_dialog
3. 查找顺序:
   a. 检查当前组件是否有 _cq_dialog
   b. 如果不存在，检查 resourceSuperType 的 _cq_dialog
   c. 递归向上查找继承链
4. 读取 .content.xml 解析对话框结构
```

**查找代码实现**:
```java
// 对话框查找
public Resource findDialog(Resource componentResource) {
    // 1. 在当前组件中查找
    Resource dialog = componentResource.getChild("_cq_dialog");
    if (dialog != null) {
        return dialog;
    }
    
    // 2. 检查 resourceSuperType
    String superType = componentResource.getValueMap()
        .get("sling:resourceSuperType", String.class);
    if (superType != null) {
        Resource superComponent = resourceResolver.resolve("/apps/" + superType);
        if (superComponent != null) {
            return findDialog(superComponent);  // 递归查找
        }
    }
    
    return null;
}
```

**7. 编辑配置（_cq_editConfig）**

**查找机制**:
```
1. 路径: /apps/myapp/components/hero/_cq_editConfig
2. 查找顺序与对话框相同（当前组件 → resourceSuperType）
3. 解析配置:
   - cq:dropTargets: 拖放目标配置
   - cq:inplaceEditing: 就地编辑配置
   - cq:toolbar: 工具栏配置
```

**8. 国际化资源（i18n）**

**查找机制**:
```
1. 从请求中获取语言: Accept-Language 或 wcmmode
2. 构建 i18n 路径:
   a. /apps/myapp/components/hero/i18n/en.json
   b. /apps/myapp/components/hero/i18n/zh-CN.json
3. 回退机制:
   a. 如果组件级 i18n 不存在，查找应用级: /apps/myapp/i18n/en.json
   b. 如果应用级不存在，查找全局: /libs/myapp/i18n/en.json
4. 使用 i18n.get() 或 ${'key' @ i18n} 访问
```

**查找代码实现**:
```java
// i18n 资源查找
public String getI18nString(String key, String locale, Resource componentResource) {
    // 1. 组件级 i18n
    Resource i18nResource = componentResource.getChild("i18n/" + locale + ".json");
    if (i18nResource != null) {
        ValueMap i18nMap = i18nResource.getValueMap();
        String value = i18nMap.get(key, String.class);
        if (value != null) {
            return value;
        }
    }
    
    // 2. 应用级 i18n
    String appPath = getAppPath(componentResource);
    Resource appI18n = resourceResolver.resolve(appPath + "/i18n/" + locale + ".json");
    if (appI18n != null) {
        ValueMap i18nMap = appI18n.getValueMap();
        String value = i18nMap.get(key, String.class);
        if (value != null) {
            return value;
        }
    }
    
    // 3. 全局 i18n
    Resource globalI18n = resourceResolver.resolve("/libs/myapp/i18n/" + locale + ".json");
    if (globalI18n != null) {
        ValueMap i18nMap = globalI18n.getValueMap();
        return i18nMap.get(key, key);  // 返回 key 作为默认值
    }
    
    return key;  // 最终回退
}
```

**9. 静态资源（图片、字体等）**

静态资源通常存储在 `/content/dam` 或组件目录下：

```
/apps/myapp/components/hero/
  └── assets/
      ├── default-image.jpg
      └── icon.svg
```

**查找机制**:
```
1. 相对路径: 相对于组件路径
   ${'assets/default-image.jpg' @ resourceType='myapp/components/hero'}
   → /apps/myapp/components/hero/assets/default-image.jpg

2. 绝对路径: 直接使用
   ${'/content/dam/myapp/hero-image.jpg'}

3. 通过 Sling Mappings 映射
```

**10. 模板文件（data-sly-template）**

模板文件可以存储在组件目录或共享位置：

```
/apps/myapp/components/hero/
  └── templates/
      └── card-template.html
```

**查找机制**:
```
1. HTL 中使用: data-sly-call="${templates.cardTemplate @ title='...'}"
2. 查找顺序:
   a. 当前组件: /apps/myapp/components/hero/templates/card-template.html
   b. 共享模板: /apps/myapp/templates/card-template.html
   c. 全局模板: /libs/myapp/templates/card-template.html
3. 解析模板参数并渲染
```

#### 渲染时的完整查找流程

当渲染一个组件时，Sling 按以下顺序查找所有需要的资源：

```
请求: /content/my-site/en/page/jcr:content/hero
  ↓
1. 资源解析
   ResourceResolver.resolve("/content/my-site/en/page/jcr:content/hero")
   → 找到 JCR 节点
   → 创建 Resource 对象
  ↓
2. Resource Type 解析
   从 Resource 获取 sling:resourceType
   → "myapp/components/hero"
  ↓
3. 组件定义查找
   /apps/myapp/components/hero/.content.xml
   → 读取组件属性
   → 获取 resourceSuperType（如果存在）
  ↓
4. 渲染脚本查找
   按优先级查找:
   a. /apps/myapp/components/hero/hero.html
   b. /apps/myapp/components/hero/hero.jsp
   c. 检查 resourceSuperType
   → 找到脚本并编译
  ↓
5. Use API 对象查找（如果 HTL 中使用 data-sly-use）
   解析类名: "com.myapp.HeroModel"
   → 查找 OSGi 服务或 Sling Model
   → 从 Resource 适配到 Model
   → 执行依赖注入
  ↓
6. ClientLib 查找（如果 HTL 中使用 clientlib）
   解析 categories: "myapp.components.hero"
   → JCR 查询查找 ClientLib 配置
   → 收集 CSS/JS 文件
   → 处理依赖
   → 生成代理路径
  ↓
7. 对话框查找（编辑模式）
   /apps/myapp/components/hero/_cq_dialog
   → 如果不存在，查找 resourceSuperType
   → 解析对话框结构
  ↓
8. 编辑配置查找（编辑模式）
   /apps/myapp/components/hero/_cq_editConfig
   → 解析编辑行为配置
  ↓
9. 子组件查找（如果使用 data-sly-resource）
   对每个子组件递归执行步骤 1-8
  ↓
10. 国际化资源查找（如果使用 i18n）
    根据语言查找 i18n 文件
    → 组件级 → 应用级 → 全局
  ↓
11. 静态资源查找（如果引用图片等）
    解析资源路径
    → 相对路径或绝对路径
  ↓
12. 模板查找（如果使用 data-sly-call）
    查找模板文件
    → 解析参数并渲染
  ↓
13. 合并所有输出
    → 生成最终 HTML
```

#### 查找优先级总结

| 资源类型 | 查找优先级 | 回退机制 |
|---------|----------|---------|
| **渲染脚本** | 1. 当前组件<br>2. resourceSuperType<br>3. /libs | 递归向上查找继承链 |
| **Java 类** | 1. OSGi 服务<br>2. Sling Model<br>3. 类路径 | 适配器模式 |
| **ClientLibs** | 1. /apps<br>2. /libs | JCR 查询 |
| **对话框** | 1. 当前组件<br>2. resourceSuperType | 递归向上查找 |
| **编辑配置** | 1. 当前组件<br>2. resourceSuperType | 递归向上查找 |
| **i18n** | 1. 组件级<br>2. 应用级<br>3. 全局 | 语言回退 |
| **静态资源** | 1. 相对路径<br>2. 绝对路径 | Sling Mappings |

#### 实际查找示例

**示例：渲染 Hero 组件**

```java
// 完整的组件查找和渲染过程
public void renderComponent(String contentPath) {
    // 1. 资源解析
    Resource resource = resourceResolver.resolve(contentPath);
    // → /content/my-site/en/page/jcr:content/hero
    
    // 2. Resource Type
    String resourceType = resource.getResourceType();
    // → "myapp/components/hero"
    
    // 3. 组件定义
    Resource componentDef = resourceResolver.resolve("/apps/" + resourceType);
    ValueMap componentProps = componentDef.getValueMap();
    String superType = componentProps.get("sling:resourceSuperType", String.class);
    // → "myapp/components/base"
    
    // 4. 渲染脚本
    Script script = scriptResolver.resolveScript(resource, "GET");
    // → /apps/myapp/components/hero/hero.html
    
    // 5. Use API（如果 HTL 中有 data-sly-use）
    if (scriptContainsUseAPI(script)) {
        String modelClass = extractModelClass(script);
        Object model = getUseAPIObject(modelClass, resource, request);
        // → com.myapp.HeroModel 实例
    }
    
    // 6. ClientLib（如果 HTL 中有 clientlib）
    if (scriptContainsClientLib(script)) {
        String[] categories = extractCategories(script);
        List<ClientLibrary> clientLibs = findClientLibraries(categories);
        // → [myapp.components.hero, myapp.base]
    }
    
    // 7. 对话框（编辑模式）
    if (isEditMode(request)) {
        Resource dialog = findDialog(componentDef);
        // → /apps/myapp/components/hero/_cq_dialog
    }
    
    // 8. 执行渲染
    script.eval(createBindings(request, response, resource));
    // → 输出 HTML
}
```

### 1.2 Sling 资源路径解析

当请求一个 AEM 组件时，Sling 使用以下步骤解析资源：

```
请求路径 → ResourceResolver → Resource → ResourceType → Script
```

1. **Resource Resolver**: 根据请求路径找到 JCR 节点（Resource）
2. **Resource Type 解析**: 从 Resource 的 `sling:resourceType` 属性获取组件类型
3. **Script 查找**: 根据 Resource Type 查找对应的渲染脚本（HTL/JSP）

#### 阶段 11: JCR 查询和索引机制

**JCR 查询在组件解析中的作用**:

AEM 使用 JCR 查询来高效查找资源，特别是在查找 ClientLibs 和组件时：

```java
// ClientLib 查找使用的查询（实际实现）
String query = "SELECT * FROM [cq:ClientLibraryFolder] " +
               "WHERE ISDESCENDANTNODE('/apps') " +
               "OR ISDESCENDANTNODE('/libs')";

// 对于 categories 的查找，使用属性查询
String categoryQuery = "SELECT * FROM [cq:ClientLibraryFolder] " +
                      "WHERE categories = '" + category + "'";

// JCR 查询执行
QueryManager queryManager = session.getWorkspace().getQueryManager();
Query jcrQuery = queryManager.createQuery(categoryQuery, Query.JCR_SQL2);
QueryResult result = jcrQuery.execute();
```

**JCR 索引的使用**:

为了提高查询性能，AEM 维护了多个索引：

```
1. Property Index（属性索引）
   - 索引节点的属性值
   - 例如：categories 属性的索引
   - 用于快速查找特定属性值的节点

2. Node Type Index（节点类型索引）
   - 索引节点的 jcr:primaryType
   - 用于快速查找特定类型的节点
   - 例如：查找所有 cq:ClientLibraryFolder 节点

3. Path Index（路径索引）
   - 索引节点路径
   - 用于快速查找特定路径下的节点

4. Fulltext Index（全文索引）
   - 索引节点内容的全文
   - 用于搜索功能
```

**索引配置示例**（Oak 索引配置）:

```json
{
  "jcr:primaryType": "oak:QueryIndexDefinition",
  "type": "property",
  "propertyNames": ["categories"],
  "async": "async",
  "reindex": false
}
```

#### 阶段 12: OSGi 服务注册和发现

**Sling Models 的 OSGi 服务注册**:

```java
// Sling Models 在 OSGi 中的注册过程
@Component(service = {})
public class SlingModelRegistration {
    
    // 在 Bundle 激活时，扫描所有 @Model 注解的类
    @Activate
    protected void activate(BundleContext bundleContext) {
        // 1. 扫描 Bundle 中的类
        List<Class<?>> modelClasses = scanForModelClasses(bundleContext);
        
        // 2. 为每个 Model 类注册 AdapterFactory
        for (Class<?> modelClass : modelClasses) {
            Model modelAnnotation = modelClass.getAnnotation(Model.class);
            
            // 3. 创建 AdapterFactory 服务
            AdapterFactory factory = new SlingModelAdapterFactory(modelClass, modelAnnotation);
            
            // 4. 注册 OSGi 服务
            ServiceRegistration<AdapterFactory> registration = 
                bundleContext.registerService(
                    AdapterFactory.class,
                    factory,
                    getServiceProperties(modelAnnotation)
                );
        }
    }
}
```

**Use API 对象的服务查找**:

```java
// Use API 对象实例化时的服务查找
public Object getUseAPIObject(String className, Resource resource, Request request) {
    BundleContext bundleContext = getBundleContext();
    
    // 1. 首先尝试作为 OSGi 服务查找
    ServiceReference<?>[] refs = bundleContext.getServiceReferences(className, null);
    if (refs != null && refs.length > 0) {
        // 找到服务，直接返回
        return bundleContext.getService(refs[0]);
    }
    
    // 2. 尝试作为 Sling Model 适配
    try {
        Class<?> modelClass = loadClass(className);
        if (modelClass.isAnnotationPresent(Model.class)) {
            return resource.adaptTo(modelClass);
        }
    } catch (ClassNotFoundException e) {
        // 类不存在
    }
    
    // 3. 使用反射创建实例
    return createInstance(className, resource, request);
}
```

#### 阶段 13: 缓存机制的详细实现

**多级缓存架构**:

```
Level 1: ResourceResolver 缓存
  ├── Resource 对象缓存
  ├── Resource Type 缓存
  └── 路径到节点的映射缓存

Level 2: Script 缓存
  ├── 编译后的 HTL 脚本缓存
  ├── Script 路径缓存
  └── 脚本修改时间缓存

Level 3: Use API 对象缓存
  ├── Sling Model 实例缓存（如果标记为可缓存）
  └── OSGi 服务引用缓存

Level 4: ClientLib 缓存
  ├── ClientLib 配置缓存
  ├── 合并后的 CSS/JS 文件缓存
  └── 依赖关系缓存

Level 5: Dispatcher 缓存
  ├── HTML 输出缓存
  └── 静态资源缓存
```

**ResourceResolver 缓存的实现**:

```java
// ResourceResolver 内部的缓存实现（概念性）
public class CachingResourceResolver implements ResourceResolver {
    private final Map<String, Resource> resourceCache = new ConcurrentHashMap<>();
    private final Map<String, String> resourceTypeCache = new ConcurrentHashMap<>();
    
    @Override
    public Resource resolve(String path) {
        // 1. 检查缓存
        Resource cached = resourceCache.get(path);
        if (cached != null && isValid(cached)) {
            return cached;
        }
        
        // 2. 缓存未命中，从 JCR 查找
        Resource resource = delegate.resolve(path);
        
        // 3. 缓存结果
        if (resource != null) {
            resourceCache.put(path, resource);
        }
        
        return resource;
    }
    
    private boolean isValid(Resource resource) {
        // 检查资源是否仍然有效
        // 例如：检查节点是否已被删除
        try {
            Node node = resource.adaptTo(Node.class);
            return node != null && node.getSession().nodeExists(node.getPath());
        } catch (RepositoryException e) {
            return false;
        }
    }
}
```

**Script 缓存的失效策略**:

```java
// Script 缓存的管理
public class ScriptCache {
    private final Map<String, CachedScript> cache = new ConcurrentHashMap<>();
    
    public CompiledScript getScript(String scriptPath) {
        CachedScript cached = cache.get(scriptPath);
        
        if (cached != null) {
            // 检查脚本文件是否已修改
            long currentModified = getFileModifiedTime(scriptPath);
            if (currentModified == cached.lastModified) {
                return cached.script;  // 缓存有效
            } else {
                // 脚本已修改，移除缓存
                cache.remove(scriptPath);
            }
        }
        
        // 重新编译脚本
        CompiledScript script = compileScript(scriptPath);
        long lastModified = getFileModifiedTime(scriptPath);
        cache.put(scriptPath, new CachedScript(script, lastModified));
        
        return script;
    }
}
```

#### 阶段 14: 性能优化的底层机制

**延迟加载（Lazy Loading）机制**:

```java
// Resource 的延迟加载实现
public class LazyResource implements Resource {
    private String path;
    private ResourceResolver resolver;
    private Resource delegate;  // 延迟初始化的实际资源
    
    @Override
    public ValueMap getValueMap() {
        // 首次访问时才加载
        if (delegate == null) {
            delegate = resolver.resolve(path);
        }
        return delegate.getValueMap();
    }
    
    @Override
    public Iterable<Resource> getChildren() {
        if (delegate == null) {
            delegate = resolver.resolve(path);
        }
        return delegate.getChildren();
    }
}
```

**批量操作优化**:

```java
// 批量获取资源属性（减少 JCR 访问次数）
public class BatchResourceLoader {
    public Map<String, ValueMap> batchLoadProperties(List<String> paths) {
        // 使用单个 JCR 查询获取所有需要的节点
        String query = buildBatchQuery(paths);
        QueryResult result = executeQuery(query);
        
        Map<String, ValueMap> results = new HashMap<>();
        NodeIterator nodes = result.getNodes();
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            results.put(node.getPath(), new JcrValueMap(node));
        }
        
        return results;
    }
}
```

#### 阶段 15: 调试和监控

**启用详细的解析日志**:

```bash
# 在 AEM 日志配置中启用 DEBUG 级别

# Resource Resolution 日志
org.apache.sling.resource.resolver DEBUG

# Script Resolution 日志
org.apache.sling.scripting DEBUG

# Sling Models 日志
org.apache.sling.models DEBUG

# HTL 编译日志
org.apache.sling.scripting.sightly DEBUG

# ClientLib 处理日志
com.day.cq.widget DEBUG
```

**使用 Sling Resource Resolver 的调试功能**:

```java
// 在代码中启用 ResourceResolver 的调试输出
ResourceResolver resolver = resourceResolverFactory.getResourceResolver(authInfo);

// 启用调试模式
if (logger.isDebugEnabled()) {
    resolver.getSearchPath();  // 输出搜索路径
    resolver.getAttributeNames();  // 输出属性
}

// 记录资源解析过程
logger.debug("Resolving resource: {}", path);
Resource resource = resolver.resolve(path);
logger.debug("Resolved to: {}", resource != null ? resource.getPath() : "null");
logger.debug("Resource Type: {}", resource != null ? resource.getResourceType() : "null");
```

**性能监控和指标收集**:

```java
// 在组件渲染过程中收集性能指标
public class PerformanceMonitoring {
    public void monitorComponentRendering(String componentPath, Runnable renderingTask) {
        long startTime = System.currentTimeMillis();
        
        try {
            renderingTask.run();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录指标
            recordMetric("component.render.time", duration, "ms");
            recordMetric("component.render.count", 1);
            
            if (duration > 100) {  // 超过 100ms
                logger.warn("Slow component rendering: {} took {}ms", componentPath, duration);
            }
        }
    }
}
```

#### 阶段 16: 完整的请求处理时序图

```
HTTP 请求到达
  │
  ├─→ [Dispatcher] 检查缓存
  │     ├─→ 缓存命中 → 返回缓存的 HTML
  │     └─→ 缓存未命中 → 继续
  │
  ├─→ [Sling Engine] 创建请求对象
  │     └─→ SlingHttpServletRequest
  │
  ├─→ [ResourceResolver] 解析路径
  │     ├─→ 应用 URL 映射
  │     ├─→ 查找 JCR 节点
  │     └─→ 创建 Resource 对象
  │
  ├─→ [Resource Type Resolver] 确定 Resource Type
  │     ├─→ 检查 sling:resourceType
  │     ├─→ 检查 jcr:primaryType
  │     └─→ 检查 resourceSuperType
  │
  ├─→ [Script Resolver] 查找脚本
  │     ├─→ 按搜索路径查找 (/apps, /libs)
  │     ├─→ 按优先级查找 (.html, .jsp, .esp)
  │     └─→ 检查脚本缓存
  │
  ├─→ [Script Compiler] 编译脚本（如果需要）
  │     ├─→ HTL → Java 类
  │     └─→ 缓存编译结果
  │
  ├─→ [Rendering Engine] 执行脚本
  │     ├─→ 创建 Bindings（上下文对象）
  │     ├─→ 处理 data-sly-use（Use API）
  │     │     ├─→ 查找 Sling Model
  │     │     ├─→ 依赖注入
  │     │     └─→ 调用 @PostConstruct
  │     ├─→ 处理 data-sly-resource（子组件）
  │     │     └─→ 递归执行（创建子请求）
  │     ├─→ 处理 clientlib（ClientLib）
  │     │     ├─→ 查询 ClientLib 配置
  │     │     ├─→ 收集依赖
  │     │     └─→ 生成链接标签
  │     └─→ 输出 HTML
  │
  └─→ [Response] 返回 HTML
        ├─→ 设置响应头
        ├─→ 写入 HTML 内容
        └─→ 关闭响应流
```

#### 阶段 17: 实际调试示例

**场景：调试组件无法正确渲染的问题**

假设一个组件 `/content/my-site/en/page/jcr:content/hero` 无法正确渲染，以下是系统级的调试步骤：

**步骤 1: 检查资源解析**

```java
// 在 Groovy Console 中执行
def path = "/content/my-site/en/page/jcr:content/hero"
def resource = resourceResolver.resolve(path)

println "Resource Path: ${resource?.path}"
println "Resource Exists: ${resource != null}"
println "Resource Type: ${resource?.resourceType}"
```

**步骤 2: 检查 Resource Type 解析**

```java
def resourceType = resource?.resourceType
println "Resource Type: ${resourceType}"

// 检查 Resource Type 属性
def properties = resource?.valueMap
println "sling:resourceType: ${properties['sling:resourceType']}"
println "jcr:primaryType: ${properties['jcr:primaryType']}"
println "sling:resourceSuperType: ${properties['sling:resourceSuperType']}"
```

**步骤 3: 检查脚本查找**

```java
def scriptPaths = [
    "/apps/${resourceType}/${resource.name}.html",
    "/apps/${resourceType}/${resource.name}.jsp",
    "/libs/${resourceType}/${resource.name}.html"
]

scriptPaths.each { scriptPath ->
    def scriptResource = resourceResolver.resolve(scriptPath)
    println "Script ${scriptPath}: ${scriptResource != null ? 'FOUND' : 'NOT FOUND'}"
    if (scriptResource) {
        println "  Last Modified: ${scriptResource.adaptTo(javax.jcr.Node).getProperty('jcr:lastModified').date}"
    }
}
```

**步骤 4: 检查 Use API 对象**

```java
// 检查 Sling Model 是否可用
def modelClass = "com.myapp.models.HeroModel"
try {
    def model = resource.adaptTo(Class.forName(modelClass))
    println "Sling Model ${modelClass}: ${model != null ? 'OK' : 'FAILED'}"
} catch (Exception e) {
    println "Sling Model ${modelClass}: ERROR - ${e.message}"
}
```

**步骤 5: 检查 ClientLib**

```java
// 查询 ClientLib
def queryManager = session.workspace.queryManager
def query = "SELECT * FROM [cq:ClientLibraryFolder] WHERE categories = 'myapp.hero'"
def result = queryManager.createQuery(query, javax.jcr.query.Query.JCR_SQL2).execute()

result.nodes.each { node ->
    println "ClientLib Found: ${node.path}"
    println "  Categories: ${node.getProperty('categories').values*.string}"
    println "  Dependencies: ${node.hasProperty('dependencies') ? node.getProperty('dependencies').values*.string : 'none'}"
}
```

**步骤 6: 检查渲染输出**

在浏览器中访问组件 URL 并查看：
- 响应状态码（200, 404, 500）
- HTML 输出内容
- 浏览器控制台错误
- 网络请求（ClientLib 是否加载）

#### 阶段 18: 系统级最佳实践

**1. 优化 Resource Resolution 性能**

```java
// ❌ 不好：每次调用都解析路径
public void processItems(List<String> paths) {
    for (String path : paths) {
        Resource resource = resourceResolver.resolve(path);  // 每次都查询 JCR
        // 处理资源
    }
}

// ✅ 好：批量解析或使用缓存
public void processItems(List<String> paths) {
    // 批量解析
    Map<String, Resource> resources = batchResolve(paths);
    for (String path : paths) {
        Resource resource = resources.get(path);
        // 处理资源
    }
}
```

**2. 优化 Script Resolution**

```java
// 确保脚本路径正确
// ✅ 好：使用明确的 Resource Type
sling:resourceType = "myapp/components/hero"

// ❌ 不好：依赖默认行为
// 不设置 sling:resourceType，依赖 jcr:primaryType
```

**3. 优化 Sling Model 性能**

```java
// ✅ 使用缓存注解
@Model(adaptables = Resource.class)
@Cache
public interface CachedModel {
    // Model 实例会被缓存
}

// ✅ 在 @PostConstruct 中预处理数据
@PostConstruct
protected void init() {
    // 一次性计算所有需要的数据
    this.processedData = expensiveCalculation();
}
```

**4. 优化 ClientLib 加载**

```xml
<!-- ✅ 明确声明依赖 -->
<jcr:root jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.hero]"
          dependencies="[myapp.base]"/>

<!-- ✅ 使用 allowProxy -->
<jcr:root jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.hero]"
          allowProxy="{Boolean}true"/>
```

**5. 监控和日志**

```java
// ✅ 添加性能监控
@Component(service = {})
public class ComponentPerformanceMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentPerformanceMonitor.class);
    
    public void logComponentRender(String componentPath, long duration) {
        if (duration > 100) {  // 超过 100ms
            LOG.warn("Slow component: {} took {}ms", componentPath, duration);
        }
    }
}
```

#### 阶段 19: 常见问题和解决方案

**问题 1: 组件无法找到脚本**

**症状**: 组件返回 404 或使用默认脚本

**排查步骤**:
1. 检查 Resource Type 是否正确
2. 检查脚本文件是否存在
3. 检查脚本文件名是否匹配（hero.html vs Hero.html）
4. 检查脚本权限

**解决方案**:
```bash
# 使用 CRXDE Lite 检查
1. 导航到 /apps/myapp/components/hero
2. 确认 hero.html 文件存在
3. 检查 jcr:primaryType = "nt:file"
4. 检查 jcr:content/jcr:mimeType = "text/html"
```

**问题 2: Sling Model 无法注入**

**症状**: model 对象为 null

**排查步骤**:
1. 检查 Model 类是否正确注册（Bundle 是否激活）
2. 检查 @Model 注解是否正确
3. 检查 adaptables 类型是否匹配
4. 查看错误日志

**解决方案**:
```java
// 确保 Model 类正确注解
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public interface MyModel {
    // ...
}

// 检查 Bundle 状态
// 在 Felix Console: http://localhost:4502/system/console/bundles
// 确认 Bundle 状态为 "Active"
```

**问题 3: ClientLib 无法加载**

**症状**: CSS/JS 文件 404

**排查步骤**:
1. 检查 ClientLib 配置是否存在
2. 检查 categories 属性是否正确
3. 检查 allowProxy 是否为 true
4. 检查代理路径是否正确

**解决方案**:
```bash
# 检查 ClientLib 配置
curl -u admin:admin \
  "http://localhost:4502/apps/myapp/clientlibs/hero/.content.xml"

# 测试代理路径
curl -u admin:admin \
  "http://localhost:4502/etc.clientlibs/myapp/clientlibs/hero.css"
```

**问题 4: 子组件无法渲染**

**症状**: data-sly-resource 不输出内容

**排查步骤**:
1. 检查子资源是否存在
2. 检查 resourceType 是否正确
3. 检查子组件脚本是否存在
4. 检查权限

**解决方案**:
```html
<!-- 添加调试输出 -->
<div data-sly-test="${resource.child}"
     data-sly-attribute.data-child-path="${resource.child.path}"
     data-sly-attribute.data-child-type="${resource.child.resourceType}">
    <div data-sly-resource="${'child' @ resourceType='myapp/components/child'}"></div>
</div>
```

#### 阶段 20: Resource Provider 机制

**Resource Provider 的作用**:

Resource Provider 允许从 JCR 之外的数据源提供资源，如数据库、外部 API、文件系统等。

**Resource Provider 接口**:

```java
public interface ResourceProvider {
    /**
     * 获取资源
     */
    Resource getResource(ResourceResolver resourceResolver, String path);
    
    /**
     * 获取子资源
     */
    Resource getResource(ResourceResolver resourceResolver, HttpServletRequest request, String path);
    
    /**
     * 枚举子资源
     */
    Iterator<Resource> listChildren(Resource parent);
}
```

**自定义 Resource Provider 示例**:

```java
@Component(service = ResourceProviderFactory.class,
           property = {
               ResourceProvider.PROPERTY_ROOT + "=/external-data"
           })
public class DatabaseResourceProviderFactory implements ResourceProviderFactory {
    
    @Reference
    private DataSource dataSource;
    
    @Override
    public ResourceProvider getResourceProvider(Map<String, Object> authenticationInfo) {
        return new DatabaseResourceProvider(dataSource);
    }
    
    private static class DatabaseResourceProvider implements ResourceProvider {
        private final DataSource dataSource;
        private static final String ROOT = "/external-data";
        
        public DatabaseResourceProvider(DataSource dataSource) {
            this.dataSource = dataSource;
        }
        
        @Override
        public Resource getResource(ResourceResolver resourceResolver, String path) {
            if (!path.startsWith(ROOT)) {
                return null;  // 此 Provider 不处理此路径
            }
            
            // 从数据库加载数据
            String id = path.substring(ROOT.length() + 1);
            Entity entity = loadFromDatabase(id);
            
            if (entity == null) {
                return null;
            }
            
            // 创建虚拟 Resource
            return new SyntheticResource(resourceResolver, path, "external/entity") {
                @Override
                public <T> T adaptTo(Class<T> type) {
                    if (type == ValueMap.class) {
                        return (T) new EntityValueMap(entity);
                    }
                    return super.adaptTo(type);
                }
            };
        }
        
        @Override
        public Iterator<Resource> listChildren(Resource parent) {
            // 从数据库枚举子资源
            List<Entity> entities = listEntitiesFromDatabase();
            return entities.stream()
                .map(entity -> getResource(parent.getResourceResolver(), 
                                          parent.getPath() + "/" + entity.getId()))
                .filter(Objects::nonNull)
                .iterator();
        }
    }
}
```

**Resource Provider 的查找顺序**:

```
1. 检查 ResourceResolver 的 ResourceProvider 列表
   ├── 按 root 路径匹配
   └── 按优先级排序
   
2. 如果找到匹配的 Provider
   └── 使用 Provider 获取资源
   
3. 如果找不到
   └── 从 JCR 查找
```

#### 阶段 21: Sling Model Exporter (JSON API)

**Sling Model Exporter 机制**:

Sling Model Exporter 允许将 Sling Model 导出为 JSON、XML 等格式，用于 Headless 场景和 API 开发。

**Exporter 注解使用**:

```java
@Model(adaptables = SlingHttpServletRequest.class,
       resourceType = "myapp/components/hero",
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, 
          extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public interface HeroModel {
    
    @Exported
    String getTitle();
    
    @Exported
    String getDescription();
    
    @Exported
    String getImageUrl();
    
    // 不导出的字段（内部使用）
    String getInternalData();
    
    // 自定义导出的字段
    @Exported(inline = true)
    Link getLink();
    
    // 嵌套对象导出
    @Exported
    List<Badge> getBadges();
    
    @Model(adaptables = Resource.class)
    interface Badge {
        @Exported
        String getLabel();
        
        @Exported
        String getType();
    }
}
```

**访问导出的 JSON**:

```
GET /content/my-site/en/page/jcr:content/hero.model.json
```

**响应示例**:

```json
{
  "title": "Hero Title",
  "description": "Hero Description",
  "imageUrl": "/content/dam/hero-image.jpg",
  "link": {
    "url": "/content/my-site/en/page",
    "text": "Learn More"
  },
  "badges": [
    {
      "label": "New",
      "type": "primary"
    }
  ]
}
```

**自定义 Exporter**:

```java
@Exporter(name = "custom-json",
          extensions = "json",
          selector = "custom")
public interface CustomExporter {
    // 自定义导出逻辑
}

// 使用
// GET /content/my-site/en/page/jcr:content/hero.custom.json
```

#### 阶段 22: Editable Templates 和 Policy 系统

**Editable Templates 的组件解析**:

在 Editable Templates 中，组件的渲染受到 Policy 配置的影响。

**Policy 配置的位置**:

```
/conf/my-site/settings/wcm/policies/myapp/components/hero
  └── policy.json
```

**Policy 配置示例**:

```json
{
  "jcr:primaryType": "cq:Policy",
  "title": "Hero Component Policy",
  "properties": {
    "allowedComponents": ["myapp/components/text", "myapp/components/image"],
    "styleSystem": {
      "enabled": true,
      "styles": [
        {
          "name": "light",
          "title": "Light Theme"
        },
        {
          "name": "dark",
          "title": "Dark Theme"
        }
      ]
    }
  }
}
```

**Policy 在组件解析中的作用**:

```java
// 在组件渲染时，Policy 影响组件的行为
@Component(service = {})
public class PolicyResolver {
    
    public Policy getPolicy(Resource resource, String policyPath) {
        // 1. 从模板配置获取 Policy 路径
        String templatePath = getTemplatePath(resource);
        Template template = templateManager.getTemplate(templatePath);
        
        // 2. 从 Policy Mapping 获取 Policy
        String policyKey = template.getPolicyMapping().get(resource.getResourceType());
        
        // 3. 解析 Policy
        return resolvePolicy(policyKey);
    }
}
```

**Style System 的解析**:

```html
<!-- HTL 模板中使用 Style System -->
<div class="hero ${style.class}"
     data-sly-attribute.data-component-id="${component.id}"
     data-sly-attribute.data-style-id="${style.id}">
    <!-- 内容 -->
</div>

<!-- Style System 的 CSS 类会动态添加到组件上 -->
<!-- 例如: hero aem-GridColumn--light -->
```

#### 阶段 23: Sling Context-Aware Configuration

**Context-Aware Configuration 机制**:

Context-Aware Configuration 允许根据内容上下文（页面、组件等）提供不同的配置。

**配置的层次结构**:

```
1. 全局配置
   /conf/global/settings/cloudconfigs/my-config
   
2. 站点配置
   /conf/my-site/settings/cloudconfigs/my-config
   
3. 页面配置
   /content/my-site/en/page/jcr:content/cloudconfigs/my-config
   
4. 组件配置
   /content/my-site/en/page/jcr:content/hero/cloudconfigs/my-config
```

**在组件中使用 Context-Aware Configuration**:

```java
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public interface ComponentWithConfig {
    
    @Inject
    @ContextAware  // 启用上下文感知配置
    @Configuration(name = "my-config")
    MyConfig getConfig();
    
    default String getApiEndpoint() {
        return getConfig().getApiEndpoint();
    }
    
    @Model(adaptables = Configuration.class)
    interface MyConfig {
        @ValueMapValue
        String getApiEndpoint();
        
        @ValueMapValue
        int getTimeout();
    }
}
```

**配置的解析过程**:

```java
// Context-Aware Configuration 的解析逻辑
public <T> T getConfiguration(Resource context, Class<T> configClass) {
    // 1. 从组件资源向上遍历，查找配置
    Resource current = context;
    while (current != null) {
        // 2. 检查当前资源下是否有配置
        Resource configResource = current.getChild("cloudconfigs/my-config");
        if (configResource != null) {
            return configResource.adaptTo(configClass);
        }
        
        // 3. 检查站点配置
        String sitePath = getSitePath(current);
        if (sitePath != null) {
            Resource siteConfig = resourceResolver.resolve(
                "/conf/" + sitePath + "/settings/cloudconfigs/my-config");
            if (siteConfig != null) {
                return siteConfig.adaptTo(configClass);
            }
        }
        
        // 4. 向上遍历
        current = current.getParent();
    }
    
    // 5. 使用全局默认配置
    return getGlobalConfiguration(configClass);
}
```

#### 阶段 24: Headless 和 GraphQL 场景

**Headless 场景下的组件解析**:

在 Headless 架构中，AEM 作为内容源，通过 GraphQL 或 REST API 提供数据。

**GraphQL 查询的组件解析**:

```graphql
# GraphQL 查询示例
query {
  heroByPath(_path: "/content/my-site/en/page/jcr:content/hero") {
    title
    description
    image {
      _path
      _dynamicUrl
    }
    link {
      url
      text
    }
  }
}
```

**GraphQL 数据获取器的实现**:

```java
@Component(service = {})
public class HeroDataFetcher implements DataFetcher<HeroModel> {
    
    @Override
    public HeroModel get(DataFetchingEnvironment environment) {
        // 1. 从 GraphQL 上下文获取 ResourceResolver
        ResourceResolver resourceResolver = environment.getContext();
        
        // 2. 解析路径
        String path = environment.getArgument("_path");
        Resource resource = resourceResolver.resolve(path);
        
        // 3. 适配到 Sling Model
        HeroModel model = resource.adaptTo(HeroModel.class);
        
        // 4. 返回模型（GraphQL 会序列化为 JSON）
        return model;
    }
}
```

**组件数据的序列化**:

```java
// GraphQL 使用 Sling Model Exporter 序列化数据
@Model(adaptables = Resource.class,
       resourceType = "myapp/components/hero")
@Exporter(name = "graphql",
          extensions = "json")
public interface HeroModel {
    
    @Exported
    String getTitle();
    
    @Exported
    ImageModel getImage();
    
    @Exported
    LinkModel getLink();
}
```

**Headless 渲染流程**:

```
GraphQL 查询请求
  ↓
GraphQL Engine
  ├── 解析查询
  ├── 确定需要的数据字段
  └── 调用 DataFetcher
  ↓
Resource Resolution (与服务器端渲染相同)
  ├── 解析路径
  ├── 获取 Resource
  └── 适配到 Sling Model
  ↓
数据序列化
  ├── 使用 Exporter 序列化
  └── 返回 JSON
  ↓
GraphQL 响应
```

#### 阶段 25: 错误处理和异常传播机制

**组件渲染过程中的错误处理**:

在组件渲染的各个阶段，都可能发生错误。Sling 提供了多层错误处理机制：

```java
// 错误处理的层次结构
public class ComponentRenderingErrorHandler {
    
    public void renderComponent(Resource resource, Request request, Response response) {
        try {
            // 1. 资源解析错误
            Resource resolvedResource = resourceResolver.resolve(resource.getPath());
            if (resolvedResource == null) {
                throw new ResourceNotFoundException("Resource not found: " + resource.getPath());
            }
            
            // 2. 脚本解析错误
            Script script = scriptResolver.resolveScript(resolvedResource, "GET");
            if (script == null) {
                throw new ScriptNotFoundException("Script not found for: " + resolvedResource.getResourceType());
            }
            
            // 3. 脚本执行错误
            try {
                script.eval(createBindings(request, response, resolvedResource));
            } catch (ScriptException e) {
                handleScriptError(e, resolvedResource, response);
            }
            
        } catch (ResourceNotFoundException e) {
            // 返回 404
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            renderErrorPage("404", e.getMessage(), response);
            
        } catch (ScriptException e) {
            // 脚本执行错误
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logError(e, resource);
            renderErrorPage("500", "Internal Server Error", response);
            
        } catch (Exception e) {
            // 其他错误
            logError(e, resource);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
```

**HTL 模板中的错误处理**:

```html
<!-- HTL 提供了一些错误处理机制 -->
<div data-sly-test="${resource}">
    <!-- 只有当 resource 存在时才渲染 -->
    <h1>${properties.title}</h1>
</div>

<!-- 使用默认值处理空值 -->
<p>${properties.description || 'No description available'}</p>

<!-- 条件渲染避免错误 -->
<div data-sly-test="${model && model.title}">
    <h2>${model.title}</h2>
</div>
```

**Sling Model 的错误处理**:

```java
@Model(adaptables = Resource.class)
public interface ErrorHandlingModel {
    
    // 使用 @Optional 处理可选字段
    @Inject
    @Optional
    String getTitle();
    
    // 提供默认值
    default String getTitle() {
        return getTitle() != null ? getTitle() : "Default Title";
    }
    
    // 使用 @PostConstruct 进行验证
    @PostConstruct
    protected void init() {
        // 验证数据
        if (getTitle() == null || getTitle().isEmpty()) {
            logger.warn("Title is empty for resource: {}", resource.getPath());
        }
    }
}
```

**错误日志记录**:

```java
// 在组件渲染过程中记录错误
@Component(service = {})
public class ComponentErrorLogger {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentErrorLogger.class);
    
    public void logComponentError(String componentPath, Exception error) {
        LOG.error("Error rendering component: {}", componentPath, error);
        
        // 记录错误上下文
        LOG.debug("Error context: {}", getErrorContext(error));
    }
    
    private Map<String, Object> getErrorContext(Exception error) {
        Map<String, Object> context = new HashMap<>();
        context.put("errorType", error.getClass().getName());
        context.put("errorMessage", error.getMessage());
        context.put("stackTrace", getStackTrace(error));
        return context;
    }
}
```

#### 阶段 26: ResourceResolver 的生命周期和最佳实践

**ResourceResolver 的创建时机**:

```java
// ✅ 在请求处理开始时创建
public class MyServlet extends SlingAllMethodsServlet {
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        // ResourceResolver 已经在 Request 中创建好了
        ResourceResolver resolver = request.getResourceResolver();
        
        // 直接使用，不需要手动关闭（由 Sling 管理）
        Resource resource = resolver.resolve(request.getResource().getPath());
    }
}

// ✅ 在后台任务中创建
@Component(service = {})
public class BackgroundTask implements Runnable {
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    @Override
    public void run() {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "background-service");
        
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(authInfo)) {
            // 执行任务
            processResources(resolver);
        } catch (LoginException e) {
            logger.error("Failed to get ResourceResolver", e);
        }
    }
}
```

**Service User 的使用**:

```java
// 使用 Service User（推荐用于后台任务）
@Component(service = {})
public class ServiceUserTask {
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    public void executeTask() {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "my-service-user");
        
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(authInfo)) {
            // Service User 不需要密码，由 AEM 配置管理
            // 在 /apps/system/config/ 中配置 Service User Mappings
            processResources(resolver);
        }
    }
}
```

**Service User Mappings 配置**:

```xml
<!-- /apps/system/config/org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="sling:OsgiConfig">
    <user.mapping>
        <my-service-user>
            <service.name>my-service-user</service.name>
            <user.name>system-user</user.name>
        </my-service-user>
    </user.mapping>
</jcr:root>
```

#### 阶段 27: 生产环境的最佳实践和经验总结

**性能优化实践**:

1. **ResourceResolver 重用**:
```java
// ❌ 不好：每次调用都创建新的 ResourceResolver
public Resource getResource(String path) {
    ResourceResolver resolver = resourceResolverFactory.getResourceResolver(authInfo);
    try {
        return resolver.resolve(path);
    } finally {
        resolver.close();
    }
}

// ✅ 好：在请求级别重用 ResourceResolver
// 在 SlingHttpServletRequest 中，ResourceResolver 已经创建好了
public Resource getResource(SlingHttpServletRequest request, String path) {
    return request.getResourceResolver().resolve(path);
}
```

2. **批量操作**:
```java
// ✅ 批量获取资源属性
public Map<String, ValueMap> batchGetProperties(ResourceResolver resolver, List<String> paths) {
    Map<String, ValueMap> results = new HashMap<>();
    
    // 使用单个查询获取所有资源
    String query = buildBatchQuery(paths);
    QueryResult result = executeQuery(resolver, query);
    
    NodeIterator nodes = result.getNodes();
    while (nodes.hasNext()) {
        Node node = nodes.nextNode();
        results.put(node.getPath(), new JcrValueMap(node));
    }
    
    return results;
}
```

3. **缓存策略**:
```java
// ✅ 使用 Sling Model 缓存
@Model(adaptables = Resource.class)
@Cache
public interface CachedModel {
    // Model 实例会被缓存
}

// ✅ 在 Sling Model 中预处理数据
@PostConstruct
protected void init() {
    // 一次性计算所有数据
    this.processedData = expensiveCalculation();
}
```

**常见生产环境问题**:

1. **ResourceResolver 泄漏**:
   - **症状**: 内存持续增长，JCR Session 耗尽
   - **原因**: 忘记调用 `close()`
   - **解决**: 使用 try-with-resources 或确保在 finally 中关闭

2. **脚本缓存问题**:
   - **症状**: 修改脚本后看不到变化
   - **原因**: 脚本缓存未失效
   - **解决**: 重启 AEM 或清除缓存

3. **ClientLib 加载顺序问题**:
   - **症状**: CSS/JS 样式或功能不正常
   - **原因**: ClientLib 依赖顺序错误
   - **解决**: 在 `.content.xml` 中正确声明 `dependencies`

4. **Sling Model 注入失败**:
   - **症状**: Model 对象为 null
   - **原因**: Bundle 未激活、注解错误、类型不匹配
   - **解决**: 检查 Bundle 状态、验证注解、检查日志

**监控和调试**:

```java
// 添加性能监控
@Component(service = {})
public class ComponentPerformanceMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentPerformanceMonitor.class);
    
    public void monitorRendering(String componentPath, Runnable renderingTask) {
        long startTime = System.currentTimeMillis();
        try {
            renderingTask.run();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            if (duration > 100) {  // 超过 100ms
                LOG.warn("Slow component rendering: {} took {}ms", componentPath, duration);
            }
            
            // 记录到监控系统
            metricsService.recordMetric("component.render.time", duration, "ms");
        }
    }
}
```

### 1.2 组件脚本查找顺序

Sling 按以下优先级查找组件的脚本：

```
1. /apps/{resourceType}/  (应用程序层，可覆盖)
2. /libs/{resourceType}/  (核心库层)
```

在每个层级中，按以下顺序查找：
- `component.html` (HTL 脚本)
- `component.jsp` (JSP 脚本)
- `component.esp` (ECMAScript)

---

Sling 按以下优先级查找组件的脚本：

```
1. /apps/{resourceType}/  (应用程序层，可覆盖)
2. /libs/{resourceType}/  (核心库层)
```

在每个层级中，按以下顺序查找：
- `component.html` (HTL 脚本)
- `component.jsp` (JSP 脚本)
- `component.esp` (ECMAScript)

---

## 2. CSS/JavaScript (ClientLibs) 依赖解析

### 2.1 ClientLibs 的存储位置

按照 AEM 规范，ClientLibs 通常存储在以下位置：

```
/apps/{project}/clientlibs/{category}/
/libs/{project}/clientlibs/{category}/
```

目录结构示例：
```
/apps/myapp/clientlibs/components/card/
  ├── .content.xml          # ClientLib 配置
  ├── css/
  │   └── card.css          # CSS 文件
  └── js/
      └── card.js           # JavaScript 文件
```

### 2.2 ClientLib 配置文件

`.content.xml` 文件定义了 ClientLib 的类别和依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.card]"
          dependencies="[myapp.base,myapp.theme]"
          allowProxy="{Boolean}true"/>
```

关键属性：
- `categories`: 定义此 ClientLib 的类别名称
- `dependencies`: 声明依赖的其他 ClientLib 类别
- `allowProxy`: 允许通过代理路径访问

### 2.3 Sling 如何找到 ClientLibs

当 HTL 模板中使用 `data-sly-call="${clientlib.css @ categories='myapp.components.card'}"` 时：

1. **类别查找**: Sling 在以下位置查找匹配的 ClientLib：
   ```
   /apps/myapp/clientlibs/**/.content.xml
   /libs/myapp/clientlibs/**/.content.xml
   ```
   查找 `categories` 属性包含 `myapp.components.card` 的节点

2. **依赖解析**: 如果找到的 ClientLib 声明了 `dependencies`，Sling 会递归查找所有依赖

3. **资源合并**: 收集所有匹配的 CSS/JS 文件并合并

4. **代理路径生成**: 通过 `/etc.clientlibs/` 代理路径提供最终文件

### 2.4 ClientLib 代理机制

AEM 使用代理路径访问 ClientLibs：

```
实际路径: /apps/myapp/clientlibs/components/card/css/card.css
代理路径: /etc.clientlibs/myapp/clientlibs/components/card.css
```

优势：
- 避免暴露内部路径结构
- 支持版本控制和缓存
- 允许跨域访问

---

## 3. 子组件依赖解析（data-sly-resource）

### 3.1 Resource Include 机制

当 HTL 模板使用 `data-sly-resource` 时：

```html
<div data-sly-resource="${'child' @ resourceType='myapp/components/child'}"></div>
```

Sling 的解析过程：

1. **资源路径解析**: 
   - 如果路径是相对路径（如 `'child'`），相对于当前 Resource
   - 如果是绝对路径，直接解析

2. **ResourceType 应用**:
   - 如果指定了 `resourceType`，使用指定的类型
   - 否则使用子资源的 `sling:resourceType` 属性

3. **脚本查找**: 使用与主组件相同的脚本查找机制

4. **递归渲染**: 对子组件执行完整的渲染流程

### 3.2 组件继承机制（sling:resourceSuperType）详解

**组件继承的核心概念**:

`sling:resourceSuperType` 是 AEM 组件系统中最强大的机制之一，它允许组件继承父组件的所有功能，包括脚本、对话框、编辑配置等，同时可以覆盖和扩展这些功能。这种机制类似于面向对象编程中的类继承。

**快速理解**:
- **作用**: 让当前组件继承另一个组件的功能（代码复用）
- **方向**: 向上继承（从父组件继承）
- **时机**: 组件渲染时查找脚本、对话框等资源
- **结果**: 减少重复代码，统一管理基础功能

**与相关属性的区别**:

| 属性 | 作用 | 方向 | 目的 |
|------|------|------|------|
| **sling:resourceSuperType** | 继承功能 | 向上 | 代码复用 |
| **allowParents** | 使用限制 | 向下 | 限制组件只能被哪些父组件包含 |
| **allowedChildren** | 包含限制 | 向上 | 限制容器只能包含哪些子组件 |

#### 3.2.1 什么是组件继承

组件继承通过 `sling:resourceSuperType` 属性实现，该属性指向父组件的路径。子组件可以继承父组件的以下内容：

1. **渲染脚本**（HTL/JSP）
   - 如果子组件没有自己的脚本，使用父组件的脚本
   - 子组件可以覆盖父组件的脚本

2. **对话框配置**（`_cq_dialog`）
   - 如果子组件没有对话框，使用父组件的对话框
   - 可以部分继承和扩展父组件的对话框

3. **设计对话框**（`_cq_design_dialog`）
   - 类似对话框的继承机制

4. **编辑配置**（`_cq_editConfig`）
   - 拖放目标、就地编辑等配置可以继承

5. **客户端库**（ClientLibs）
   - 通过 `extraClientlibs` 属性继承

6. **Sling Model 定义**
   - 子组件可以使用父组件的 Model（如果 resourceType 匹配）

7. **组件属性**（如 `componentGroup`）
   - 组件的基本属性可以继承

#### 3.2.2 组件继承的配置

**基础配置示例**:

```xml
<!-- 子组件: /apps/myapp/components/hero/.content.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="cq:Component"
          jcr:title="Hero Component"
          sling:resourceSuperType="myapp/components/base"
          componentGroup="MyApp - Content"/>

<!-- 父组件: /apps/myapp/components/base/.content.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="cq:Component"
          jcr:title="Base Component"
          componentGroup="MyApp - Base"/>
```

#### 3.2.3 继承的查找机制

**完整的继承查找链**:

```
请求组件: hero
Resource Type: "myapp/components/hero"

查找脚本时（hero.html）:
1. /apps/myapp/components/hero/hero.html  ← 优先使用子组件的脚本
   ↓ (如果不存在)
2. /apps/myapp/components/base/hero.html  ← 查找父组件
   ↓ (如果不存在)
3. /apps/myapp/components/base/base.html  ← 父组件的默认脚本
   ↓ (如果不存在，继续向上查找 resourceSuperType)
4. /libs/myapp/components/base/base.html  ← 查找 /libs
   ↓
5. 继续递归查找父组件的 resourceSuperType
```

**继承查找的递归算法**:

```java
// 组件继承查找的完整实现
public class ComponentInheritanceResolver {
    
    private ResourceResolver resourceResolver;
    private List<String> searchPaths = Arrays.asList("/apps", "/libs");
    
    /**
     * 查找组件的脚本，支持继承
     */
    public Resource findScript(String resourceType, String scriptName) {
        Set<String> visited = new HashSet<>();
        return findScriptRecursive(resourceType, scriptName, visited);
    }
    
    private Resource findScriptRecursive(String resourceType, String scriptName, Set<String> visited) {
        // 防止循环引用
        if (visited.contains(resourceType)) {
            return null;
        }
        visited.add(resourceType);
        
        // 1. 在当前组件中查找脚本
        for (String searchPath : searchPaths) {
            String scriptPath = searchPath + "/" + resourceType + "/" + scriptName;
            Resource script = resourceResolver.resolve(scriptPath);
            if (script != null) {
                return script;  // 找到脚本，返回
            }
        }
        
        // 2. 查找父组件（resourceSuperType）
        Resource componentResource = findComponentResource(resourceType);
        if (componentResource != null) {
            String resourceSuperType = componentResource.getValueMap()
                .get("sling:resourceSuperType", String.class);
            
            if (resourceSuperType != null && !resourceSuperType.isEmpty()) {
                // 递归查找父组件
                return findScriptRecursive(resourceSuperType, scriptName, visited);
            }
        }
        
        return null;  // 未找到
    }
    
    private Resource findComponentResource(String resourceType) {
        for (String searchPath : searchPaths) {
            Resource resource = resourceResolver.resolve(searchPath + "/" + resourceType);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }
}
```

#### 3.2.4 继承的具体内容

**1. 脚本继承（HTL/JSP）**

子组件优先使用自己的脚本，如果不存在，则使用父组件的脚本：

```
/apps/myapp/components/hero/
  ├── hero.html          ← 如果存在，使用此脚本
  └── (如果不存在，查找父组件)
      /apps/myapp/components/base/
          └── base.html  ← 使用父组件的脚本
```

**示例：脚本覆盖**:

```html
<!-- 父组件: /apps/myapp/components/base/base.html -->
<div class="base-component">
    <h1>${properties.title}</h1>
    <div>${properties.content}</div>
</div>

<!-- 子组件: /apps/myapp/components/hero/hero.html (覆盖父组件脚本) -->
<div class="hero-component">
    <div class="hero-image">
        <img src="${properties.imagePath}" alt="${properties.title}">
    </div>
    <div class="hero-content">
        <h1>${properties.title}</h1>
        <p>${properties.description}</p>
        <a href="${properties.linkUrl}">${properties.linkText}</a>
    </div>
</div>
```

**2. 对话框继承（_cq_dialog）**

对话框的继承机制：如果子组件没有定义对话框，则使用父组件的对话框。

```
/apps/myapp/components/hero/
  └── _cq_dialog/        ← 如果存在，使用此对话框
      └── .content.xml
  ↓ (如果不存在)
/apps/myapp/components/base/
  └── _cq_dialog/        ← 使用父组件的对话框
      └── .content.xml
```

**对话框合并机制**:

AEM 还支持对话框的部分继承和合并（通过 Granite UI 的 `sling:resourceSuperType` 在对话框节点级别）：

```xml
<!-- 子组件可以扩展父组件的对话框 -->
<!-- /apps/myapp/components/hero/_cq_dialog/.content.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          jcr:primaryType="nt:unstructured"
          jcr:title="Hero"
          sling:resourceType="cq/gui/components/authoring/dialog"
          sling:resourceSuperType="myapp/components/base/_cq_dialog">
    <!-- 添加额外的字段 -->
    <content>
        <items>
            <!-- 父组件的字段会自动包含 -->
            <!-- 这里只添加子组件特有的字段 -->
            <image jcr:primaryType="nt:unstructured"
                   sling:resourceType="granite/ui/components/coral/foundation/form/pathfield"
                   fieldLabel="Hero Image"
                   name="./imagePath"/>
        </items>
    </content>
</jcr:root>
```

**3. 编辑配置继承（_cq_editConfig）**

编辑配置的继承：

```
/apps/myapp/components/hero/
  └── _cq_editConfig/    ← 如果存在，合并或覆盖父组件的配置
      └── .content.xml
  ↓ (如果不存在)
/apps/myapp/components/base/
  └── _cq_editConfig/    ← 使用父组件的配置
      └── .content.xml
```

**4. 客户端库继承**

客户端库通过 `extraClientlibs` 属性继承：

```xml
<!-- 子组件可以继承父组件的 ClientLibs -->
<!-- /apps/myapp/components/hero/.content.xml -->
<jcr:root jcr:primaryType="cq:Component"
          sling:resourceSuperType="myapp/components/base">
    <!-- 继承父组件的 ClientLibs，并添加自己的 -->
    <!-- 在 HTL 中使用: data-sly-call="${clientlib.all @ categories='myapp.components.base,myapp.components.hero'}" -->
</jcr:root>
```

**5. Sling Model 继承**

Sling Model 通过 `resourceType` 匹配，子组件可以使用父组件的 Model：

```java
// 父组件的 Model
@Model(adaptables = Resource.class,
       resourceType = "myapp/components/base")
public interface BaseModel {
    String getTitle();
}

// 子组件可以重用父组件的 Model（如果 resourceType 匹配）
// 或者定义自己的 Model
@Model(adaptables = Resource.class,
       resourceType = "myapp/components/hero")
public interface HeroModel extends BaseModel {  // 继承父 Model
    String getImagePath();
}
```

#### 3.2.5 多级继承

AEM 支持多级继承（继承链）：

```
/myapp/components/card/
  └── sling:resourceSuperType = "myapp/components/base"
      └── sling:resourceSuperType = "foundation/components/parbase"
          └── (没有 resourceSuperType，继承链结束)
```

**多级继承的查找顺序**:

```
请求: card.html

查找顺序:
1. /apps/myapp/components/card/card.html
   ↓
2. /apps/myapp/components/base/card.html
   ↓
3. /apps/myapp/components/base/base.html
   ↓
4. /libs/foundation/components/parbase/card.html
   ↓
5. /libs/foundation/components/parbase/parbase.html
```

**多级继承的递归实现**:

```java
public List<String> getInheritanceChain(String resourceType) {
    List<String> chain = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    
    String currentType = resourceType;
    while (currentType != null && !visited.contains(currentType)) {
        visited.add(currentType);
        chain.add(currentType);
        
        Resource componentResource = findComponentResource(currentType);
        if (componentResource != null) {
            String superType = componentResource.getValueMap()
                .get("sling:resourceSuperType", String.class);
            currentType = superType;
        } else {
            break;
        }
    }
    
    return chain;
}
```

#### 3.2.6 继承 vs 覆盖

**覆盖规则**:

1. **脚本覆盖**: 子组件的脚本完全覆盖父组件的脚本（不存在合并）
2. **对话框**: 如果子组件定义了对话框，通常不会自动继承父组件的对话框（除非使用 `sling:resourceSuperType`）
3. **属性覆盖**: 子组件的属性会覆盖父组件的同名属性

**实际应用示例**:

```
场景: 创建一个卡片组件，继承基础组件

父组件: base
  ├── base.html (基础 HTML 结构)
  ├── _cq_dialog (基础字段: title, content)
  └── ClientLib: myapp.components.base

子组件: card (继承 base)
  ├── card.html (覆盖，使用卡片布局)
  ├── _cq_dialog (覆盖，添加 imagePath 字段)
  └── ClientLib: myapp.components.card (继承 myapp.components.base)
```

#### 3.2.7 在 HTL 中使用继承

**在 HTL 模板中引用父组件**:

```html
<!-- 显式使用 resourceSuperType -->
<div data-sly-resource="${resource @ resourceSuperType='myapp/components/base'}"></div>

<!-- 隐式继承（通过组件的 resourceSuperType 属性） -->
<!-- 如果当前组件设置了 sling:resourceSuperType，脚本查找会自动使用继承 -->
```

#### 3.2.8 继承的最佳实践

1. **基础组件设计**:
   - 创建通用的基础组件（如 `base`, `container`）
   - 在基础组件中定义通用的对话框字段
   - 基础组件的脚本应该足够通用

2. **继承层次**:
   - 避免过深的继承链（建议不超过 3-4 层）
   - 每层继承应该有明确的职责

3. **脚本覆盖**:
   - 子组件应该覆盖父组件的脚本，而不是完全重写
   - 考虑使用模板和片段（`data-sly-call`）来实现代码复用

4. **对话框设计**:
   - 在父组件中定义通用字段
   - 在子组件中只添加特定字段

#### 3.2.9 React 迁移中的继承映射

**AEM 继承 → React 继承/组合**:

```typescript
// AEM 继承结构
// base/.content.xml: sling:resourceSuperType = null
// card/.content.xml: sling:resourceSuperType = "myapp/components/base"

// React 映射方案 1: 类继承
class BaseComponent extends React.Component<BaseProps> {
    render() {
        return <div className="base">{this.props.children}</div>;
    }
}

class CardComponent extends BaseComponent {
    render() {
        return (
            <div className="card">
                {super.render()}
            </div>
        );
    }
}

// React 映射方案 2: 组合模式（推荐）
interface BaseProps {
    title?: string;
    content?: string;
}

const BaseComponent: React.FC<BaseProps> = ({ title, content, children }) => {
    return (
        <div className="base">
            {title && <h1>{title}</h1>}
            {content && <p>{content}</p>}
            {children}
        </div>
    );
};

interface CardProps extends BaseProps {
    imagePath?: string;
}

const CardComponent: React.FC<CardProps> = ({ imagePath, ...baseProps }) => {
    return (
        <div className="card">
            {imagePath && <img src={imagePath} alt={baseProps.title} />}
            <BaseComponent {...baseProps} />
        </div>
    );
};

// React 映射方案 3: HOC（高阶组件）
function withBaseComponent<T extends BaseProps>(Component: React.ComponentType<T>) {
    return (props: T) => {
        return (
            <BaseComponent {...props}>
                <Component {...props} />
            </BaseComponent>
        );
    };
}

const CardWithBase = withBaseComponent(CardComponent);
```

**继承链的可视化**:

```javascript
// 分析组件继承链的工具函数
function getInheritanceChain(componentPath) {
    const chain = [componentPath];
    let current = componentPath;
    
    while (true) {
        const config = getComponentConfig(current);
        const superType = config['sling:resourceSuperType'];
        
        if (!superType) break;
        chain.push(superType);
        current = superType;
    }
    
    return chain;
}

// 使用示例
const chain = getInheritanceChain('myapp/components/hero');
// 返回: ['myapp/components/hero', 'myapp/components/base', 'foundation/components/parbase']
```

#### 3.2.10 继承机制的调试

**检查继承链**:

```groovy
// Groovy 脚本：打印组件的完整继承链
def componentPath = "/apps/myapp/components/hero"
def resource = resourceResolver.getResource(componentPath)
def visited = []

while (resource != null) {
    def path = resource.path
    if (visited.contains(path)) {
        println "循环引用检测到: $path"
        break
    }
    visited.add(path)
    
    println "组件: $path"
    def properties = resource.getValueMap()
    println "  - Resource Type: ${properties.get('sling:resourceType', 'N/A')}"
    println "  - Super Type: ${properties.get('sling:resourceSuperType', 'N/A')}"
    
    def superType = properties.get('sling:resourceSuperType', String.class)
    if (superType) {
        resource = resourceResolver.getResource("/apps/$superType")
    } else {
        break
    }
}
```

**检查脚本查找路径**:

```bash
# 使用 AEM Developer Tools 检查脚本解析
# 访问: http://localhost:4502/system/console/slingresolver

# 输入 Resource Type: myapp/components/hero
# 输入脚本名: hero.html
# 查看解析结果和继承链
```

---

**总结**:

`sling:resourceSuperType` 是 AEM 组件系统的核心机制，它提供了强大的代码复用能力。理解继承机制对于：
- 设计和组织组件架构
- 调试组件渲染问题
- 将 AEM 组件迁移到 React 等现代框架

都至关重要。继承机制允许开发者创建可维护、可扩展的组件系统，同时保持代码的 DRY（Don't Repeat Yourself）原则。

---

## 4. Java 类依赖解析（Sling Models / Use API）

### 4.1 Use API 类查找

当 HTL 使用 `data-sly-use` 时：

```html
<sly data-sly-use.model="${'com.example.MyModel'}"></sly>
```

Sling 的类加载机制：

1. **OSGi 服务查找**:
   - 首先检查是否为 OSGi 服务（@ProviderType）
   - 如果是，从 OSGi Service Registry 获取实例

2. **类路径查找**:
   - 在 Bundle 的类路径中查找类
   - 支持 Java 包名和类名

3. **适配器机制**:
   - 如果类实现了 `AdapterFactory`，使用适配器模式
   - 从 Resource 或 Request 适配到目标类型

### 4.2 Sling Model 解析

Sling Models 使用注解驱动：

```java
@Model(adaptables = Resource.class)
public interface MyModel {
    @Inject
    String getTitle();
}
```

解析过程：

1. **Model 注册**: 在 Bundle 激活时，扫描 `@Model` 注解的类
2. **适配器匹配**: 根据 `adaptables` 类型匹配
3. **依赖注入**: 使用 `@Inject` 注解注入依赖
4. **实例创建**: 创建 Model 实例并注入依赖

### 4.3 OSGi 服务注入

Sling Models 可以注入 OSGi 服务：

```java
@Model(adaptables = Resource.class)
public interface MyModel {
    @OSGiService
    MyService myService;
}
```

Sling 从 OSGi Service Registry 查找匹配的服务接口。

---

## 5. 完整依赖解析流程示例

假设有以下组件结构：

```
/apps/myapp/components/card/
  ├── card.html
  ├── .content.xml
  └── _cq_dialog/
      └── .content.xml
```

HTL 模板内容：

```html
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-use.model="${'com.myapp.CardModel'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.card'}"></sly>

<div class="card">
    <h2>${model.title}</h2>
    <div data-sly-resource="${'content' @ resourceType='foundation/components/parsys'}"></div>
</div>
```

Sling 的解析步骤：

### 步骤 1: 资源解析
```
请求: /content/my-site/en/page/jcr:content/card
→ ResourceResolver 找到 JCR 节点
→ 获取 sling:resourceType = 'myapp/components/card'
```

### 步骤 2: 脚本查找
```
Resource Type: myapp/components/card
→ 查找 /apps/myapp/components/card/card.html
→ 找到脚本，准备渲染
```

### 步骤 3: ClientLib 解析
```
categories='myapp.components.card'
→ 查找 /apps/myapp/clientlibs/**/.content.xml
→ 找到 /apps/myapp/clientlibs/components/card/.content.xml
→ 读取 dependencies 属性
→ 递归查找依赖的 ClientLibs
→ 收集 CSS 文件并生成代理路径
```

### 步骤 4: Java 类解析
```
'com.myapp.CardModel'
→ 在 Bundle 类路径中查找
→ 检查是否为 Sling Model
→ 从 Resource 适配到 CardModel
→ 执行依赖注入（@Inject）
→ 创建实例
```

### 步骤 5: 子组件解析
```
resourceType='foundation/components/parsys'
→ 查找 /libs/foundation/components/parsys/parsys.html
→ 渲染子组件
→ 递归处理子组件中的组件
```

### 步骤 6: 合并输出
```
合并所有 HTML 输出
→ 添加 ClientLib 引用（<link>, <script>）
→ 返回最终 HTML
```

---

## 6. 依赖解析的优化机制

### 6.1 缓存机制

Sling 使用多层缓存：

1. **Resource 缓存**: 缓存 Resource 对象
2. **Script 缓存**: 缓存编译后的脚本
3. **ClientLib 缓存**: 缓存合并后的 CSS/JS
4. **Model 缓存**: 缓存 Sling Model 实例（如果标记为可缓存）

### 6.2 延迟加载

- **ClientLibs**: 只在需要时加载（通过 categories）
- **子组件**: 只在渲染时解析
- **Java 类**: 懒加载模式

### 6.3 依赖图构建

AEM 在构建时分析依赖关系：

```
ClientLib Dependencies:
  myapp.components.card
    → myapp.base
    → myapp.theme

Component Hierarchy:
  card
    → parsys
      → text
      → image
```

---

## 7. 调试依赖解析

### 7.1 查看 Resource Type

在 HTL 模板中：

```html
<!-- 当前资源的 Resource Type -->
<p>Resource Type: ${resource.resourceType}</p>

<!-- Resource Super Type -->
<p>Super Type: ${resource.resourceSuperType}</p>
```

### 7.2 查看 ClientLib 路径

访问 ClientLib 代理路径：

```
http://localhost:4502/etc.clientlibs/myapp/clientlibs/components/card.css
```

### 7.3 查看组件脚本路径

使用 AEM Developer Mode 查看实际使用的脚本路径。

### 7.4 日志调试

启用 Sling 日志查看解析过程：

```
org.apache.sling.scripting.sightly DEBUG
org.apache.sling.models DEBUG
```

---

## 8. 最佳实践

### 8.1 组织 ClientLibs

```
/apps/myapp/clientlibs/
  ├── base/           # 基础样式和脚本
  ├── theme/          # 主题样式
  ├── components/     # 组件相关
  │   ├── card/
  │   ├── header/
  │   └── footer/
  └── vendor/         # 第三方库
```

### 8.2 声明依赖关系

在 ClientLib 的 `.content.xml` 中明确声明依赖：

```xml
dependencies="[myapp.base,myapp.theme]"
```

### 8.3 使用 Resource Super Type

避免代码重复，使用组件继承：

```
/apps/myapp/components/
  ├── base/           # 基础组件
  └── card/           # 继承 base
      .content.xml: sling:resourceSuperType = "myapp/components/base"
```

### 8.4 合理的 Resource Type 命名

使用有意义的命名空间：

```
✅ myapp/components/card
✅ myapp/components/product/list
❌ card
❌ component1
```

---

## 9. 常见问题

### Q1: ClientLib 找不到？

**检查项**:
- `.content.xml` 中的 `categories` 是否正确
- ClientLib 路径是否正确
- Bundle 是否已激活
- 是否有权限访问

### Q2: Sling Model 无法注入？

**检查项**:
- Model 是否正确注册（`@Model` 注解）
- `adaptables` 类型是否匹配
- Bundle 是否已激活
- 类路径是否正确

### Q3: 子组件无法渲染？

**检查项**:
- `resourceType` 路径是否正确
- 组件脚本是否存在
- 是否有渲染权限
- Resource 是否存在

### Q4: 依赖顺序问题？

**解决方案**:
- 在 ClientLib 的 `dependencies` 中明确声明
- 使用 Resource Super Type 管理组件继承
- 合理组织组件结构

---

## 10. 总结

Sling 的资源解析机制是一个多阶段的递归过程：

1. **Resource 解析**: 根据路径找到 JCR 节点
2. **Resource Type 解析**: 确定组件类型
3. **脚本查找**: 按优先级查找渲染脚本
4. **依赖收集**: 递归收集所有依赖（ClientLibs、子组件、Java 类）
5. **资源加载**: 从不同来源加载资源
6. **合并渲染**: 合并所有输出

理解这个机制有助于：
- 正确组织组件结构
- 优化性能
- 调试问题
- 遵循 AEM 最佳实践
- **组件迁移和分析**（如迁移到 React）

---

## 11. 组件依赖分析：React 迁移场景

### 11.1 依赖分析的目标

当需要将 AEM 组件迁移到 React 时，需要提取以下完整信息：

1. **组件结构信息**
   - Resource Type 和 Super Type
   - 组件路径和文件结构
   - 对话框配置（`_cq_dialog`）
   - 设计对话框配置（`_cq_design_dialog`）
   - 编辑配置（`_cq_editConfig`）
   - 策略配置（Editable Templates）
   - 组件属性（`jcr:title`, `jcr:description`, `componentGroup` 等）

**使用 CRX/DE Lite 收集组件信息**:

在 AEM 中，最直接的方法是使用 CRX/DE Lite 工具：

```
1. 访问: http://localhost:4502/crx/de/index.jsp

2. 导航到组件路径:
   /apps/myapp/components/hero

3. 查看组件结构:
   ├── .content.xml (组件定义)
   │   ├── jcr:title
   │   ├── jcr:description
   │   ├── sling:resourceType
   │   ├── sling:resourceSuperType
   │   └── componentGroup
   │
   ├── hero.html (HTL 脚本)
   │   └── 查看脚本逻辑
   │
   ├── _cq_dialog/ (编辑对话框)
   │   └── .content.xml
   │       └── 查看所有可编辑字段
   │
   ├── _cq_design_dialog/ (设计对话框)
   │   └── .content.xml
   │
   └── _cq_editConfig/ (编辑配置)
       └── .content.xml
```

**从 JCR 节点提取组件信息**:

```groovy
// Groovy 脚本（在 AEM Groovy Console 中执行）
def componentPath = "/apps/myapp/components/hero"
def componentResource = resourceResolver.getResource(componentPath)
def componentNode = componentResource.adaptTo(javax.jcr.Node.class)

def componentInfo = [:]

// 1. 基本属性
componentInfo.title = componentNode.getProperty("jcr:title")?.string
componentInfo.description = componentNode.getProperty("jcr:description")?.string
componentInfo.resourceType = componentPath
componentInfo.resourceSuperType = componentNode.getProperty("sling:resourceSuperType")?.string
componentInfo.componentGroup = componentNode.getProperty("componentGroup")?.string
componentInfo.isContainer = componentNode.getProperty("cq:isContainer")?.boolean

// 2. 对话框字段
def dialogResource = componentResource.getChild("_cq_dialog")
if (dialogResource) {
    componentInfo.dialogFields = extractDialogFields(dialogResource)
}

// 3. 编辑配置
def editConfigResource = componentResource.getChild("_cq_editConfig")
if (editConfigResource) {
    componentInfo.editConfig = extractEditConfig(editConfigResource)
}

// 4. HTL 脚本
def scriptResource = componentResource.getChild("hero.html")
if (scriptResource) {
    componentInfo.scriptContent = scriptResource.adaptTo(javax.jcr.Node.class)
        .getNode("jcr:content").getProperty("jcr:data").binary.stream.text
}

// 输出 JSON
import groovy.json.JsonBuilder
def json = new JsonBuilder(componentInfo)
println json.toPrettyString()
```

2. **样式依赖（ClientLibs）**
   - 所有使用的 CSS categories
   - CSS 文件的完整路径和内容
   - 依赖的 ClientLib categories（递归）
   - CSS 变量和主题配置

3. **脚本依赖**
   - JavaScript categories
   - JS 文件内容和依赖
   - 第三方库依赖

4. **数据模型依赖**
   - Sling Model 类名和包路径
   - Model 接口定义和方法
   - 数据属性和结构
   - OSGi 服务依赖

5. **子组件依赖**
   - 所有包含的子组件（递归）
   - 子组件的 Resource Type
   - 参数传递关系

6. **资源依赖**
   - 图片、字体等静态资源
   - 模板文件（`data-sly-template`）
   - 国际化资源

### 11.2 依赖分析工具和方法

#### 方法 1: JCR 查询分析

使用 JCR 查询提取组件信息：

**Groovy 脚本示例（AEM Groovy Console）**:

```groovy
import org.apache.sling.api.resource.Resource
import javax.jcr.Node
import javax.jcr.query.Query
import com.day.cq.wcm.api.PageManager
import com.day.cq.commons.jcr.JcrConstants

def componentPath = "/apps/myapp/components/card"
def resourceResolver = resourceResolver
def componentResource = resourceResolver.getResource(componentPath)

def componentInfo = [:]

// 1. 基本信息
componentInfo.resourceType = componentPath
componentInfo.path = componentPath

// 2. 组件配置
def componentNode = componentResource.adaptTo(Node.class)
if (componentNode.hasProperty("sling:resourceSuperType")) {
    componentInfo.resourceSuperType = componentNode.getProperty("sling:resourceSuperType").string
}

// 3. 对话框配置
def dialogResource = componentResource.getChild("_cq_dialog")
if (dialogResource) {
    componentInfo.hasDialog = true
    // 可以进一步解析对话框结构
}

// 4. 查找 HTL 模板
def templateResource = componentResource.getChild("card.html")
if (!templateResource) {
    templateResource = componentResource.getChild("card.jsp")
}
if (templateResource) {
    componentInfo.templatePath = templateResource.path
    componentInfo.templateType = templateResource.path.endsWith(".html") ? "HTL" : "JSP"
}

// 5. ClientLibs 分析
componentInfo.clientlibs = []
// 需要解析 HTL 模板中的 clientlib categories

// 6. Sling Models 分析
componentInfo.slingModels = []
// 需要解析 HTL 模板中的 data-sly-use

// 输出 JSON
import groovy.json.JsonBuilder
def json = new JsonBuilder(componentInfo)
println json.toPrettyString()

return componentInfo
```

#### 方法 2: HTL 模板解析工具

创建一个 Java 工具来解析 HTL 模板：

```java
package com.myapp.analysis;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ComponentDependencyAnalyzer {
    
    public static class ComponentDependencies {
        public String resourceType;
        public String resourceSuperType;
        public List<String> clientLibCategories = new ArrayList<>();
        public List<String> slingModels = new ArrayList<>();
        public List<String> childComponents = new ArrayList<>();
        public Map<String, String> childComponentTypes = new HashMap<>();
        public List<String> templates = new ArrayList<>();
        public Map<String, Object> properties = new HashMap<>();
    }
    
    public ComponentDependencies analyzeComponent(ResourceResolver resolver, String componentPath) {
        ComponentDependencies deps = new ComponentDependencies();
        Resource componentResource = resolver.getResource(componentPath);
        
        if (componentResource == null) {
            return deps;
        }
        
        deps.resourceType = componentPath;
        
        // 查找 HTL 模板
        Resource htmlTemplate = componentResource.getChild("card.html");
        if (htmlTemplate == null) {
            htmlTemplate = componentResource.getChild(componentResource.getName() + ".html");
        }
        
        if (htmlTemplate != null) {
            String templateContent = readResourceContent(htmlTemplate);
            deps = parseHTLTemplate(templateContent, deps);
        }
        
        // 查找 ClientLib 配置文件
        analyzeClientLibs(resolver, componentPath, deps);
        
        return deps;
    }
    
    private ComponentDependencies parseHTLTemplate(String content, ComponentDependencies deps) {
        // 解析 clientlib categories
        Pattern clientLibPattern = Pattern.compile(
            "clientlib\\.(css|js|all)\\s*@\\s*categories=['\"]([^'\"]+)['\"]"
        );
        Matcher clientLibMatcher = clientLibPattern.matcher(content);
        while (clientLibMatcher.find()) {
            String categories = clientLibMatcher.group(2);
            deps.clientLibCategories.addAll(Arrays.asList(categories.split(",")));
        }
        
        // 解析 Sling Models
        Pattern modelPattern = Pattern.compile(
            "data-sly-use\\.\\w+\\s*=\\s*['\"]([^'\"]+)['\"]"
        );
        Matcher modelMatcher = modelPattern.matcher(content);
        while (modelMatcher.find()) {
            deps.slingModels.add(modelMatcher.group(1));
        }
        
        // 解析子组件
        Pattern resourcePattern = Pattern.compile(
            "data-sly-resource\\s*=\\s*['\"]([^'\"]+)['\"][^>]*resourceType=['\"]([^'\"]+)['\"]"
        );
        Matcher resourceMatcher = resourcePattern.matcher(content);
        while (resourceMatcher.find()) {
            String childPath = resourceMatcher.group(1);
            String childType = resourceMatcher.group(2);
            deps.childComponents.add(childPath);
            deps.childComponentTypes.put(childPath, childType);
        }
        
        // 解析模板调用
        Pattern templatePattern = Pattern.compile(
            "data-sly-call\\s*=\\s*\\$\\{([^}]+)\\}",
            Pattern.DOTALL
        );
        Matcher templateMatcher = templatePattern.matcher(content);
        while (templateMatcher.find()) {
            deps.templates.add(templateMatcher.group(1));
        }
        
        return deps;
    }
    
    private void analyzeClientLibs(ResourceResolver resolver, String componentPath, 
                                   ComponentDependencies deps) {
        // 递归查找所有 ClientLib dependencies
        Set<String> allCategories = new HashSet<>(deps.clientLibCategories);
        Set<String> processed = new HashSet<>();
        
        while (!allCategories.isEmpty()) {
            String category = allCategories.iterator().next();
            allCategories.remove(category);
            processed.add(category);
            
            // 查找 ClientLib 配置
            String query = "SELECT * FROM [cq:ClientLibraryFolder] WHERE " +
                          "categories = '" + category + "'";
            // 执行查询并解析 dependencies
            // ...
        }
    }
    
    private String readResourceContent(Resource resource) {
        // 读取资源内容
        // ...
        return "";
    }
}
```

#### 方法 3: AEM HTTP API 分析

使用 AEM 的 JSON API 获取组件信息：

```bash
# 获取组件节点信息（JSON）
curl -u admin:admin \
  "http://localhost:4502/apps/myapp/components/card.json"

# 获取组件对话框配置
curl -u admin:admin \
  "http://localhost:4502/apps/myapp/components/card/_cq_dialog.json"

# 获取 ClientLib 信息
curl -u admin:admin \
  "http://localhost:4502/etc.clientlibs/myapp/clientlibs/components/card.json"
```

#### 方法 4: Node.js 依赖分析脚本

创建 Node.js 脚本来分析组件：

```javascript
// analyze-component-deps.js
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class ComponentAnalyzer {
    constructor(aemBaseUrl, credentials) {
        this.baseUrl = aemBaseUrl;
        this.credentials = credentials;
    }

    async analyzeComponent(componentPath) {
        const deps = {
            resourceType: componentPath,
            clientlibs: [],
            slingModels: [],
            childComponents: [],
            templates: [],
            dialog: null,
            properties: {}
        };

        // 1. 读取组件配置
        const config = await this.fetchJSON(`${componentPath}.json`);
        deps.properties = config;

        // 2. 读取 HTL 模板
        const htmlPath = `${componentPath}/${path.basename(componentPath)}.html`;
        const htmlContent = await this.fetchText(htmlPath);
        this.parseHTL(htmlContent, deps);

        // 3. 读取对话框配置
        const dialogPath = `${componentPath}/_cq_dialog.json`;
        try {
            deps.dialog = await this.fetchJSON(dialogPath);
        } catch (e) {
            // 对话框可能不存在
        }

        // 4. 递归分析 ClientLibs
        await this.analyzeClientLibs(deps.clientlibs, deps);

        return deps;
    }

    parseHTL(content, deps) {
        // 解析 clientlib categories
        const clientLibRegex = /clientlib\.(css|js|all)\s*@\s*categories=['"]([^'"]+)['"]/g;
        let match;
        while ((match = clientLibRegex.exec(content)) !== null) {
            const categories = match[2].split(',').map(c => c.trim());
            deps.clientlibs.push(...categories);
        }

        // 解析 Sling Models
        const modelRegex = /data-sly-use\.\w+\s*=\s*['"]([^'"]+)['"]/g;
        while ((match = modelRegex.exec(content)) !== null) {
            deps.slingModels.push(match[1]);
        }

        // 解析子组件
        const resourceRegex = /data-sly-resource\s*=\s*['"]([^'"]+)['"][^>]*resourceType=['"]([^'"]+)['"]/g;
        while ((match = resourceRegex.exec(content)) !== null) {
            deps.childComponents.push({
                path: match[1],
                resourceType: match[2]
            });
        }
    }

    async analyzeClientLibs(categories, deps) {
        const allClientLibs = {
            categories: [],
            cssFiles: [],
            jsFiles: [],
            dependencies: []
        };

        const processed = new Set();
        const queue = [...categories];

        while (queue.length > 0) {
            const category = queue.shift();
            if (processed.has(category)) continue;
            processed.add(category);

            try {
                // 查找 ClientLib 配置
                const clientLibInfo = await this.findClientLib(category);
                if (clientLibInfo) {
                    allClientLibs.categories.push(category);
                    allClientLibs.cssFiles.push(...clientLibInfo.cssFiles);
                    allClientLibs.jsFiles.push(...clientLibInfo.jsFiles);
                    
                    // 添加依赖到队列
                    if (clientLibInfo.dependencies) {
                        queue.push(...clientLibInfo.dependencies);
                        allClientLibs.dependencies.push(...clientLibInfo.dependencies);
                    }
                }
            } catch (e) {
                console.warn(`无法找到 ClientLib: ${category}`, e.message);
            }
        }

        deps.clientlibs = allClientLibs;
    }

    async findClientLib(category) {
        // 通过 AEM API 查找 ClientLib
        // 或解析 JCR 结构
        // 返回 { cssFiles: [], jsFiles: [], dependencies: [] }
        return null;
    }

    async fetchJSON(url) {
        const fullUrl = `${this.baseUrl}${url}`;
        const auth = Buffer.from(this.credentials).toString('base64');
        const response = await fetch(fullUrl, {
            headers: { 'Authorization': `Basic ${auth}` }
        });
        return await response.json();
    }

    async fetchText(url) {
        const fullUrl = `${this.baseUrl}${url}`;
        const auth = Buffer.from(this.credentials).toString('base64');
        const response = await fetch(fullUrl, {
            headers: { 'Authorization': `Basic ${auth}` }
        });
        return await response.text();
    }
}

// 使用示例
const analyzer = new ComponentAnalyzer(
    'http://localhost:4502',
    'admin:admin'
);

analyzer.analyzeComponent('/apps/myapp/components/card')
    .then(deps => {
        console.log(JSON.stringify(deps, null, 2));
        fs.writeFileSync('component-deps.json', JSON.stringify(deps, null, 2));
    });
```

### 11.3 依赖信息输出格式（React 迁移用）

对于 React 迁移，建议输出以下 JSON 格式：

```json
{
  "component": {
    "resourceType": "myapp/components/card",
    "resourceSuperType": "myapp/components/base",
    "path": "/apps/myapp/components/card",
    "template": {
      "path": "/apps/myapp/components/card/card.html",
      "type": "HTL",
      "content": "..."
    }
  },
  "clientlibs": {
    "categories": [
      "myapp.components.card",
      "myapp.base",
      "myapp.theme"
    ],
    "css": [
      {
        "category": "myapp.components.card",
        "files": [
          "/apps/myapp/clientlibs/components/card/css/card.css"
        ],
        "content": "..."
      }
    ],
    "js": [
      {
        "category": "myapp.components.card",
        "files": [
          "/apps/myapp/clientlibs/components/card/js/card.js"
        ],
        "content": "..."
      }
    ],
    "dependencies": {
      "myapp.components.card": ["myapp.base", "myapp.theme"]
    }
  },
  "slingModels": [
    {
      "className": "com.myapp.models.CardModel",
      "package": "com.myapp.models",
      "interface": {
        "methods": [
          {
            "name": "getTitle",
            "returnType": "String"
          },
          {
            "name": "getDescription",
            "returnType": "String"
          }
        ]
      },
      "properties": {
        "title": "String",
        "description": "String",
        "image": "String"
      }
    }
  ],
  "childComponents": [
    {
      "path": "content",
      "resourceType": "foundation/components/parsys",
      "dependencies": {
        // 递归分析的子组件依赖
      }
    }
  ],
  "dialog": {
    "jcr:primaryType": "cq:Dialog",
    "items": {
      // 对话框配置
    }
  },
  "properties": {
    "jcr:title": "Card Component",
    "componentGroup": "MyApp"
  },
  "templates": [
    {
      "name": "cardTemplate",
      "parameters": ["title", "content"]
    }
  ]
}
```

### 11.4 完整的依赖分析工作流

#### 步骤 1: 准备分析环境

```bash
# 1. 安装必要的工具
npm install -g @adobe/aem-cli
# 或使用 AEM Package Manager

# 2. 配置 AEM 连接
export AEM_HOST=localhost:4502
export AEM_USER=admin
export AEM_PASS=admin
```

#### 步骤 2: 提取组件信息

```bash
# 使用脚本提取单个组件
node analyze-component.js /apps/myapp/components/card

# 批量提取所有组件
node analyze-all-components.js /apps/myapp/components
```

#### 步骤 3: 生成依赖报告

```bash
# 生成 JSON 报告
node generate-dependency-report.js \
  --component /apps/myapp/components/card \
  --output card-dependencies.json \
  --format json

# 生成可视化依赖图
node generate-dependency-graph.js \
  --input card-dependencies.json \
  --output card-dependencies.svg
```

#### 步骤 4: 导出资源文件

```bash
# 导出 CSS 文件
node export-clientlibs.js \
  --categories "myapp.components.card,myapp.base" \
  --output-dir ./exported-styles

# 导出图片和静态资源
node export-assets.js \
  --component /apps/myapp/components/card \
  --output-dir ./exported-assets
```

### 11.5 React 迁移时的依赖映射

将 AEM 依赖映射到 React 项目结构：

| AEM 元素 | React 对应 | 说明 |
|---------|-----------|------|
| `cq:Component` 节点 | React 组件类/函数 | 组件定义 |
| `sling:resourceType` | 组件 ID/路径 | 唯一标识符 |
| `sling:resourceSuperType` | 组件继承/组合 | 父组件引用 |
| `_cq_dialog` 字段 | React Props 接口 | 可编辑属性映射 |
| `_cq_dialog` 配置 | React 表单组件 | 内容编辑界面 |
| `_cq_design_dialog` | 设计时配置 | 模板配置 |
| `_cq_editConfig` | 编辑行为配置 | 编辑模式逻辑 |
| HTL 脚本 (`hero.html`) | JSX 渲染逻辑 | 模板转换 |
| ClientLib CSS | `Card.module.css` | CSS Modules 或 styled-components |
| ClientLib JS | `Card.jsx` | React 组件逻辑 |
| Sling Model | `CardModel.js` | 数据模型/Service |
| `properties` (JCR 属性) | Component Props | 数据输入 |
| 子组件 (parsys) | `<ChildComponent />` | React 组件导入 |
| Parsys 顺序 | 数组顺序渲染 | 保持子组件顺序 |

**完整的映射示例**:

```
AEM 组件结构:
/apps/myapp/components/hero/
  ├── .content.xml
  │   ├── jcr:title = "Hero Component"
  │   ├── sling:resourceSuperType = "myapp/components/base"
  │   └── componentGroup = "MyApp - Content"
  ├── hero.html
  │   └── HTL 模板逻辑
  └── _cq_dialog/
      └── .content.xml
          └── 字段: title, description, imagePath

映射到 React:
src/components/Hero/
  ├── Hero.tsx                    # 主组件（对应 hero.html）
  ├── Hero.types.ts              # Props 类型（对应 _cq_dialog 字段）
  ├── Hero.module.css            # 样式（对应 ClientLib CSS）
  ├── Hero.config.ts              # 配置（对应 .content.xml 属性）
  └── Hero.stories.tsx           # Storybook（对应对话框预览）
```

**对话框字段到 React Props 的完整映射**:

```javascript
// AEM 对话框配置
{
  "title": {
    "sling:resourceType": "granite/ui/components/coral/foundation/form/textfield",
    "name": "./title",
    "fieldLabel": "Title"
  },
  "imagePath": {
    "sling:resourceType": "granite/ui/components/coral/foundation/form/pathfield",
    "name": "./imagePath",
    "fieldLabel": "Image"
  },
  "showDescription": {
    "sling:resourceType": "granite/ui/components/coral/foundation/form/checkbox",
    "name": "./showDescription",
    "text": "Show Description"
  }
}

// 映射到 React TypeScript Props
export interface HeroProps {
  title?: string;              // 来自 ./title (textfield)
  imagePath?: string;          // 来自 ./imagePath (pathfield)
  showDescription?: boolean;   // 来自 ./showDescription (checkbox)
  description?: string;        // 来自 ./description (如果存在)
}

// 映射到 React 表单组件（用于 CMS 编辑）
export const HeroDialog = () => {
  return (
    <form>
      <TextField name="title" label="Title" />
      <PathField name="imagePath" label="Image" />
      <Checkbox name="showDescription" label="Show Description" />
    </form>
  );
};
```

**HTL 脚本到 JSX 的转换模式**:

```html
<!-- AEM HTL 模板 -->
<div class="hero">
    <h1 data-sly-test="${properties.title}">${properties.title}</h1>
    <img data-sly-test="${properties.imagePath}"
         src="${properties.imagePath}"
         alt="${properties.altText || properties.title}">
    <div data-sly-test="${properties.showDescription && properties.description}">
        <p>${properties.description}</p>
    </div>
</div>
```

```typescript
// React JSX 等价代码
export const Hero: React.FC<HeroProps> = ({
  title,
  imagePath,
  altText,
  showDescription,
  description
}) => {
  return (
    <div className={styles.hero}>
      {title && <h1>{title}</h1>}
      {imagePath && (
        <img
          src={imagePath}
          alt={altText || title}
        />
      )}
      {showDescription && description && (
        <div>
          <p>{description}</p>
        </div>
      )}
    </div>
  );
};
```

**转换规则对照表**:

| HTL 语法 | React JSX | 说明 |
|---------|----------|------|
| `${properties.title}` | `{title}` | 属性访问 |
| `data-sly-test="${condition}"` | `{condition && <div>...</div>}` | 条件渲染 |
| `data-sly-list="${items}"` | `{items.map(item => ...)}` | 循环渲染 |
| `data-sly-resource="${'child'}"` | `<ChildComponent />` | 子组件包含 |
| `data-sly-attribute.class="${...}"` | `className={...}` | 动态属性 |
| `data-sly-use.model="${'...'}"` | `const model = useModel()` | Use API |
| `data-sly-call="${template}"` | `<TemplateComponent />` | 模板调用 |
| `${value \|\| 'default'}` | `{value \|\| 'default'}` | 默认值 |
| `data-sly-unescape="${html}"` | `dangerouslySetInnerHTML` | HTML 注入（谨慎使用）|

### 11.6 实际案例：Card 组件迁移

**AEM HTL 组件** (`/apps/myapp/components/card/card.html`):

```html
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-use.model="${'com.myapp.models.CardModel'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.card'}"></sly>

<div class="card">
    <h2>${model.title}</h2>
    <p>${model.description}</p>
    <div data-sly-resource="${'content' @ resourceType='foundation/components/parsys'}"></div>
</div>
```

**分析后的依赖信息**:

```json
{
  "component": "myapp/components/card",
  "clientlibs": ["myapp.components.card"],
  "slingModels": ["com.myapp.models.CardModel"],
  "childComponents": [
    {
      "path": "content",
      "resourceType": "foundation/components/parsys"
    }
  ]
}
```

**React 组件结构**:

```
src/
  components/
    Card/
      Card.jsx              # 主组件
      Card.module.css       # 样式（从 ClientLib 迁移）
      Card.config.js        # 配置（从 Dialog 迁移）
  models/
    CardModel.js            # 数据模型（从 Sling Model 迁移）
```

### 11.7 自动化迁移工具建议

创建一个完整的迁移工具链：

```bash
# 1. 分析工具
aem-react-migrate analyze \
  --component /apps/myapp/components/card \
  --output deps.json

# 2. 生成 React 组件骨架
aem-react-migrate generate \
  --input deps.json \
  --template react-functional \
  --output src/components/Card

# 3. 转换样式
aem-react-migrate convert-styles \
  --input deps.json \
  --output src/components/Card/Card.module.css

# 4. 生成数据模型
aem-react-migrate generate-model \
  --sling-model com.myapp.models.CardModel \
  --output src/models/CardModel.js
```

### 11.8 Sling Model 接口详细提取

对于 React 迁移，需要提取 Sling Model 的完整接口定义。以下是几种方法：

#### 方法 1: 从源代码提取（推荐）

如果能够访问源代码，直接分析 Java 接口：

```java
// 使用 Java 反射或 AST 解析工具
// 示例：使用 JavaParser 库
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

public class ModelExtractor {
    public ModelInfo extractModel(String javaSource) {
        CompilationUnit cu = JavaParser.parse(javaSource);
        ModelInfo info = new ModelInfo();
        
        cu.findAll(MethodDeclaration.class).forEach(method -> {
            info.addMethod({
                name: method.getNameAsString(),
                returnType: method.getType().asString(),
                parameters: method.getParameters().stream()
                    .map(p -> p.getNameAsString() + ": " + p.getType())
                    .collect(Collectors.toList())
            });
        });
        
        return info;
    }
}
```

#### 方法 2: 从编译后的类文件提取

使用反射 API 提取接口信息：

```java
// Groovy 脚本（AEM Groovy Console）
import java.lang.reflect.Method
import com.myapp.models.CardModel

def modelClass = CardModel.class
def modelInfo = [:]

modelInfo.className = modelClass.name
modelInfo.packageName = modelClass.package.name
modelInfo.methods = []

modelClass.declaredMethods.each { Method method ->
    def methodInfo = [:]
    methodInfo.name = method.name
    methodInfo.returnType = method.returnType.name
    methodInfo.parameters = method.parameterTypes.collect { it.name }
    methodInfo.isGetter = method.name.startsWith("get") || method.name.startsWith("is")
    
    modelInfo.methods << methodInfo
}

// 输出 JSON
import groovy.json.JsonBuilder
def json = new JsonBuilder(modelInfo)
println json.toPrettyString()
```

#### 方法 3: 从 HTL 使用模式推断

分析 HTL 模板中如何使用 Model，推断接口：

```javascript
// Node.js 脚本
function inferModelInterface(htlContent, modelClass) {
    const interface = {
        className: modelClass,
        methods: [],
        properties: []
    };
    
    // 提取所有 ${model.xxx} 的使用
    const usageRegex = /\$\{model\.(\w+)\}/g;
    const usages = new Set();
    let match;
    
    while ((match = usageRegex.exec(htlContent)) !== null) {
        usages.add(match[1]);
    }
    
    // 推断方法名（camelCase to getter）
    usages.forEach(usage => {
        const methodName = 'get' + usage.charAt(0).toUpperCase() + usage.slice(1);
        interface.methods.push({
            name: methodName,
            property: usage,
            inferred: true  // 标记为推断的
        });
    });
    
    return interface;
}
```

#### 输出格式

```json
{
  "slingModels": [
    {
      "className": "com.myapp.models.CardModel",
      "package": "com.myapp.models",
      "interface": {
        "methods": [
          {
            "name": "getTitle",
            "returnType": "String",
            "parameters": [],
            "property": "title",
            "source": "interface"  // 或 "inferred"
          },
          {
            "name": "getDescription",
            "returnType": "String",
            "parameters": [],
            "property": "description"
          },
          {
            "name": "hasImage",
            "returnType": "boolean",
            "parameters": [],
            "property": "image"
          }
        ]
      },
      "reactMapping": {
        "component": "CardModel",
        "path": "src/models/CardModel.js",
        "typeDefinition": "src/models/CardModel.d.ts"
      }
    }
  ]
}
```

### 11.9 国际化资源提取

提取组件使用的所有国际化字符串：

#### 方法 1: 从 HTL 模板提取

```javascript
function extractI18nStrings(htlContent) {
    const i18nStrings = [];
    
    // 匹配 i18n.get() 调用
    const i18nRegex = /i18n\.get\(['"]([^'"]+)['"]/g;
    let match;
    
    while ((match = i18nRegex.exec(htlContent)) !== null) {
        i18nStrings.push({
            key: match[1],
            source: "i18n.get",
            line: htlContent.substring(0, match.index).split('\n').length
        });
    }
    
    // 匹配 ${'key' @ i18n}
    const htlI18nRegex = /\$\{['"]([^'"]+)['"]\s*@\s*i18n\}/g;
    while ((match = htlI18nRegex.exec(htlContent)) !== null) {
        i18nStrings.push({
            key: match[1],
            source: "htl-i18n",
            line: htlContent.substring(0, match.index).split('\n').length
        });
    }
    
    return i18nStrings;
}
```

#### 方法 2: 从字典文件提取

```bash
# 提取特定语言的字典
curl -u admin:admin \
  "http://localhost:4502/content/dictionaries/myapp/en.json" \
  -o i18n/en.json

curl -u admin:admin \
  "http://localhost:4502/content/dictionaries/myapp/zh-CN.json" \
  -o i18n/zh-CN.json
```

#### 输出格式

```json
{
  "i18n": {
    "strings": [
      {
        "key": "card.title.label",
        "defaultValue": "Card Title",
        "usage": [
          {
            "file": "card.html",
            "line": 25,
            "context": "${i18n.get('card.title.label')}"
          }
        ],
        "translations": {
          "en": "Card Title",
          "zh-CN": "卡片标题",
          "es": "Título de Tarjeta"
        }
      }
    ],
    "dictionaries": [
      "/content/dictionaries/myapp/en",
      "/content/dictionaries/myapp/zh-CN"
    ],
    "reactMapping": {
      "format": "react-i18next",
      "namespace": "card",
      "files": [
        "src/locales/en/card.json",
        "src/locales/zh-CN/card.json"
      ]
    }
  }
}
```

### 11.10 对话框到 React Props 映射

将 AEM 对话框配置转换为 React 组件的 Props 定义：

#### 对话框字段类型映射

| AEM 字段类型 | React PropType | TypeScript | 说明 |
|------------|---------------|------------|------|
| `textfield` | `PropTypes.string` | `string` | 文本输入 |
| `textarea` | `PropTypes.string` | `string` | 多行文本 |
| `pathfield` | `PropTypes.string` | `string` | 路径选择 |
| `richtext` | `PropTypes.string` | `string` | 富文本 |
| `checkbox` | `PropTypes.bool` | `boolean` | 复选框 |
| `numberfield` | `PropTypes.number` | `number` | 数字输入 |
| `selection` | `PropTypes.oneOf` | `'option1' \| 'option2'` | 下拉选择 |
| `multifield` | `PropTypes.array` | `Array<T>` | 多字段数组 |
| `imagefield` | `PropTypes.string` | `string` | 图片路径 |
| `datepicker` | `PropTypes.string` | `string` | 日期选择 |

#### 转换工具示例

```javascript
function convertDialogToProps(dialogConfig) {
    const props = {
        typescript: [],
        propTypes: [],
        defaultValues: {}
    };
    
    function processField(field, prefix = '') {
        const name = prefix ? `${prefix}.${field.name}` : field.name;
        const propName = camelCase(name);
        
        switch (field.xtype) {
            case 'textfield':
            case 'textarea':
            case 'pathfield':
            case 'richtext':
                props.typescript.push(`${propName}?: string;`);
                props.propTypes.push(`${propName}: PropTypes.string,`);
                if (field.value) {
                    props.defaultValues[propName] = field.value;
                }
                break;
                
            case 'checkbox':
                props.typescript.push(`${propName}?: boolean;`);
                props.propTypes.push(`${propName}: PropTypes.bool,`);
                props.defaultValues[propName] = field.checked || false;
                break;
                
            case 'numberfield':
                props.typescript.push(`${propName}?: number;`);
                props.propTypes.push(`${propName}: PropTypes.number,`);
                if (field.value !== undefined) {
                    props.defaultValues[propName] = Number(field.value);
                }
                break;
                
            case 'selection':
                const options = field.options || [];
                const optionValues = options.map(opt => 
                    typeof opt === 'string' ? opt : opt.value
                );
                props.typescript.push(
                    `${propName}?: ${optionValues.map(v => `'${v}'`).join(' | ')};`
                );
                props.propTypes.push(
                    `${propName}: PropTypes.oneOf([${optionValues.map(v => `'${v}'`).join(', ')}]),`
                );
                break;
                
            case 'multifield':
                props.typescript.push(`${propName}?: Array<any>;`);
                props.propTypes.push(`${propName}: PropTypes.array,`);
                props.defaultValues[propName] = [];
                // 递归处理子字段
                if (field.items) {
                    field.items.forEach(item => processField(item, name));
                }
                break;
        }
    }
    
    // 处理对话框配置
    if (dialogConfig.items && dialogConfig.items.tab1) {
        dialogConfig.items.tab1.items.forEach(field => {
            processField(field);
        });
    }
    
    return props;
}
```

#### 生成的 React 代码示例

```typescript
// Card.props.ts (TypeScript)
export interface CardProps {
    title?: string;
    description?: string;
    imagePath?: string;
    showImage?: boolean;
    cardType?: 'default' | 'featured' | 'compact';
    tags?: Array<string>;
    linkUrl?: string;
    openInNewTab?: boolean;
}

// Card.jsx (PropTypes)
import PropTypes from 'prop-types';

Card.propTypes = {
    title: PropTypes.string,
    description: PropTypes.string,
    imagePath: PropTypes.string,
    showImage: PropTypes.bool,
    cardType: PropTypes.oneOf(['default', 'featured', 'compact']),
    tags: PropTypes.array,
    linkUrl: PropTypes.string,
    openInNewTab: PropTypes.bool,
};

Card.defaultProps = {
    showImage: true,
    cardType: 'default',
    tags: [],
    openInNewTab: false,
};
```

### 11.11 依赖冲突处理

在分析过程中可能遇到依赖冲突，需要识别和处理：

#### 常见的依赖冲突

1. **ClientLib 版本冲突**
   - 同一 category 在不同位置定义
   - 不同的依赖版本

2. **Sling Model 名称冲突**
   - 不同包中的同名类
   - 接口和实现类混淆

3. **CSS 类名冲突**
   - 不同组件使用相同的 CSS 类名
   - 全局样式污染

#### 冲突检测方法

```javascript
function detectConflicts(dependencies) {
    const conflicts = {
        clientlibs: [],
        models: [],
        cssClasses: []
    };
    
    // 检测 ClientLib 冲突
    const clientLibMap = new Map();
    dependencies.clientlibs.forEach(lib => {
        if (clientLibMap.has(lib.category)) {
            conflicts.clientlibs.push({
                category: lib.category,
                locations: [
                    clientLibMap.get(lib.category),
                    lib.path
                ]
            });
        } else {
            clientLibMap.set(lib.category, lib.path);
        }
    });
    
    // 检测 CSS 类名冲突
    const classUsage = new Map();
    dependencies.css.forEach(cssFile => {
        // 提取 CSS 类名（简化版）
        const classRegex = /\.([a-zA-Z_-][\w-]*)\s*\{/g;
        let match;
        while ((match = classRegex.exec(cssFile.content)) !== null) {
            const className = match[1];
            if (!classUsage.has(className)) {
                classUsage.set(className, []);
            }
            classUsage.get(className).push(cssFile.path);
        }
    });
    
    // 找出在多个文件中使用的类名
    classUsage.forEach((files, className) => {
        if (files.length > 1) {
            conflicts.cssClasses.push({
                className,
                files
            });
        }
    });
    
    return conflicts;
}
```

#### 冲突解决策略

1. **ClientLib 冲突**: 优先使用 `/apps` 层级的定义
2. **Sling Model 冲突**: 使用完整的包名限定
3. **CSS 类名冲突**: 使用 CSS Modules 或 BEM 命名规范

### 11.12 错误处理和边界情况

分析过程中可能遇到的错误和解决方案：

#### 常见错误

1. **资源不存在**
   ```javascript
   try {
       const resource = await fetchResource(path);
   } catch (error) {
       if (error.status === 404) {
           console.warn(`资源不存在: ${path}`);
           // 使用默认值或跳过
       }
   }
   ```

2. **权限不足**
   ```javascript
   if (error.status === 403) {
       console.error(`无权限访问: ${path}`);
       // 记录到报告中，需要手动处理
   }
   ```

3. **循环依赖**
   ```javascript
   const analyzed = new Set();
   function analyzeComponent(path) {
       if (analyzed.has(path)) {
           console.warn(`检测到循环依赖: ${path}`);
           return null;  // 返回已分析的结果
       }
       analyzed.add(path);
       // ... 分析逻辑
   }
   ```

4. **模板解析失败**
   ```javascript
   try {
       parseHTL(templateContent);
   } catch (error) {
       console.warn(`模板解析失败: ${error.message}`);
       // 使用正则表达式进行简单提取
       return extractBasicInfo(templateContent);
   }
   ```

#### 边界情况处理

- **空组件**: 组件可能没有 HTL 模板，只有 Java 类
- **动态 ResourceType**: ResourceType 可能通过表达式动态计算
- **条件 ClientLib**: ClientLib 可能通过条件加载
- **外部依赖**: 组件可能依赖外部 API 或服务

### 11.13 完整的端到端示例

完整的组件分析和迁移工作流：

```bash
#!/bin/bash
# complete-migration-workflow.sh

COMPONENT_PATH="/apps/myapp/components/card"
OUTPUT_DIR="./migration-output"
REACT_PROJECT_DIR="../react-app"

echo "=== 步骤 1: 分析组件依赖 ==="
node component-dependency-analyzer.js \
    "$COMPONENT_PATH" \
    --output "$OUTPUT_DIR/dependencies.json" \
    --format json \
    --recursive

echo "=== 步骤 2: 检测冲突 ==="
node detect-conflicts.js \
    --input "$OUTPUT_DIR/dependencies.json" \
    --output "$OUTPUT_DIR/conflicts.json"

echo "=== 步骤 3: 导出资源文件 ==="
# 导出 CSS
node export-clientlibs.js \
    --input "$OUTPUT_DIR/dependencies.json" \
    --output-dir "$OUTPUT_DIR/styles"

# 导出图片和资产
node export-assets.js \
    --component "$COMPONENT_PATH" \
    --output-dir "$OUTPUT_DIR/assets"

# 导出国际化字典
node export-i18n.js \
    --input "$OUTPUT_DIR/dependencies.json" \
    --output-dir "$OUTPUT_DIR/locales"

echo "=== 步骤 4: 生成 React 代码 ==="
# 生成组件骨架
node generate-react-component.js \
    --input "$OUTPUT_DIR/dependencies.json" \
    --template functional \
    --output "$REACT_PROJECT_DIR/src/components/Card"

# 生成 TypeScript 类型定义
node generate-typescript-defs.js \
    --input "$OUTPUT_DIR/dependencies.json" \
    --output "$REACT_PROJECT_DIR/src/components/Card/Card.types.ts"

# 转换样式（CSS Modules）
node convert-styles.js \
    --input-dir "$OUTPUT_DIR/styles" \
    --output "$REACT_PROJECT_DIR/src/components/Card/Card.module.css" \
    --format css-modules

echo "=== 步骤 5: 生成迁移报告 ==="
node generate-migration-report.js \
    --input "$OUTPUT_DIR/dependencies.json" \
    --output "$OUTPUT_DIR/migration-report.md"

echo "=== 完成！==="
echo "迁移输出目录: $OUTPUT_DIR"
echo "React 组件位置: $REACT_PROJECT_DIR/src/components/Card"
```

### 11.14 依赖关系可视化

将分析结果可视化为依赖关系图，有助于理解组件结构：

#### 使用 Graphviz 生成依赖图

```javascript
// generate-dependency-graph.js
const fs = require('fs');
const { execSync } = require('child_process');

function generateGraphviz(dependencies, outputFile) {
    let dotContent = 'digraph ComponentDependencies {\n';
    dotContent += '  rankdir=LR;\n';
    dotContent += '  node [shape=box];\n\n';
    
    // 组件节点
    dotContent += `  "${dependencies.component.resourceType}" [label="${dependencies.component.resourceType}", style=filled, fillcolor=lightblue];\n\n`;
    
    // ClientLib 依赖
    dependencies.clientlibs.categories.forEach(category => {
        dotContent += `  "${category}" [label="${category}", shape=ellipse, fillcolor=lightgreen];\n`;
        dotContent += `  "${dependencies.component.resourceType}" -> "${category}" [label="uses"];\n`;
    });
    
    // ClientLib 之间的依赖
    Object.keys(dependencies.clientlibs.dependencies).forEach(category => {
        dependencies.clientlibs.dependencies[category].forEach(dep => {
            dotContent += `  "${category}" -> "${dep}" [label="depends", style=dashed];\n`;
        });
    });
    
    // Sling Models
    dependencies.slingModels.forEach(model => {
        const modelName = model.className.split('.').pop();
        dotContent += `  "${modelName}" [label="${modelName}", shape=hexagon, fillcolor=lightyellow];\n`;
        dotContent += `  "${dependencies.component.resourceType}" -> "${modelName}" [label="uses"];\n`;
    });
    
    // 子组件
    dependencies.childComponents.forEach(child => {
        const childName = child.resourceType.split('/').pop();
        dotContent += `  "${childName}" [label="${child.resourceType}", shape=box, fillcolor=lightgray];\n`;
        dotContent += `  "${dependencies.component.resourceType}" -> "${childName}" [label="includes"];\n`;
    });
    
    dotContent += '}\n';
    
    fs.writeFileSync('dependencies.dot', dotContent);
    
    // 生成 PNG 图片
    try {
        execSync('dot -Tpng dependencies.dot -o ' + outputFile);
        console.log(`依赖图已生成: ${outputFile}`);
    } catch (error) {
        console.warn('Graphviz 未安装，无法生成图片。安装方法: brew install graphviz');
    }
}
```

#### 使用 Mermaid 生成图表

```javascript
function generateMermaid(dependencies) {
    let mermaid = 'graph TD\n';
    
    const componentId = 'component';
    mermaid += `    ${componentId}["${dependencies.component.resourceType}"]\n`;
    
    // ClientLibs
    dependencies.clientlibs.categories.forEach((category, index) => {
        const id = `clientlib${index}`;
        mermaid += `    ${id}["${category}"]\n`;
        mermaid += `    ${componentId} -->|uses| ${id}\n`;
    });
    
    // Sling Models
    dependencies.slingModels.forEach((model, index) => {
        const modelName = model.className.split('.').pop();
        const id = `model${index}`;
        mermaid += `    ${id}["${modelName}"]\n`;
        mermaid += `    ${componentId} -->|uses| ${id}\n`;
    });
    
    return mermaid;
}
```

#### 使用 D3.js 交互式可视化

```javascript
// 生成 D3.js 可用的 JSON 格式
function generateD3Data(dependencies) {
    const nodes = [];
    const links = [];
    
    // 添加组件节点
    nodes.push({
        id: dependencies.component.resourceType,
        type: 'component',
        name: dependencies.component.resourceType
    });
    
    // 添加 ClientLib 节点
    dependencies.clientlibs.categories.forEach(category => {
        nodes.push({
            id: category,
            type: 'clientlib',
            name: category
        });
        links.push({
            source: dependencies.component.resourceType,
            target: category,
            type: 'uses'
        });
    });
    
    // 添加 Sling Model 节点
    dependencies.slingModels.forEach(model => {
        const modelName = model.className.split('.').pop();
        nodes.push({
            id: model.className,
            type: 'model',
            name: modelName
        });
        links.push({
            source: dependencies.component.resourceType,
            target: model.className,
            type: 'uses'
        });
    });
    
    return { nodes, links };
}
```

### 11.15 性能分析识别

在依赖分析过程中识别潜在的性能问题：

#### 识别大文件依赖

```javascript
function identifyLargeDependencies(dependencies) {
    const warnings = [];
    
    // 检查 CSS 文件大小
    dependencies.clientlibs.css.forEach(css => {
        css.files.forEach(file => {
            if (file.size > 100 * 1024) {  // 100KB
                warnings.push({
                    type: 'large-css',
                    file: file.path,
                    size: file.size,
                    recommendation: '考虑拆分或优化 CSS'
                });
            }
        });
    });
    
    // 检查 JS 文件大小
    dependencies.clientlibs.js.forEach(js => {
        js.files.forEach(file => {
            if (file.size > 200 * 1024) {  // 200KB
                warnings.push({
                    type: 'large-js',
                    file: file.path,
                    size: file.size,
                    recommendation: '考虑代码分割或懒加载'
                });
            }
        });
    });
    
    // 检查依赖深度
    const maxDepth = calculateDependencyDepth(dependencies);
    if (maxDepth > 5) {
        warnings.push({
            type: 'deep-dependency',
            depth: maxDepth,
            recommendation: '依赖层级过深，考虑重构'
        });
    }
    
    return warnings;
}

function calculateDependencyDepth(dependencies, depth = 0, visited = new Set()) {
    let maxDepth = depth;
    const componentId = dependencies.component.resourceType;
    
    if (visited.has(componentId)) {
        return depth;  // 循环依赖
    }
    visited.add(componentId);
    
    dependencies.childComponents.forEach(child => {
        if (child.dependencies) {
            const childDepth = calculateDependencyDepth(
                child.dependencies,
                depth + 1,
                new Set(visited)
            );
            maxDepth = Math.max(maxDepth, childDepth);
        }
    });
    
    return maxDepth;
}
```

#### 识别未使用的依赖

```javascript
function identifyUnusedDependencies(dependencies) {
    const unused = [];
    
    // 检查 ClientLib 是否真的被使用
    dependencies.clientlibs.categories.forEach(category => {
        // 检查 CSS 类名是否在模板中使用
        const cssClasses = extractCssClasses(dependencies, category);
        const usedClasses = extractUsedClasses(dependencies.component.template.content);
        
        const unusedClasses = cssClasses.filter(cls => !usedClasses.has(cls));
        if (unusedClasses.length > cssClasses.length * 0.5) {
            unused.push({
                type: 'unused-css',
                category,
                unusedClasses: unusedClasses.length,
                recommendation: `考虑移除未使用的 CSS 类或整个 ClientLib`
            });
        }
    });
    
    // 检查 Sling Model 方法是否被使用
    dependencies.slingModels.forEach(model => {
        if (model.interface && model.interface.methods) {
            const usedMethods = extractUsedMethods(dependencies.component.template.content);
            const unusedMethods = model.interface.methods.filter(
                m => !usedMethods.has(m.name)
            );
            
            if (unusedMethods.length > 0) {
                unused.push({
                    type: 'unused-model-methods',
                    model: model.className,
                    unusedMethods: unusedMethods.map(m => m.name),
                    recommendation: '考虑移除未使用的方法'
                });
            }
        }
    });
    
    return unused;
}
```

### 11.16 增量分析

对于大型项目，增量分析可以提高效率：

#### 基于 Git 的增量分析

```javascript
// incremental-analyzer.js
const { execSync } = require('child_process');

function getChangedComponents(sinceCommit = 'HEAD~1') {
    // 获取变更的文件
    const changedFiles = execSync(
        `git diff --name-only ${sinceCommit} HEAD`,
        { encoding: 'utf-8' }
    ).trim().split('\n');
    
    const changedComponents = new Set();
    
    changedFiles.forEach(file => {
        // 匹配组件路径
        const componentMatch = file.match(/\/apps\/[^\/]+\/components\/([^\/]+)/);
        if (componentMatch) {
            changedComponents.add(componentMatch[0]);
        }
        
        // 匹配 ClientLib 路径
        const clientLibMatch = file.match(/\/apps\/[^\/]+\/clientlibs\/(.+)/);
        if (clientLibMatch) {
            // 找出使用此 ClientLib 的组件
            const affectedComponents = findComponentsUsingClientLib(clientLibMatch[1]);
            affectedComponents.forEach(comp => changedComponents.add(comp));
        }
    });
    
    return Array.from(changedComponents);
}

function analyzeIncremental(changedComponents, baseAnalysis) {
    const results = {
        changed: [],
        affected: [],
        unchanged: baseAnalysis
    };
    
    changedComponents.forEach(componentPath => {
        // 分析变更的组件
        const analysis = analyzeComponent(componentPath);
        results.changed.push(analysis);
        
        // 找出受影响的组件（依赖此组件的组件）
        const affected = findDependentComponents(componentPath, baseAnalysis);
        results.affected.push(...affected);
    });
    
    return results;
}
```

#### 缓存机制

```javascript
const fs = require('fs');
const crypto = require('crypto');

class AnalysisCache {
    constructor(cacheDir = '.analysis-cache') {
        this.cacheDir = cacheDir;
        if (!fs.existsSync(cacheDir)) {
            fs.mkdirSync(cacheDir, { recursive: true });
        }
    }
    
    getCacheKey(componentPath, options = {}) {
        const content = JSON.stringify({ componentPath, options });
        return crypto.createHash('md5').update(content).digest('hex');
    }
    
    get(componentPath, options) {
        const key = this.getCacheKey(componentPath, options);
        const cacheFile = `${this.cacheDir}/${key}.json`;
        
        if (fs.existsSync(cacheFile)) {
            const cached = JSON.parse(fs.readFileSync(cacheFile, 'utf-8'));
            
            // 检查是否过期（例如：组件文件是否已修改）
            if (this.isValid(cached, componentPath)) {
                return cached.data;
            }
        }
        
        return null;
    }
    
    set(componentPath, options, data) {
        const key = this.getCacheKey(componentPath, options);
        const cacheFile = `${this.cacheDir}/${key}.json`;
        
        const cached = {
            timestamp: Date.now(),
            componentPath,
            data
        };
        
        fs.writeFileSync(cacheFile, JSON.stringify(cached, null, 2));
    }
    
    isValid(cached, componentPath) {
        // 检查组件文件是否已修改
        try {
            const stats = fs.statSync(`${componentPath}/${path.basename(componentPath)}.html`);
            return stats.mtimeMs < cached.timestamp;
        } catch (e) {
            return false;
        }
    }
}
```

### 11.17 与构建工具集成

将依赖分析集成到 React 项目的构建流程中：

#### Webpack 插件示例

```javascript
// webpack-aem-dependency-plugin.js
class AEMDependencyPlugin {
    constructor(options) {
        this.options = options;
    }
    
    apply(compiler) {
        compiler.hooks.beforeCompile.tapAsync('AEMDependencyPlugin', (params, callback) => {
            // 分析 AEM 组件依赖
            const analyzer = new ComponentDependencyAnalyzer(this.options);
            
            analyzer.analyzeComponent(this.options.componentPath)
                .then(deps => {
                    // 生成依赖映射文件
                    const mapping = this.generateWebpackMapping(deps);
                    fs.writeFileSync(
                        'aem-dependencies.json',
                        JSON.stringify(mapping, null, 2)
                    );
                    
                    callback();
                })
                .catch(callback);
        });
    }
    
    generateWebpackMapping(dependencies) {
        return {
            alias: {
                // 将 AEM ClientLib 映射到 React 组件
                '@aem/styles': dependencies.clientlibs.css.map(c => c.files).flat()
            },
            externals: {
                // 外部依赖
            },
            resolve: {
                alias: this.generateAliases(dependencies)
            }
        };
    }
}
```

#### Vite 插件示例

```javascript
// vite-aem-plugin.js
export function aemDependencyPlugin(options) {
    return {
        name: 'aem-dependency',
        config(config) {
            const dependencies = analyzeDependencies(options.componentPath);
            
            return {
                resolve: {
                    alias: generateAliases(dependencies)
                },
                css: {
                    preprocessorOptions: {
                        // 处理 AEM CSS
                    }
                }
            };
        },
        buildStart() {
            // 在构建开始时分析依赖
            this.addWatchFile(options.componentPath);
        }
    };
}
```

### 11.18 完整案例分析：ProductCard 组件

一个完整的实际组件分析示例：

#### AEM 组件结构

```
/apps/myapp/components/product-card/
  ├── product-card.html
  ├── .content.xml
  ├── _cq_dialog/
  │   └── .content.xml
  └── _cq_editConfig.xml
```

#### HTL 模板

```html
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-use.model="${'com.myapp.models.ProductCardModel'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.product-card'}"></sly>

<div class="product-card ${component.cssClassNames}" 
     data-product-id="${model.productId}">
    <div class="product-card__image">
        <img src="${model.imageUrl}"
             srcset="${model.imageSrcset}"
             sizes="${model.imageSizes}"
             alt="${model.imageAlt}"
             loading="lazy">
    </div>
    <div class="product-card__content">
        <h3 class="product-card__title">${model.title}</h3>
        <p class="product-card__price">${model.formattedPrice}</p>
        <div class="product-card__badges">
            <span data-sly-list="${model.badges}"
                  data-sly-attribute.class="'badge badge--' + item.type">
                ${item.label}
            </span>
        </div>
        <div data-sly-test="${model.showDescription}"
             class="product-card__description">
            ${model.description}
        </div>
        <a href="${model.linkUrl}"
           class="product-card__link"
           data-sly-attribute.aria-label="${model.linkAriaLabel}">
            ${i18n.get('product.card.viewDetails')}
        </a>
    </div>
</div>

<sly data-sly-call="${clientlib.js @ categories='myapp.components.product-card', async=true}"></sly>
```

#### 分析结果

```json
{
  "component": {
    "resourceType": "myapp/components/product-card",
    "template": {
      "path": "/apps/myapp/components/product-card/product-card.html",
      "type": "HTL"
    }
  },
  "clientlibs": {
    "categories": ["myapp.components.product-card", "myapp.base"],
    "css": [
      {
        "category": "myapp.components.product-card",
        "files": [
          "/apps/myapp/clientlibs/components/product-card/css/product-card.css"
        ],
        "size": 15420
      }
    ],
    "js": [
      {
        "category": "myapp.components.product-card",
        "files": [
          "/apps/myapp/clientlibs/components/product-card/js/product-card.js"
        ],
        "size": 8765
      }
    ]
  },
  "slingModels": [
    {
      "className": "com.myapp.models.ProductCardModel",
      "methods": [
        {"name": "getProductId", "returnType": "String"},
        {"name": "getTitle", "returnType": "String"},
        {"name": "getImageUrl", "returnType": "String"},
        {"name": "getFormattedPrice", "returnType": "String"},
        {"name": "getBadges", "returnType": "List<Badge>"},
        {"name": "getShowDescription", "returnType": "boolean"},
        {"name": "getDescription", "returnType": "String"},
        {"name": "getLinkUrl", "returnType": "String"},
        {"name": "getLinkAriaLabel", "returnType": "String"}
      ]
    }
  ],
  "i18n": {
    "strings": [
      {
        "key": "product.card.viewDetails",
        "defaultValue": "View Details"
      }
    ]
  },
  "performance": {
    "warnings": [],
    "metrics": {
      "cssSize": 15420,
      "jsSize": 8765,
      "dependencyDepth": 2
    }
  }
}
```

#### React 迁移结果

```typescript
// ProductCard.tsx
import React from 'react';
import styles from './ProductCard.module.css';
import { ProductCardProps } from './ProductCard.types';
import { useTranslation } from 'react-i18next';

export const ProductCard: React.FC<ProductCardProps> = ({
  productId,
  title,
  imageUrl,
  imageSrcset,
  imageSizes,
  imageAlt,
  formattedPrice,
  badges = [],
  showDescription = false,
  description,
  linkUrl,
  linkAriaLabel
}) => {
  const { t } = useTranslation();
  
  return (
    <div className={styles.productCard} data-product-id={productId}>
      <div className={styles.image}>
        <img
          src={imageUrl}
          srcSet={imageSrcset}
          sizes={imageSizes}
          alt={imageAlt}
          loading="lazy"
        />
      </div>
      <div className={styles.content}>
        <h3 className={styles.title}>{title}</h3>
        <p className={styles.price}>{formattedPrice}</p>
        <div className={styles.badges}>
          {badges.map((badge, index) => (
            <span key={index} className={`${styles.badge} ${styles[`badge--${badge.type}`]}`}>
              {badge.label}
            </span>
          ))}
        </div>
        {showDescription && (
          <div className={styles.description}>{description}</div>
        )}
        <a href={linkUrl} className={styles.link} aria-label={linkAriaLabel}>
          {t('product.card.viewDetails')}
        </a>
      </div>
    </div>
  );
};
```

### 11.19 迁移验证和测试

确保迁移后的 React 组件功能与原始 AEM 组件一致：

#### 功能对比测试

```javascript
// migration-verification.js
class MigrationVerifier {
    async verifyComponent(originalComponent, reactComponent, testData) {
        const results = {
            passed: [],
            failed: [],
            warnings: []
        };
        
        // 1. 渲染对比
        const originalHTML = await this.renderAEMComponent(originalComponent, testData);
        const reactHTML = await this.renderReactComponent(reactComponent, testData);
        
        const htmlComparison = this.compareHTML(originalHTML, reactHTML);
        if (htmlComparison.matches) {
            results.passed.push('HTML 结构匹配');
        } else {
            results.failed.push({
                test: 'HTML 结构匹配',
                differences: htmlComparison.differences
            });
        }
        
        // 2. CSS 类名对比
        const cssComparison = this.compareCSSClasses(originalHTML, reactHTML);
        if (cssComparison.matches) {
            results.passed.push('CSS 类名匹配');
        } else {
            results.warnings.push({
                test: 'CSS 类名匹配',
                differences: cssComparison.differences
            });
        }
        
        // 3. 属性对比
        const attrComparison = this.compareAttributes(originalHTML, reactHTML);
        if (attrComparison.matches) {
            results.passed.push('属性匹配');
        } else {
            results.failed.push({
                test: '属性匹配',
                differences: attrComparison.differences
            });
        }
        
        return results;
    }
    
    compareHTML(html1, html2) {
        // 使用 DOM 解析器比较 HTML 结构
        const parser = new DOMParser();
        const doc1 = parser.parseFromString(html1, 'text/html');
        const doc2 = parser.parseFromString(html2, 'text/html');
        
        return this.compareDOMNodes(doc1.body, doc2.body);
    }
    
    compareCSSClasses(html1, html2) {
        const classes1 = this.extractCSSClasses(html1);
        const classes2 = this.extractCSSClasses(html2);
        
        const missing = classes1.filter(c => !classes2.has(c));
        const extra = classes2.filter(c => !classes1.has(c));
        
        return {
            matches: missing.length === 0 && extra.length === 0,
            differences: {
                missing,
                extra
            }
        };
    }
}
```

#### 视觉回归测试

```javascript
// visual-regression-test.js
const puppeteer = require('puppeteer');
const pixelmatch = require('pixelmatch');
const PNG = require('pngjs').PNG;

async function visualRegressionTest(aemUrl, reactUrl) {
    const browser = await puppeteer.launch();
    
    try {
        // 截图 AEM 组件
        const aemPage = await browser.newPage();
        await aemPage.goto(aemUrl);
        const aemScreenshot = await aemPage.screenshot({ fullPage: true });
        
        // 截图 React 组件
        const reactPage = await browser.newPage();
        await reactPage.goto(reactUrl);
        const reactScreenshot = await reactPage.screenshot({ fullPage: true });
        
        // 比较截图
        const img1 = PNG.sync.read(aemScreenshot);
        const img2 = PNG.sync.read(reactScreenshot);
        
        const { width, height } = img1;
        const diff = new PNG({ width, height });
        
        const numDiffPixels = pixelmatch(
            img1.data, img2.data, diff.data,
            width, height,
            { threshold: 0.1 }
        );
        
        const diffPercentage = (numDiffPixels / (width * height)) * 100;
        
        return {
            passed: diffPercentage < 1,  // 差异小于 1%
            diffPercentage,
            diffImage: PNG.sync.write(diff)
        };
    } finally {
        await browser.close();
    }
}
```

#### 测试清单

```javascript
const testChecklist = {
    functionality: [
        '✓ 所有属性正确传递',
        '✓ 条件渲染逻辑一致',
        '✓ 循环渲染正确',
        '✓ 事件处理正确',
        '✓ 数据格式化一致'
    ],
    styling: [
        '✓ CSS 类名正确应用',
        '✓ 样式视觉效果一致',
        '✓ 响应式行为一致',
        '✓ 主题支持一致'
    ],
    accessibility: [
        '✓ ARIA 属性正确',
        '✓ 语义化 HTML 结构',
        '✓ 键盘导航支持',
        '✓ 屏幕阅读器兼容'
    ],
    performance: [
        '✓ 初始加载时间',
        '✓ 交互响应时间',
        '✓ 包体积合理',
        '✓ 代码分割正确'
    ],
    i18n: [
        '✓ 所有翻译字符串正确',
        '✓ 多语言切换正常',
        '✓ RTL 语言支持',
        '✓ 日期/数字格式化'
    ]
};
```

### 11.20 依赖分析检查清单

在开始 React 迁移前，确保收集了以下信息：

- [ ] 组件 Resource Type 和 Super Type
- [ ] HTL 模板内容
- [ ] 所有 ClientLib categories（包括依赖）
- [ ] CSS 文件内容和路径
- [ ] JavaScript 文件内容和依赖
- [ ] Sling Model 类名和**完整接口定义**（方法签名、返回类型）
- [ ] 所有子组件的 Resource Type（递归）
- [ ] 对话框配置结构（所有字段类型和选项）
- [ ] 组件属性定义（`.content.xml`）
- [ ] 使用的模板（`data-sly-template`）
- [ ] **国际化字符串和字典文件**
- [ ] 静态资源（图片、字体等）
- [ ] 组件继承关系
- [ ] 条件渲染逻辑
- [ ] 循环和数据映射逻辑
- [ ] **依赖冲突检测结果**
- [ ] **错误和警告日志**
- [ ] **性能相关的依赖**（懒加载、代码分割等）

---

## 12. 总结

Sling 的资源解析机制是一个多阶段的递归过程：

1. **Resource 解析**: 根据路径找到 JCR 节点
2. **Resource Type 解析**: 确定组件类型
3. **脚本查找**: 按优先级查找渲染脚本
4. **依赖收集**: 递归收集所有依赖（ClientLibs、子组件、Java 类）
5. **资源加载**: 从不同来源加载资源
6. **合并渲染**: 合并所有输出

理解这个机制有助于：
- 正确组织组件结构
- 优化性能
- 调试问题
- 遵循 AEM 最佳实践
- **组件迁移和分析**（如迁移到 React）

**对于 React 迁移，完整流程**：

1. **依赖分析**
   - 使用工具分析组件依赖（JCR 查询、HTL 解析、API 调用等）
   - 提取所有 CSS、JS、Model、子组件、国际化资源
   - 识别依赖冲突和性能问题

2. **依赖可视化**
   - 生成依赖关系图（Graphviz、Mermaid、D3.js）
   - 识别依赖层级和关系

3. **资源提取**
   - 导出 CSS/JS 文件
   - 提取国际化字典
   - 导出静态资源

4. **代码生成**
   - 生成 React 组件骨架
   - 转换样式（CSS Modules）
   - 生成 TypeScript 类型定义
   - 映射对话框到 Props

5. **验证测试**
   - 功能对比测试
   - 视觉回归测试
   - 性能和可访问性测试

6. **集成部署**
   - 与构建工具集成（Webpack、Vite）
   - CI/CD 集成
   - 增量分析和缓存优化

**关键工具和资源**：
- 依赖分析工具：`component-dependency-analyzer.js`
- 可视化工具：Graphviz、Mermaid、D3.js
- 测试工具：Puppeteer、视觉回归测试
- 构建集成：Webpack/Vite 插件

