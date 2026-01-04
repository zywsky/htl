# AEM 页面暴露和访问机制详解

## 概述

本文档详细分析 AEM 中页面的暴露机制、URL 解析、访问控制以及外部访问方式，从基础概念到高级应用，全面系统地进行讲解。

---

## 1. AEM 页面基础概念

### 1.1 什么是 AEM 页面

AEM 页面是内容存储和展示的基本单元，在 JCR 中存储为 `cq:Page` 类型的节点。

**页面节点结构**:

```
/content/my-site/en/home
  ├── jcr:content (nt:unstructured 或 cq:PageContent)
  │   ├── jcr:title = "Home Page"
  │   ├── sling:resourceType = "myapp/components/page"
  │   ├── jcr:created = "2024-01-01T00:00:00.000Z"
  │   └── [组件内容节点]
  │       ├── hero (nt:unstructured)
  │       ├── content (nt:unstructured)
  │       └── footer (nt:unstructured)
  └── [子页面节点]
      └── about (cq:Page)
```

**关键特性**:
- 页面以节点树的形式存储在 JCR 中
- 每个页面有一个 `jcr:content` 子节点包含页面内容
- 页面可以有子页面，形成页面层次结构
- 页面通过 URL 路径映射到 JCR 路径

### 1.2 页面的存储位置

AEM 页面主要存储在 `/content` 路径下：

```
/content
  ├── my-site/              # 网站根路径
  │   ├── en/               # 语言版本
  │   │   ├── home          # 首页
  │   │   ├── about         # 关于页面
  │   │   └── products/     # 产品目录页面
  │   │       ├── product1  # 产品详情页
  │   │       └── product2
  │   └── zh/               # 中文版本
  └── other-site/           # 其他网站
```

**不同路径的作用**:

| 路径 | 用途 | 是否可外部访问 |
|------|------|---------------|
| `/content` | 网站内容页面 | 是（通过配置） |
| `/etc` | 配置和系统资源 | 部分可访问 |
| `/apps` | 应用程序代码 | 否（开发环境除外） |
| `/libs` | AEM 核心库 | 否（部分资源可访问） |
| `/system` | 系统配置 | 否 |
| `/var` | 临时和缓存数据 | 部分可访问 |

### 1.3 页面类型

**1. 内容页面 (Content Pages)**
- 存储在 `/content` 下
- 通过模板创建
- 可以被外部用户访问（基于权限）
- 示例: `/content/my-site/en/home`

**2. 系统页面 (System Pages)**
- 存储在 `/etc` 或 `/system` 下
- 用于系统功能
- 通常不直接对外暴露
- 示例: `/etc/cloudsettings`

**3. 配置页面 (Configuration Pages)**
- 存储在 `/conf` 下
- 用于配置管理
- 不直接访问，作为配置引用
- 示例: `/conf/my-site/settings`

**4. 模板页面 (Template Pages)**
- 存储在 `/conf` 下
- Editable Templates 的模板定义
- 不直接访问，用于创建内容页面
- 示例: `/conf/my-site/settings/wcm/templates/content-page`

---

## 2. URL 解析和路由机制

### 2.1 URL 到 JCR 路径的映射

AEM 使用 Sling 框架将 HTTP 请求的 URL 映射到 JCR 节点路径。

**基本映射规则**:

```
浏览器请求: http://www.example.com/content/my-site/en/home.html
           ↓
Sling 解析: /content/my-site/en/home.html
           ↓
JCR 路径:   /content/my-site/en/home
           ↓
资源类型:   cq:Page
           ↓
渲染脚本:   /apps/myapp/components/page/page.html
```

**URL 组成部分**:

```
/content/my-site/en/home.html
│        │       │  │    │
│        │       │  │    └─ 扩展名 (extension)
│        │       │  └───── 页面名称 (page name)
│        │       └──────── 语言/区域 (locale)
│        └─────────────── 站点标识 (site identifier)
└─────────────────────── 内容根路径 (content root)
```

### 2.2 Sling URL 解析机制

**URL 解析步骤**:

1. **路径提取**
   ```java
   // 从 HTTP 请求中提取路径
   String requestPath = request.getPathInfo();
   // 示例: "/content/my-site/en/home.html"
   ```

2. **扩展名处理**
   ```java
   // 移除扩展名，获取基础路径
   String basePath = removeExtension(requestPath);
   // "/content/my-site/en/home.html" → "/content/my-site/en/home"
   ```

3. **选择器处理**
   ```java
   // 处理选择器（如 .print, .json）
   // "/content/my-site/en/home.print.html" → path: "/content/my-site/en/home", selector: "print"
   ```

4. **资源解析**
   ```java
   ResourceResolver resolver = request.getResourceResolver();
   Resource resource = resolver.resolve(basePath);
   // 返回: Resource 对象，对应 JCR 节点 /content/my-site/en/home
   ```

5. **脚本解析**
   ```java
   // 根据资源的 sling:resourceType 查找渲染脚本
   String resourceType = resource.getResourceType();
   Script script = scriptResolver.resolveScript(resource, "GET");
   // 返回: /apps/myapp/components/page/page.html
   ```

### 2.3 URL 选择器 (Selectors)

选择器用于请求同一资源的不同表示形式：

**常见选择器**:

| URL | 选择器 | 说明 |
|-----|--------|------|
| `/content/page.html` | (无) | 标准 HTML 视图 |
| `/content/page.print.html` | `print` | 打印版本 |
| `/content/page.json` | (无) | JSON 数据（通过扩展名） |
| `/content/page.model.json` | `model` | Sling Model JSON 导出 |
| `/content/page.infinity.json` | `infinity` | AEM 编辑器数据 |

**选择器实现**:

```java
// 在组件脚本中处理选择器
if ("print".equals(request.getRequestPathInfo().getSelectorString())) {
    // 返回打印版本
    return "print-view.html";
} else {
    // 返回标准视图
    return "standard-view.html";
}
```

### 2.4 URL 扩展名 (Extensions)

扩展名决定响应的内容类型：

| 扩展名 | 内容类型 | 说明 |
|--------|----------|------|
| `.html` | text/html | 标准 HTML 页面 |
| `.json` | application/json | JSON 数据 |
| `.xml` | application/xml | XML 数据 |
| `.txt` | text/plain | 纯文本 |

**扩展名处理**:

```java
// Sling 根据扩展名选择渲染脚本
String extension = request.getRequestPathInfo().getExtension();
// "html" → 查找 page.html
// "json" → 查找 page.json
```

---

## 3. 页面的访问控制

### 3.1 哪些页面可以被外部访问？

**可访问的页面**:

1. **内容页面** (`/content/*`)
   - 存储在 `/content` 路径下的页面
   - 默认可以通过 URL 访问
   - 受权限控制（ACL）

2. **公共资源**
   - `/etc.clientlibs/*` - ClientLibs（CSS/JS）
   - `/content/dam/*` - DAM 资产
   - 其他配置为可访问的资源

**不可访问的页面**:

1. **应用程序代码** (`/apps/*`)
   - 组件定义和脚本
   - 默认不对外暴露
   - 开发环境可能可访问（用于调试）

2. **系统库** (`/libs/*`)
   - AEM 核心库
   - 不直接访问
   - 部分资源通过代理访问（如 `/etc.clientlibs`）

3. **系统配置** (`/system/*`, `/etc/system/*`)
   - 系统内部配置
   - 不对外暴露

4. **临时数据** (`/var/*`)
   - 临时文件和缓存
   - 部分可访问，部分不可访问

### 3.2 访问控制列表 (ACL)

AEM 使用 JCR ACL 控制页面访问：

**权限类型**:

- `jcr:read` - 读取权限
- `jcr:write` - 写入权限
- `jcr:modifyAccessControl` - 修改 ACL 权限
- `rep:remove` - 删除权限

**权限检查流程**:

```
HTTP 请求
  ↓
Authentication (身份验证)
  ↓
Authorization (权限检查)
  ├── 检查用户/组的 ACL
  ├── 检查页面节点的权限
  └── 检查父节点的继承权限
  ↓
允许访问 → 继续处理
拒绝访问 → 返回 403 Forbidden
```

**权限配置示例**:

```java
// 设置页面访问权限
AccessControlManager acm = session.getAccessControlManager();
Principal principal = new PrincipalImpl("everyone");

// 允许所有人读取
Privilege[] readPrivileges = new Privilege[]{
    acm.privilegeFromName(Privilege.JCR_READ)
};
AccessControlList acl = acm.getAccessControlList("/content/my-site/en/home");
acl.addAccessControlEntry(principal, readPrivileges);
acm.setPolicy("/content/my-site/en/home", acl);
session.save();
```

### 3.3 匿名访问配置

**默认匿名访问**:

- AEM Publisher 实例默认允许匿名用户访问 `/content` 下的页面
- 需要配置适当的 ACL 来限制访问

**配置匿名访问权限**:

1. **通过 UI 配置**:
   - 工具 → 安全性 → 权限
   - 选择页面节点
   - 设置匿名用户的权限

2. **通过代码配置**:

```java
// 在安装脚本中配置匿名访问
@Activate
protected void activate(ConfigurationContext ctx) {
    Session session = ctx.getSession();
    AccessControlManager acm = session.getAccessControlManager();
    
    // 允许匿名用户读取 /content
    Principal anonymous = new PrincipalImpl("anonymous");
    Privilege[] readPrivileges = new Privilege[]{
        acm.privilegeFromName(Privilege.JCR_READ)
    };
    
    AccessControlList acl = acm.getAccessControlList("/content");
    acl.addAccessControlEntry(anonymous, readPrivileges);
    acm.setPolicy("/content", acl);
    session.save();
}
```

### 3.4 页面可见性控制

**页面属性控制可见性**:

1. **隐藏页面** (`hideInNav`)
   - 在导航中隐藏，但仍可通过直接 URL 访问
   - 属性: `hideInNav = true`

2. **禁用页面** (`disabled`)
   - 页面被禁用，返回 404
   - 属性: `disabled = true`

3. **重定向页面**
   - 页面自动重定向到其他页面
   - 属性: `sling:redirect` 或 `sling:target`

---

## 4. Sling Mappings (URL 重写)

### 4.1 什么是 Sling Mappings

Sling Mappings 允许重写 URL，将外部 URL 映射到内部 JCR 路径。

**映射类型**:

1. **内部重定向** (Internal Redirect)
   - 将外部 URL 映射到内部路径
   - 用户看到的 URL 保持不变

2. **外部重定向** (External Redirect)
   - 将请求重定向到外部 URL
   - HTTP 302/301 重定向

3. **虚拟主机映射** (Virtual Host Mapping)
   - 基于主机名的路径映射

### 4.2 映射配置位置

**映射配置文件路径**:

```
/etc/map/
  ├── http/                  # HTTP 映射
  │   └── content-dam/       # 特定映射
  ├── https/                 # HTTPS 映射
  │   └── content-dam/
  └── publish/               # Publisher 特定映射
      └── http/
          └── content-dam/
```

**映射节点结构**:

```
/etc/map/http/content-dam
  └── .content.xml
```

### 4.3 映射配置示例

**示例 1: 隐藏 /content 路径**

```xml
<!-- /etc/map/http/.content.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="sling:Mapping"
          sling:match="www.example.com/"
          sling:internalRedirect="/content/my-site/en"/>
```

**效果**:
- 外部 URL: `http://www.example.com/` → 内部路径: `/content/my-site/en`
- 外部 URL: `http://www.example.com/about` → 内部路径: `/content/my-site/en/about`

**示例 2: 多语言映射**

```xml
<!-- /etc/map/http/en/.content.xml -->
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="sling:Mapping"
          sling:match="www.example.com/en"
          sling:internalRedirect="/content/my-site/en"/>

<!-- /etc/map/http/zh/.content.xml -->
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="sling:Mapping"
          sling:match="www.example.com/zh"
          sling:internalRedirect="/content/my-site/zh"/>
```

**示例 3: 外部重定向**

```xml
<!-- /etc/map/http/old-site/.content.xml -->
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="sling:Mapping"
          sling:match="old.example.com"
          sling:redirect="https://www.example.com"
          sling:status="301"/>
```

### 4.4 映射优先级

映射按照以下顺序匹配：

1. **精确匹配** (Exact Match)
   - 完全匹配的映射优先

2. **前缀匹配** (Prefix Match)
   - 匹配路径前缀

3. **默认映射**
   - 如果没有匹配，使用默认行为

---

## 5. Dispatcher 缓存和访问控制

### 5.1 Dispatcher 的作用

Dispatcher 是 AEM 架构中的反向代理和缓存层：

**功能**:
- 缓存静态和动态内容
- 负载均衡
- SSL 终止
- 安全过滤

**架构位置**:

```
Internet
  ↓
Dispatcher (Apache HTTP Server)
  ↓
AEM Publisher
```

### 5.2 Dispatcher 过滤规则

**过滤器配置** (`dispatcher.any`):

```apache
/filter
{
    /0001 { /type "deny" /url "*" }
    /0002 { /type "allow" /url "/content/*" }
    /0003 { /type "allow" /url "/etc.clientlibs/*" }
    /0004 { /type "deny" /url "/apps/*" }
    /0005 { /type "deny" /url "/libs/*" }
    /0006 { /type "allow" /url "/system/console/*" }
}
```

**规则说明**:
- 规则按顺序执行
- `deny` 规则拒绝访问
- `allow` 规则允许访问
- 最后一个匹配的规则生效

### 5.3 Dispatcher 缓存规则

**缓存配置**:

```apache
/cache
{
    /rules
    {
        /0000 { /type "deny" /glob "*" }
        /0001 { /type "allow" /glob "/content/*.html" }
        /0002 { /type "allow" /glob "/content/dam/*" }
        /0003 { /type "deny" /glob "/content/*.json" }
    }
    
    /invalidate
    {
        /0000 { /glob "*" /type "allow" }
    }
}
```

**缓存行为**:
- 允许缓存的路径会被缓存
- 缓存命中时，直接返回缓存内容（不访问 AEM）
- 缓存未命中时，转发请求到 AEM Publisher

---

## 6. 页面的生命周期

### 6.1 页面创建

**通过模板创建页面**:

1. 内容作者在 Sites 控制台创建页面
2. 选择模板（Editable Template）
3. AEM 在 JCR 中创建页面节点
4. 复制模板的初始内容结构
5. 页面可以编辑和发布

**代码创建页面**:

```java
// 通过 PageManager 创建页面
PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
Page parentPage = pageManager.getPage("/content/my-site/en");

Page newPage = pageManager.create(
    parentPage.getPath(),
    "new-page",
    "/conf/my-site/settings/wcm/templates/content-page",
    "New Page Title"
);
```

### 6.2 页面激活/发布

**发布流程**:

1. **激活页面**:
   - 将页面从 Author 复制到 Publisher
   - 通过复制代理 (Replication Agent) 执行

2. **发布状态**:
   - `cq:lastReplicated` - 最后复制时间
   - `cq:lastReplicatedBy` - 复制执行者
   - `cq:lastReplicationAction` - 复制操作（ACTIVATE/DEACTIVATE）

**代码激活页面**:

```java
Replicator replicator = resourceResolver.adaptTo(Replicator.class);
Session session = resourceResolver.adaptTo(Session.class);

// 激活页面
replicator.replicate(
    session,
    ReplicationActionType.ACTIVATE,
    "/content/my-site/en/home"
);
```

### 6.3 页面删除

**删除流程**:

1. 停用页面（从 Publisher 删除）
2. 从 Author 删除页面
3. 清理相关资源

**代码删除页面**:

```java
PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
Page page = pageManager.getPage("/content/my-site/en/old-page");

if (page != null) {
    // 先停用
    replicator.replicate(session, ReplicationActionType.DEACTIVATE, page.getPath());
    
    // 再删除
    pageManager.delete(page, false);  // false = 不强制删除（如果有子页面会失败）
}
```

---

## 7. 页面访问的最佳实践

### 7.1 URL 设计原则

**好的 URL 设计**:
- ✅ `/content/my-site/en/products/electronics/laptop`
- ✅ `/content/my-site/en/about/team`
- ✅ 清晰、层次化的路径结构

**不好的 URL 设计**:
- ❌ `/content/my-site/en/page-12345`
- ❌ `/content/my-site/en/p?id=123`
- ❌ 使用数字 ID 或查询参数

### 7.2 安全性考虑

**安全最佳实践**:

1. **最小权限原则**
   - 只授予必要的权限
   - 定期审查权限配置

2. **敏感页面保护**
   - 使用 ACL 限制访问
   - 考虑使用身份验证

3. **输入验证**
   - 验证 URL 参数
   - 防止路径遍历攻击

4. **HTTPS**
   - 生产环境使用 HTTPS
   - 配置 SSL 证书

### 7.3 性能优化

**性能最佳实践**:

1. **Dispatcher 缓存**
   - 配置适当的缓存规则
   - 使用缓存头控制缓存行为

2. **页面结构优化**
   - 避免过深的页面层次
   - 优化组件数量

3. **懒加载**
   - 使用懒加载组件
   - 延迟加载非关键内容

---

## 8. 故障排查

### 8.1 常见问题

**问题 1: 页面返回 404**

**可能原因**:
- 页面不存在
- 页面路径错误
- 页面未激活（仅在 Author 存在）
- Dispatcher 过滤规则阻止访问

**调试方法**:
1. 检查页面是否存在: CRX/DE Lite 查看 `/content/my-site/en/page`
2. 检查激活状态: 查看 `cq:lastReplicated` 属性
3. 检查 Dispatcher 过滤规则
4. 查看 AEM 错误日志

**问题 2: 页面返回 403 Forbidden**

**可能原因**:
- 用户没有读取权限
- ACL 配置错误
- 匿名用户权限不足

**调试方法**:
1. 检查 ACL 配置
2. 检查用户/组权限
3. 测试不同用户的访问
4. 查看权限相关日志

**问题 3: URL 映射不工作**

**可能原因**:
- Sling Mapping 配置错误
- 映射路径不匹配
- 映射优先级问题

**调试方法**:
1. 检查映射配置文件
2. 验证映射规则语法
3. 测试映射匹配
4. 查看 Sling 日志

### 8.2 调试工具

**1. CRX/DE Lite**
- 查看 JCR 节点结构
- 检查页面属性
- 验证路径存在

**2. AEM 错误日志**
- 路径: `crx-quickstart/logs/error.log`
- 查看错误和警告信息

**3. Dispatcher 日志**
- 查看访问日志
- 分析缓存行为
- 检查过滤规则

**4. 浏览器开发者工具**
- Network 标签查看请求/响应
- 检查 HTTP 状态码
- 分析重定向

---

## 9. 总结

### 9.1 关键要点

1. **页面存储**: AEM 页面存储在 `/content` 路径下，以 JCR 节点形式存在

2. **URL 解析**: Sling 框架将 HTTP URL 映射到 JCR 路径，通过 ResourceResolver 解析

3. **访问控制**: 通过 ACL 控制页面访问，Dispatcher 提供额外的过滤层

4. **页面类型**: 不是所有页面都可以外部访问，只有 `/content` 下的内容页面通常可访问

5. **URL 映射**: Sling Mappings 允许重写 URL，实现友好的 URL 结构

### 9.2 最佳实践检查清单

- ✅ 使用清晰的 URL 结构
- ✅ 配置适当的 ACL
- ✅ 使用 Dispatcher 缓存
- ✅ 配置 Sling Mappings（如需要）
- ✅ 定期审查权限配置
- ✅ 监控页面访问日志
- ✅ 优化页面性能

---

## 10. 高级主题

### 10.1 虚拟主机和域名映射

**虚拟主机配置**:

AEM 支持基于主机名的路径映射：

```xml
<!-- /etc/map/http/www.example.com/.content.xml -->
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="sling:Mapping"
          sling:match="www.example.com"
          sling:internalRedirect="/content/example-site/en"/>

<!-- /etc/map/http/m.example.com/.content.xml -->
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          jcr:primaryType="sling:Mapping"
          sling:match="m.example.com"
          sling:internalRedirect="/content/example-site/en/mobile"/>
```

**Dispatcher 虚拟主机配置**:

```apache
/virtualhosts
{
    "www.example.com"
    "example.com"
    "m.example.com"
}
```

### 10.2 多站点管理 (MSM)

**MSM 概述**:

MSM (Multi-Site Manager) 允许管理多个相关站点：

- **Live Copy**: 从源页面创建的页面副本
- **Blueprint**: 源页面模板
- **Rollout**: 将更改从 Blueprint 推送到 Live Copy

**MSM 对页面访问的影响**:

- Live Copy 页面可以独立访问
- 页面路径基于 Live Copy 配置
- 激活行为受 MSM 配置影响

### 10.3 国际化 (i18n) 和本地化

**语言结构**:

```
/content/my-site/
  ├── en/          # 英语
  │   ├── home
  │   └── about
  ├── zh/          # 中文
  │   ├── home
  │   └── about
  └── fr/          # 法语
      ├── home
      └── about
```

**语言解析**:

AEM 使用多种方式确定语言：

1. **URL 路径**: `/content/my-site/en/home`
2. **Cookie**: 存储用户语言偏好
3. **Accept-Language 头**: 浏览器语言设置
4. **域名**: `en.example.com`, `zh.example.com`

**语言切换**:

```java
// 获取当前语言
Page currentPage = ...;
String language = currentPage.getLanguage(false).getLanguage();

// 获取其他语言版本的页面
PageManager pageManager = currentPage.getPageManager();
Page enPage = pageManager.getPage("/content/my-site/en/home");
Page zhPage = pageManager.getPage("/content/my-site/zh/home");
```

### 10.4 页面版本和回滚

**版本管理**:

AEM 支持页面版本控制：

```java
// 创建版本
VersionManager versionManager = resourceResolver.adaptTo(Session.class)
    .getWorkspace().getVersionManager();
    
Node pageNode = session.getNode("/content/my-site/en/home/jcr:content");
Version version = versionManager.checkin(pageNode.getPath());

// 回滚到版本
versionManager.restore(version, true);
```

**版本访问**:

- 当前版本: `/content/my-site/en/home.html`
- 历史版本: `/content/my-site/en/home.html;v=1.0`
- 版本信息: 存储在 `jcr:versionHistory`

### 10.5 页面模板和 Editable Templates

**模板类型**:

1. **Static Templates** (旧版)
   - 存储在 `/apps/myapp/templates`
   - 代码定义模板结构
   - 较少灵活性

2. **Editable Templates** (推荐)
   - 存储在 `/conf/my-site/settings/wcm/templates`
   - 内容作者可编辑
   - 支持策略 (Policies)

**模板对页面访问的影响**:

- 模板定义页面结构
- 模板影响页面渲染
- 模板策略控制组件可用性

### 10.6 页面工作流

**工作流集成**:

页面可以触发工作流：

```java
// 启动工作流
WorkflowSession workflowSession = workflowService.getWorkflowSession(session);
WorkflowModel model = workflowSession.getModel("/etc/workflow/models/page-approval");
WorkflowData data = workflowSession.newWorkflowData("JCR_PATH", "/content/my-site/en/home");
Workflow workflow = workflowSession.startWorkflow(model, data);
```

**工作流状态**:

- `RUNNING`: 工作流运行中
- `COMPLETE`: 工作流完成
- `ABORTED`: 工作流中止

### 10.7 页面复制和同步

**复制机制**:

AEM 使用复制代理同步内容：

**复制代理类型**:

1. **Publish Agent**: Author → Publisher
2. **Reverse Replication Agent**: Publisher → Author (用户生成内容)
3. **Dispatcher Flush Agent**: 清除 Dispatcher 缓存

**复制配置**:

```
/etc/replication/agents.author
  └── publish
      └── .content.xml
```

**复制触发**:

- 手动激活页面
- 工作流完成
- 计划复制
- API 调用

### 10.8 页面元数据和属性

**标准页面属性**:

| 属性 | 说明 | 示例 |
|------|------|------|
| `jcr:title` | 页面标题 | "Home Page" |
| `jcr:description` | 页面描述 | "Welcome to our site" |
| `sling:resourceType` | 页面模板 | "myapp/components/page" |
| `cq:template` | 使用的模板路径 | "/conf/my-site/.../content-page" |
| `cq:lastModified` | 最后修改时间 | 2024-01-01T00:00:00.000Z |
| `cq:lastModifiedBy` | 最后修改者 | "admin" |
| `cq:tags` | 页面标签 | ["marketing", "homepage"] |

**SEO 属性**:

| 属性 | 说明 |
|------|------|
| `jcr:title` | 页面标题 (用于 `<title>` 标签) |
| `jcr:description` | 页面描述 (用于 `<meta name="description">`) |
| `cq:tags` | 关键词标签 |
| `sling:vanityPath` | 自定义 URL 路径 |

**访问页面属性**:

```java
// 通过 Page API
Page page = pageManager.getPage("/content/my-site/en/home");
PageProperties props = page.getProperties();
String title = props.get("jcr:title", String.class);

// 通过 Resource API
Resource pageResource = resourceResolver.resolve("/content/my-site/en/home/jcr:content");
ValueMap props = pageResource.getValueMap();
String title = props.get("jcr:title", String.class);
```

### 10.9 页面搜索和查询

**使用 Query Builder 搜索页面**:

```java
// 搜索包含特定标签的页面
Map<String, String> predicates = new HashMap<>();
predicates.put("type", "cq:Page");
predicates.put("path", "/content/my-site");
predicates.put("property", "jcr:content/cq:tags");
predicates.put("property.value", "marketing");

QueryBuilder queryBuilder = resourceResolver.adaptTo(QueryBuilder.class);
Session session = resourceResolver.adaptTo(Session.class);

Query query = queryBuilder.createQuery(PredicateGroup.create(predicates), session);
SearchResult result = query.getResult();

for (Hit hit : result.getHits()) {
    String path = hit.getPath();
    Page page = pageManager.getPage(path);
    // 处理页面
}
```

**使用 JCR 查询**:

```java
// SQL2 查询
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content/my-site') " +
               "AND [jcr:content/cq:tags] = 'marketing'";

QueryManager queryManager = session.getWorkspace().getQueryManager();
javax.jcr.query.Query jcrQuery = queryManager.createQuery(query, "JCR-SQL2");
QueryResult result = jcrQuery.execute();
NodeIterator nodes = result.getNodes();
```

### 10.10 页面性能和缓存

**页面缓存策略**:

1. **Dispatcher 缓存**
   - 缓存完整 HTML 页面
   - 基于 URL 和查询参数
   - 可配置 TTL

2. **浏览器缓存**
   - 通过 HTTP 缓存头控制
   - `Cache-Control: max-age=3600`
   - `ETag` 和 `Last-Modified` 支持条件请求

3. **组件级缓存**
   - 使用 `@Cache` 注解缓存组件输出
   - 基于资源路径和选择器

**缓存头配置**:

```java
// 在组件中设置缓存头
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "myapp/components/page",
    selectors = "html",
    extensions = "html"
)
public class CacheHeaderServlet extends SlingSafeMethodsServlet {
    
    @Override
    protected void doGet(SlingHttpServletRequest request, 
                         SlingHttpServletResponse response) {
        // 设置缓存头
        response.setHeader("Cache-Control", "public, max-age=3600");
        response.setHeader("ETag", generateETag(request.getResource()));
    }
}
```

### 10.11 页面安全和权限深度分析

**权限检查流程详解**:

```
HTTP 请求
  ↓
提取用户身份 (Authentication)
  ↓
获取资源路径
  ↓
检查 ACL (从资源节点向上遍历)
  ├── 检查当前节点的 ACL
  ├── 检查父节点的 ACL (继承)
  ├── 检查组权限
  └── 检查用户权限
  ↓
应用权限规则
  ├── 允许 → 继续处理
  └── 拒绝 → 返回 403
```

**权限继承**:

- 子节点继承父节点的权限（除非被覆盖）
- ACL 从根节点向下应用
- 更具体的 ACL 覆盖通用 ACL

**权限调试**:

```java
// 检查用户权限
AccessControlManager acm = session.getAccessControlManager();
Principal principal = new PrincipalImpl("john.doe");

Privilege[] privileges = acm.getPrivileges("/content/my-site/en/home");
for (Privilege privilege : privileges) {
    System.out.println("User has privilege: " + privilege.getName());
}
```

### 10.12 页面 API 和编程访问

**PageManager API**:

```java
// 获取 PageManager
PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

// 获取页面
Page page = pageManager.getPage("/content/my-site/en/home");

// 检查页面存在
boolean exists = pageManager.pageExists("/content/my-site/en/home");

// 获取父页面
Page parentPage = page.getParent();

// 获取子页面
Iterator<Page> children = page.listChildren();

// 获取页面属性
PageProperties props = page.getProperties();
String title = props.get("jcr:title", String.class);
```

**ResourceResolver API**:

```java
// 解析页面资源
Resource pageResource = resourceResolver.resolve("/content/my-site/en/home");
Resource contentResource = pageResource.getChild("jcr:content");

// 获取资源属性
ValueMap props = contentResource.getValueMap();
String resourceType = props.get("sling:resourceType", String.class);

// 遍历子资源
Iterator<Resource> children = contentResource.listChildren();
```

## 11. 实际应用场景

### 11.1 场景 1: 多语言网站

**需求**: 支持多语言的网站，每个语言有独立的 URL 结构

**实现**:

1. **页面结构**:
```
/content/my-site/
  ├── en/
  │   └── home
  ├── zh/
  │   └── home
  └── fr/
      └── home
```

2. **URL 映射**:
```xml
<!-- /etc/map/http/en/.content.xml -->
<sling:match>www.example.com/en</sling:match>
<sling:internalRedirect>/content/my-site/en</sling:internalRedirect>

<!-- /etc/map/http/zh/.content.xml -->
<sling:match>www.example.com/zh</sling:match>
<sling:internalRedirect>/content/my-site/zh</sling:internalRedirect>
```

3. **语言切换组件**:
```java
@Model(adaptables = SlingHttpServletRequest.class)
public interface LanguageSwitcher {
    
    default List<LanguageLink> getLanguageLinks() {
        Page currentPage = ...;
        List<LanguageLink> links = new ArrayList<>();
        
        // 获取所有语言版本
        for (String lang : Arrays.asList("en", "zh", "fr")) {
            Page langPage = pageManager.getPage("/content/my-site/" + lang + currentPage.getName());
            if (langPage != null) {
                links.add(new LanguageLink(lang, langPage.getPath() + ".html"));
            }
        }
        
        return links;
    }
}
```

### 11.2 场景 2: 移动站点

**需求**: 为移动设备提供独立的站点

**实现**:

1. **页面结构**:
```
/content/my-site/
  ├── en/
  │   └── home
  └── en/mobile/
      └── home
```

2. **设备检测和重定向**:
```java
@Component(service = Filter.class)
@SlingServletFilter(scope = {REQUEST})
public class MobileRedirectFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        String userAgent = slingRequest.getHeader("User-Agent");
        
        if (isMobileDevice(userAgent)) {
            String path = slingRequest.getPathInfo();
            if (path.startsWith("/content/my-site/en/") && !path.contains("/mobile/")) {
                String mobilePath = path.replace("/content/my-site/en/", 
                                                 "/content/my-site/en/mobile/");
                ((SlingHttpServletResponse) response).sendRedirect(mobilePath);
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
```

### 11.3 场景 3: 私有内容区域

**需求**: 创建需要登录才能访问的私有内容区域

**实现**:

1. **页面结构**:
```
/content/my-site/
  ├── en/
  │   ├── public/
  │   │   └── home
  │   └── private/
  │       └── dashboard
```

2. **权限配置**:
```java
// 配置私有区域的 ACL
AccessControlManager acm = session.getAccessControlManager();
Principal authenticatedUsers = new PrincipalImpl("authenticated-users");

AccessControlList acl = acm.getAccessControlList("/content/my-site/en/private");
acl.addAccessControlEntry(authenticatedUsers, 
    new Privilege[]{acm.privilegeFromName(Privilege.JCR_READ)});
acm.setPolicy("/content/my-site/en/private", acl);
session.save();
```

3. **登录重定向**:
```java
@Component(service = Filter.class)
public class AuthenticationFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        String path = slingRequest.getPathInfo();
        
        if (path.startsWith("/content/my-site/en/private")) {
            ResourceResolver resolver = slingRequest.getResourceResolver();
            if (resolver.getUserID() == null || "anonymous".equals(resolver.getUserID())) {
                // 重定向到登录页面
                ((SlingHttpServletResponse) response).sendRedirect("/content/my-site/en/login.html");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
```

## 12. 相关文档

- AEM 官方文档: https://experienceleague.adobe.com/docs/experience-manager.html
- Sling URL 解析: https://sling.apache.org/documentation/the-sling-engine/url-decomposition.html
- Dispatcher 文档: https://experienceleague.adobe.com/docs/experience-manager-dispatcher.html
- AEM 安全: https://experienceleague.adobe.com/docs/experience-manager-65/administering/security/security.html
- AEM 页面 API: https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/javadoc/com/day/cq/wcm/api/PageManager.html
- MSM 文档: https://experienceleague.adobe.com/docs/experience-manager-65/administering/introduction/msm.html
- 国际化: https://experienceleague.adobe.com/docs/experience-manager-65/administering/introduction/tc-manage.html

