# AEM CSS 发现与解析机制详解

## 目录
1. [概述：CSS 查找的完整流程](#概述css-查找的完整流程)
2. [第一步：组件/页面声明 CSS](#第一步组件页面声明-css)
3. [第二步：Categories 解析](#第二步categories-解析)
4. [第三步：JCR 查询查找 ClientLib](#第三步jcr-查询查找-clientlib)
5. [第四步：ClientLib 配置解析](#第四步clientlib-配置解析)
6. [第五步：CSS 文件收集](#第五步css-文件收集)
7. [第六步：依赖解析](#第六步依赖解析)
8. [第七步：文件合并与处理](#第七步文件合并与处理)
9. [第八步：代理路径生成](#第八步代理路径生成)
10. [第九步：HTML 输出](#第九步html-输出)
11. [完整流程图](#完整流程图)
12. [实际案例分析](#实际案例分析)
13. [迁移准备指南](#迁移准备指南)
14. [高级主题：性能优化与缓存机制](#高级主题性能优化与缓存机制)
15. [高级主题：错误处理与边界情况](#高级主题错误处理与边界情况)
16. [高级主题：调试与故障排查](#高级主题调试与故障排查)
17. [高级主题：安全考虑](#高级主题安全考虑)
18. [高级主题：AEM 版本差异](#高级主题aem-版本差异)
19. [高级主题：性能监控](#高级主题性能监控)
20. [高级主题：最佳实践](#高级主题最佳实践)

---

## 概述：CSS 查找的完整流程

### 核心问题

**问题**: 当组件或页面声明了 `categories='myapp.components.hero'` 时，AEM 是如何找到对应的 CSS 文件的？

**答案**: AEM 通过一个复杂的查找和解析机制，将 categories 转换为实际的 CSS 文件路径。

### 完整流程概览

```
1. 组件/页面声明 CSS
   ↓
2. 提取 categories
   ↓
3. JCR 查询查找 ClientLib
   ↓
4. 解析 ClientLib 配置
   ↓
5. 收集 CSS 文件
   ↓
6. 解析依赖关系
   ↓
7. 合并和处理文件
   ↓
8. 生成代理路径
   ↓
9. 输出 HTML <link> 标签
```

### 关键概念

- **Categories**: CSS 的标识符，用于查找和引用
- **ClientLib**: 客户端库，AEM 管理 CSS/JS 的机制
- **JCR 查询**: 在 JCR 仓库中查找资源
- **代理路径**: 通过 `/etc.clientlibs/` 访问的路径

---

## 第一步：组件/页面声明 CSS

### 1.1 在 HTL 模板中声明

**位置**: 组件模板文件（如 `hero.html`）

```html
<!-- 引入 ClientLib 模板 -->
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>

<!-- 声明 CSS categories -->
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>
```

**关键点**:
- `clientlib.css` 是 HTL 模板方法
- `categories` 参数指定要加载的 CSS 类别
- 可以有多个 categories（数组或逗号分隔）

### 1.2 在页面模板中声明

**位置**: 页面模板（如 `page.html`）

```html
<!-- 在 <head> 中声明 -->
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories=['myapp.base', 'myapp.theme']}"></sly>
```

### 1.3 在组件中声明（通过 component.html）

**位置**: 组件定义文件

```html
<!-- 组件自动加载的 CSS -->
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>
```

### 1.4 声明方式总结

| 声明位置 | 声明方式 | 作用范围 |
|---------|---------|---------|
| 组件模板 | `clientlib.css @ categories='...'` | 该组件 |
| 页面模板 | `clientlib.css @ categories='...'` | 整个页面 |
| 组件定义 | 通过 `.content.xml` 配置 | 组件实例 |

---

## 第二步：Categories 解析

### 2.1 HTL 模板处理

**处理位置**: HTL 引擎（Sightly）

**处理过程**:

```java
// HTL 引擎处理 clientlib.css 调用
public class ClientLibTemplate {
    public void css(Use use, String[] categories) {
        // 1. 解析 categories 参数
        List<String> categoryList = parseCategories(categories);
        
        // 2. 调用 ClientLib 服务查找
        ClientLibrary clientLib = clientLibService.getLibraries(categoryList, LibraryType.CSS);
        
        // 3. 生成 HTML
        return generateLinkTags(clientLib);
    }
}
```

### 2.2 Categories 参数解析

**单个 category**:
```html
categories='myapp.components.hero'
```
**解析结果**: `['myapp.components.hero']`

**多个 categories（数组）**:
```html
categories=['myapp.base', 'myapp.components.hero']
```
**解析结果**: `['myapp.base', 'myapp.components.hero']`

**多个 categories（字符串）**:
```html
categories='myapp.base,myapp.components.hero'
```
**解析结果**: `['myapp.base', 'myapp.components.hero']`

### 2.3 Categories 验证

**验证规则**:
- 不能为空
- 必须是字符串或字符串数组
- 格式：点分隔的小写字母和连字符

**有效示例**:
- ✅ `myapp.components.hero`
- ✅ `myapp.base`
- ✅ `myapp.theme.brand-a`

**无效示例**:
- ❌ `Hero` (大写)
- ❌ `myapp-hero` (连字符分隔)
- ❌ `myapp/components/hero` (斜杠分隔)

---

## 第三步：JCR 查询查找 ClientLib

### 3.1 JCR 查询机制

**查询位置**: AEM ClientLib 服务

**查询方法**: JCR SQL2 查询

```java
// ClientLib 服务查找逻辑
public class ClientLibraryService {
    public ClientLibrary findClientLib(String category) {
        // 1. 构建 JCR 查询
        String query = "SELECT * FROM [cq:ClientLibraryFolder] " +
                      "WHERE [categories] = '" + category + "'";
        
        // 2. 执行查询
        QueryResult result = queryManager.createQuery(query, Query.JCR_SQL2);
        NodeIterator nodes = result.getNodes();
        
        // 3. 返回第一个匹配的节点
        if (nodes.hasNext()) {
            Node clientLibNode = nodes.nextNode();
            return new ClientLibrary(clientLibNode);
        }
        
        return null;
    }
}
```

### 3.2 查询路径

**查询范围**: 整个 JCR 仓库

**查询顺序**:
1. `/apps/` - 应用程序目录（优先级最高）
2. `/libs/` - 库目录（AEM 核心）
3. `/etc/` - 配置目录

**查询优化**: AEM 会缓存查询结果，提高性能

### 3.3 查找路径详解

#### 路径 1: `/apps/` 目录

**标准路径**:
```
/apps/myapp/clientlibs/components/hero/
```

**查找逻辑**:
1. 将 category 转换为路径
   - `myapp.components.hero` → `myapp/components/hero`
2. 拼接完整路径
   - `/apps/myapp/clientlibs/components/hero/`
3. 检查节点是否存在
   - 检查 `.content.xml` 文件
   - 验证 `jcr:primaryType = cq:ClientLibraryFolder`
   - 验证 `categories` 属性包含目标 category

#### 路径 2: `/libs/` 目录

**标准路径**:
```
/libs/granite/ui/components/coral/foundation/clientlibs/
```

**查找逻辑**: 如果 `/apps/` 中找不到，查找 `/libs/` 目录

#### 路径 3: `/etc/` 目录

**标准路径**:
```
/etc/clientlibs/myapp/components/hero/
```

**查找逻辑**: 最后查找 `/etc/` 目录（通常用于全局配置）

### 3.4 查找优先级

**优先级顺序**:
1. `/apps/{project}/clientlibs/` - 项目特定（最高优先级）
2. `/libs/` - AEM 核心库
3. `/etc/clientlibs/` - 全局配置（最低优先级）

**原因**: `/apps/` 中的资源可以覆盖 `/libs/` 和 `/etc/` 中的资源

### 3.5 查找失败处理

**情况 1: 找不到 ClientLib**

```java
if (clientLib == null) {
    // 记录警告日志
    logger.warn("ClientLib not found for category: " + category);
    
    // 返回空结果（不生成 <link> 标签）
    return "";
}
```

**情况 2: 找到多个 ClientLib**

```java
if (nodes.getSize() > 1) {
    // 记录警告日志
    logger.warn("Multiple ClientLibs found for category: " + category);
    
    // 使用第一个匹配的
    return new ClientLibrary(nodes.nextNode());
}
```

---

## 第四步：ClientLib 配置解析

### 4.1 读取配置文件

**配置文件位置**: `/apps/myapp/clientlibs/components/hero/.content.xml`

**配置文件内容**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base]"
          allowProxy="{Boolean}true"/>
```

### 4.2 解析关键属性

#### 属性 1: `jcr:primaryType`

**值**: `cq:ClientLibraryFolder`

**验证**: 必须是 `cq:ClientLibraryFolder`，否则不是有效的 ClientLib

#### 属性 2: `categories`

**值**: `[myapp.components.hero]`

**格式**: 数组格式，用方括号包裹，多个值用逗号分隔

**解析**:
```java
String categoriesStr = node.getProperty("categories").getString();
// 解析: "[myapp.components.hero]" → ["myapp.components.hero"]
List<String> categories = parseArrayProperty(categoriesStr);
```

#### 属性 3: `dependencies`

**值**: `[myapp.base]`

**作用**: 指定依赖的其他 ClientLibs

**解析**: 与 `categories` 相同的方式解析

#### 属性 4: `allowProxy`

**值**: `true` 或 `false`

**作用**: 是否允许通过代理路径访问

**重要性**: 生产环境通常需要设置为 `true`

### 4.3 配置文件结构验证

**必需属性**:
- ✅ `jcr:primaryType` - 必须为 `cq:ClientLibraryFolder`
- ✅ `categories` - 必须包含目标 category

**可选属性**:
- `dependencies` - 依赖关系
- `allowProxy` - 代理访问
- `embed` - 嵌入其他 ClientLibs
- `cssProcessor` - CSS 处理器
- `jsProcessor` - JavaScript 处理器

---

## 第五步：CSS 文件收集

### 5.1 文件目录结构

**标准结构**:
```
/apps/myapp/clientlibs/components/hero/
├── .content.xml          # ClientLib 配置
├── css/                  # CSS 文件目录
│   ├── hero.css
│   └── hero-responsive.css
└── js/                   # JavaScript 文件目录（如果有）
    └── hero.js
```

### 5.2 CSS 文件查找逻辑

**查找方法**:
```java
public List<File> collectCssFiles(Node clientLibNode) {
    List<File> cssFiles = new ArrayList<>();
    
    // 1. 查找 css/ 子节点
    Node cssNode = clientLibNode.getNode("css");
    if (cssNode != null && cssNode.hasNodes()) {
        // 2. 遍历 css/ 目录下的所有文件
        NodeIterator files = cssNode.getNodes();
        while (files.hasNext()) {
            Node fileNode = files.nextNode();
            
            // 3. 检查是否是文件节点
            if (fileNode.getPrimaryNodeType().getName().equals("nt:file")) {
                // 4. 获取文件内容
                Node contentNode = fileNode.getNode("jcr:content");
                String content = contentNode.getProperty("jcr:data").getString();
                
                cssFiles.add(new File(fileNode.getPath(), content));
            }
        }
    }
    
    return cssFiles;
}
```

### 5.3 文件排序

**排序规则**:
1. 按文件名字母顺序排序
2. 确保加载顺序一致

**示例**:
```
css/
├── hero-base.css      # 1
├── hero-components.css # 2
└── hero-theme.css     # 3
```

**加载顺序**: `hero-base.css` → `hero-components.css` → `hero-theme.css`

### 5.4 文件内容读取

**读取方法**:
```java
public String readFileContent(Node fileNode) {
    // 1. 获取 jcr:content 节点
    Node contentNode = fileNode.getNode("jcr:content");
    
    // 2. 获取 jcr:data 属性（二进制数据）
    Binary binary = contentNode.getProperty("jcr:data").getBinary();
    
    // 3. 读取二进制数据
    InputStream stream = binary.getStream();
    return IOUtils.toString(stream, StandardCharsets.UTF_8);
}
```

---

## 第六步：依赖解析

### 6.1 依赖链分析

**依赖关系示例**:
```
myapp.components.hero
  └── 依赖: myapp.base
        └── 依赖: myapp.theme
```

**解析逻辑**:
```java
public List<ClientLibrary> resolveDependencies(ClientLibrary clientLib) {
    List<ClientLibrary> resolved = new ArrayList<>();
    Set<String> processed = new HashSet<>();
    Queue<String> queue = new LinkedList<>();
    
    // 1. 添加当前 ClientLib 的依赖
    queue.addAll(clientLib.getDependencies());
    
    // 2. 递归解析依赖
    while (!queue.isEmpty()) {
        String depCategory = queue.poll();
        
        // 3. 避免循环依赖
        if (processed.contains(depCategory)) {
            continue;
        }
        processed.add(depCategory);
        
        // 4. 查找依赖的 ClientLib
        ClientLibrary depLib = findClientLib(depCategory);
        if (depLib != null) {
            resolved.add(depLib);
            
            // 5. 添加依赖的依赖
            queue.addAll(depLib.getDependencies());
        }
    }
    
    return resolved;
}
```

### 6.2 依赖加载顺序

**规则**: 依赖的 ClientLib 必须在依赖它的 ClientLib 之前加载

**示例**:
```
请求: myapp.components.hero
依赖: myapp.base

加载顺序:
1. myapp.base (依赖)
2. myapp.components.hero (请求的)
```

**实现**:
```java
public List<ClientLibrary> getLoadOrder(List<ClientLibrary> libs) {
    // 使用拓扑排序确保依赖顺序
    return topologicalSort(libs);
}
```

### 6.3 循环依赖检测

**检测方法**:
```java
public boolean hasCircularDependency(String category, Set<String> visited) {
    if (visited.contains(category)) {
        return true; // 发现循环依赖
    }
    
    visited.add(category);
    ClientLibrary lib = findClientLib(category);
    
    if (lib != null) {
        for (String dep : lib.getDependencies()) {
            if (hasCircularDependency(dep, new HashSet<>(visited))) {
                return true;
            }
        }
    }
    
    return false;
}
```

**处理**: 如果检测到循环依赖，记录错误并中断加载

---

## 第七步：文件合并与处理

### 7.1 文件合并

**合并规则**:
1. 按依赖顺序合并
2. 每个 ClientLib 内的文件按文件名排序
3. 合并为一个字符串

**合并逻辑**:
```java
public String mergeCssFiles(List<ClientLibrary> libs) {
    StringBuilder merged = new StringBuilder();
    
    // 1. 按依赖顺序遍历
    for (ClientLibrary lib : libs) {
        // 2. 合并该 ClientLib 的所有 CSS 文件
        for (File cssFile : lib.getCssFiles()) {
            merged.append(cssFile.getContent());
            merged.append("\n");
        }
    }
    
    return merged.toString();
}
```

### 7.2 CSS 处理

**处理步骤**:
1. **压缩** (生产环境)
   ```java
   if (isProduction()) {
       css = cssMinifier.minify(css);
   }
   ```

2. **自动添加浏览器前缀** (如果配置了 PostCSS)
   ```java
   if (hasPostCssProcessor()) {
       css = postCssProcessor.process(css);
   }
   ```

3. **URL 重写** (相对路径转换为绝对路径)
   ```java
   css = rewriteUrls(css, basePath);
   ```

### 7.3 缓存处理

**缓存策略**:
- 合并后的 CSS 会被缓存
- 缓存键：categories + 文件修改时间
- 缓存失效：文件修改时自动失效

**实现**:
```java
public String getCachedCss(String cacheKey) {
    CacheEntry entry = cache.get(cacheKey);
    if (entry != null && !entry.isExpired()) {
        return entry.getContent();
    }
    return null;
}
```

---

## 第八步：代理路径生成

### 8.1 代理路径的作用

**问题**: 直接暴露 `/apps/` 路径不安全

**解决方案**: 通过 `/etc.clientlibs/` 代理路径访问

**优势**:
- ✅ 隐藏内部结构
- ✅ 支持版本控制
- ✅ 支持缓存优化

### 8.2 路径转换规则

**原始路径**:
```
/apps/myapp/clientlibs/components/hero/css/hero.css
```

**代理路径**:
```
/etc.clientlibs/myapp/clientlibs/components/hero.css
```

**转换逻辑**:
```java
public String generateProxyPath(String originalPath) {
    // 1. 移除 /apps/ 前缀
    String path = originalPath.replace("/apps/", "");
    
    // 2. 添加 /etc.clientlibs/ 前缀
    return "/etc.clientlibs/" + path;
}
```

### 8.3 代理路径验证

**验证规则**:
1. ClientLib 的 `allowProxy` 必须为 `true`
2. 路径必须在 `/apps/` 下
3. 文件必须存在

**验证逻辑**:
```java
public boolean canProxy(String path) {
    // 1. 检查路径是否在 /apps/ 下
    if (!path.startsWith("/apps/")) {
        return false;
    }
    
    // 2. 查找对应的 ClientLib
    ClientLibrary lib = findClientLibByPath(path);
    if (lib == null) {
        return false;
    }
    
    // 3. 检查 allowProxy 属性
    return lib.isAllowProxy();
}
```

### 8.4 版本控制

**版本号生成**:
```java
public String addVersion(String path) {
    // 1. 计算文件内容的 hash
    String hash = calculateHash(path);
    
    // 2. 添加到路径
    return path + "?hash=" + hash.substring(0, 8);
}
```

**最终路径**:
```
/etc.clientlibs/myapp/clientlibs/components/hero.css?hash=a1b2c3d4
```

---

## 第九步：HTML 输出

### 9.1 生成 <link> 标签

**生成逻辑**:
```java
public String generateLinkTag(String cssPath) {
    return "<link rel=\"stylesheet\" " +
           "href=\"" + cssPath + "\" " +
           "type=\"text/css\">";
}
```

**输出示例**:
```html
<link rel="stylesheet" 
      href="/etc.clientlibs/myapp/clientlibs/components/hero.css?hash=a1b2c3d4" 
      type="text/css">
```

### 9.2 多个 CSS 的处理

**情况 1: 合并输出**

如果配置了合并，所有 CSS 合并为一个文件：
```html
<link rel="stylesheet" 
      href="/etc.clientlibs/myapp/clientlibs/components/hero.css" 
      type="text/css">
```

**情况 2: 分别输出**

如果未配置合并，每个 ClientLib 单独输出：
```html
<link rel="stylesheet" 
      href="/etc.clientlibs/myapp/clientlibs/base.css" 
      type="text/css">
<link rel="stylesheet" 
      href="/etc.clientlibs/myapp/clientlibs/components/hero.css" 
      type="text/css">
```

### 9.3 输出位置

**在 <head> 中输出**:
```html
<head>
    <meta charset="UTF-8">
    <title>Page Title</title>
    
    <!-- ClientLibs CSS -->
    <link rel="stylesheet" href="...">
    <link rel="stylesheet" href="...">
</head>
```

**输出顺序**:
1. 依赖的 ClientLibs（按依赖顺序）
2. 请求的 ClientLibs（按声明顺序）

---

## 完整流程图

### 流程图

```
┌─────────────────────────────────────────────────────────────┐
│ 1. 组件/页面声明 CSS                                         │
│    <sly data-sly-call="${clientlib.css @                    │
│                    categories='myapp.components.hero'}">     │
└──────────────────────┬──────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. HTL 引擎解析                                              │
│    - 提取 categories: ['myapp.components.hero']            │
│    - 调用 ClientLib 服务                                    │
└──────────────────────┬──────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. JCR 查询查找 ClientLib                                    │
│    SELECT * FROM [cq:ClientLibraryFolder]                   │
│    WHERE [categories] = 'myapp.components.hero'            │
│    - 查找路径: /apps/myapp/clientlibs/components/hero/      │
│    - 查找路径: /libs/...                                     │
│    - 查找路径: /etc/clientlibs/...                           │
└──────────────────────┬──────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. 解析 ClientLib 配置                                       │
│    - 读取 .content.xml                                       │
│    - 解析 categories, dependencies, allowProxy              │
└──────────────────────┬──────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. 收集 CSS 文件                                             │
│    - 查找 css/ 目录                                          │
│    - 读取所有 .css 文件                                       │
│    - 按文件名排序                                            │
└──────────────────────┬──────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. 解析依赖关系                                              │
│    - 查找 dependencies: ['myapp.base']                      │
│    - 递归解析依赖的依赖                                       │
│    - 检测循环依赖                                            │
└──────────────────────┬──────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. 合并和处理文件                                            │
│    - 按依赖顺序合并                                           │
│    - 压缩（生产环境）                                         │
│    - 添加浏览器前缀                                           │
└──────────────────────┬──────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────────┐
│ 8. 生成代理路径                                              │
│    /apps/myapp/clientlibs/components/hero.css               │
│    → /etc.clientlibs/myapp/clientlibs/components/hero.css   │
│    → /etc.clientlibs/.../hero.css?hash=a1b2c3d4             │
└──────────────────────┬──────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────────┐
│ 9. 输出 HTML <link> 标签                                    │
│    <link rel="stylesheet"                                   │
│          href="/etc.clientlibs/.../hero.css?hash=..."      │
│          type="text/css">                                   │
└─────────────────────────────────────────────────────────────┘
```

### 时间线

```
时间轴:
0ms    - 组件声明 CSS
1ms    - HTL 引擎解析
2ms    - JCR 查询（如果缓存命中，0ms）
5ms    - 解析配置
6ms    - 收集 CSS 文件
10ms   - 解析依赖
15ms   - 合并和处理
20ms   - 生成代理路径
21ms   - 输出 HTML
```

---

## 实际案例分析

### 案例 1: 简单组件

**组件**: `/apps/myapp/components/hero`

**HTL 模板**:
```html
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>
```

**查找过程**:
1. 提取 category: `myapp.components.hero`
2. JCR 查询: 查找 `categories = 'myapp.components.hero'`
3. 找到: `/apps/myapp/clientlibs/components/hero/`
4. 读取配置: `categories=[myapp.components.hero]`
5. 收集文件: `css/hero.css`
6. 生成路径: `/etc.clientlibs/myapp/clientlibs/components/hero.css`
7. 输出: `<link rel="stylesheet" href="...">`

### 案例 2: 有依赖的组件

**组件**: `/apps/myapp/components/product-card`

**HTL 模板**:
```html
<sly data-sly-call="${clientlib.css @ categories='myapp.components.product-card'}"></sly>
```

**ClientLib 配置**:
```xml
categories="[myapp.components.product-card]"
dependencies="[myapp.base, myapp.components.card]"
```

**查找过程**:
1. 提取 category: `myapp.components.product-card`
2. 找到 ClientLib: `/apps/myapp/clientlibs/components/product-card/`
3. 发现依赖: `['myapp.base', 'myapp.components.card']`
4. 递归查找依赖:
   - `myapp.base` → `/apps/myapp/clientlibs/base/`
   - `myapp.components.card` → `/apps/myapp/clientlibs/components/card/`
5. 加载顺序:
   - `myapp.base` (依赖)
   - `myapp.components.card` (依赖)
   - `myapp.components.product-card` (请求的)
6. 合并所有 CSS 文件
7. 输出多个 `<link>` 标签或一个合并的标签

### 案例 3: 多个 Categories

**HTL 模板**:
```html
<sly data-sly-call="${clientlib.css @ 
    categories=['myapp.base', 'myapp.components.hero', 'myapp.theme']}"></sly>
```

**查找过程**:
1. 解析 categories: `['myapp.base', 'myapp.components.hero', 'myapp.theme']`
2. 分别查找每个 category:
   - `myapp.base` → `/apps/myapp/clientlibs/base/`
   - `myapp.components.hero` → `/apps/myapp/clientlibs/components/hero/`
   - `myapp.theme` → `/apps/myapp/clientlibs/theme/`
3. 解析每个 ClientLib 的依赖
4. 合并所有依赖和请求的 ClientLibs
5. 去重（避免重复加载）
6. 按依赖顺序排序
7. 输出合并的 CSS 或分别输出

---

## 迁移准备指南

### 1. 识别组件使用的 CSS

**方法 1: 分析 HTL 模板**

```javascript
// 从 HTL 模板中提取 categories
const htlContent = fs.readFileSync('hero.html', 'utf8');
const regex = /clientlib\.css\s*@\s*categories=['"]([^'"]+)['"]/g;
const categories = [];
let match;

while ((match = regex.exec(htlContent)) !== null) {
    categories.push(...match[1].split(',').map(c => c.trim()));
}
```

**方法 2: 分析页面模板**

检查页面模板中声明的 CSS categories。

**方法 3: 使用浏览器开发者工具**

1. 打开页面
2. 查看 Network 标签
3. 找到加载的 CSS 文件
4. 分析 URL 路径，反推 category

### 2. 查找对应的 ClientLib

**方法 1: 使用 JCR 查询**

```javascript
// 通过 AEM API 查询
const query = `
    SELECT * FROM [cq:ClientLibraryFolder] 
    WHERE [categories] = 'myapp.components.hero'
`;
const results = await aemClient.query(query);
```

**方法 2: 使用 CRX/DE Lite**

1. 访问 `http://localhost:4502/crx/de/index.jsp`
2. 搜索 `categories` 属性
3. 找到匹配的节点

**方法 3: 使用 ClientLibs Dump Tool**

访问 `http://localhost:4502/libs/granite/ui/content/dumplibs.html`

### 3. 收集 CSS 文件

**方法 1: 通过 AEM API**

```javascript
// 获取 ClientLib 路径
const clientLibPath = '/apps/myapp/clientlibs/components/hero';

// 获取 CSS 文件列表
const cssFiles = await aemClient.getJSON(`${clientLibPath}/css.json`);

// 下载每个文件
for (const fileName in cssFiles) {
    const filePath = `${clientLibPath}/css/${fileName}`;
    const content = await aemClient.getText(filePath);
    // 保存文件
    fs.writeFileSync(`./collected/css/${fileName}`, content);
}
```

**方法 2: 通过代理路径**

```javascript
// 直接访问代理路径
const proxyPath = '/etc.clientlibs/myapp/clientlibs/components/hero.css';
const cssContent = await fetch(`${aemHost}${proxyPath}`);
```

### 4. 分析依赖关系

**方法 1: 读取配置文件**

```javascript
const config = await aemClient.getJSON(`${clientLibPath}.json`);
const dependencies = config.dependencies || [];
```

**方法 2: 递归分析**

```javascript
async function analyzeDependencies(category, visited = new Set()) {
    if (visited.has(category)) {
        return []; // 避免循环依赖
    }
    visited.add(category);
    
    const clientLib = await findClientLib(category);
    const deps = clientLib.dependencies || [];
    
    const allDeps = [];
    for (const dep of deps) {
        allDeps.push(dep);
        allDeps.push(...await analyzeDependencies(dep, visited));
    }
    
    return allDeps;
}
```

### 5. 迁移到 React

**步骤 1: 提取 CSS 文件**

将所有 CSS 文件复制到 React 项目中：
```
src/
└── components/
    └── hero/
        └── Hero.module.css  (从 AEM 收集的 CSS)
```

**步骤 2: 转换 CSS**

- 将全局 CSS 转换为 CSS Modules
- 处理 URL 路径（图片、字体等）
- 处理 AEM 特定的类名

**步骤 3: 在 React 组件中引入**

```tsx
import styles from './Hero.module.css';

export function Hero() {
    return <div className={styles.hero}>...</div>;
}
```

**步骤 4: 处理依赖**

如果组件依赖其他组件的 CSS：
```tsx
// Hero 组件依赖 Base 样式
import '../base/base.css';
import styles from './Hero.module.css';
```

---

## 高级主题：性能优化与缓存机制

### 10.1 JCR 查询缓存

**缓存策略**:
```java
public class ClientLibraryCache {
    private final Cache<String, ClientLibrary> queryCache;
    
    public ClientLibrary findClientLib(String category) {
        // 1. 检查缓存
        ClientLibrary cached = queryCache.get(category);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }
        
        // 2. 执行查询
        ClientLibrary lib = performJCRQuery(category);
        
        // 3. 更新缓存
        if (lib != null) {
            queryCache.put(category, lib);
        }
        
        return lib;
    }
}
```

**缓存失效策略**:
- 文件修改时间变化时失效
- 配置更新时失效
- 手动清除缓存

### 10.2 CSS 内容缓存

**缓存键生成**:
```java
public String generateCacheKey(String category, List<String> dependencies) {
    // 1. 收集所有相关文件的修改时间
    List<Long> timestamps = new ArrayList<>();
    timestamps.add(getClientLibModTime(category));
    for (String dep : dependencies) {
        timestamps.add(getClientLibModTime(dep));
    }
    
    // 2. 生成 hash
    String combined = category + ":" + String.join(",", dependencies) + ":" + 
                     timestamps.stream().map(String::valueOf).collect(Collectors.joining(","));
    return DigestUtils.md5Hex(combined);
}
```

**缓存存储**:
```java
public class CssCache {
    // 内存缓存（快速访问）
    private final Cache<String, String> memoryCache;
    
    // 磁盘缓存（持久化）
    private final File cacheDirectory;
    
    public String getCss(String cacheKey) {
        // 1. 先查内存缓存
        String css = memoryCache.get(cacheKey);
        if (css != null) {
            return css;
        }
        
        // 2. 查磁盘缓存
        File cacheFile = new File(cacheDirectory, cacheKey + ".css");
        if (cacheFile.exists()) {
            css = FileUtils.readFileToString(cacheFile, StandardCharsets.UTF_8);
            memoryCache.put(cacheKey, css);
            return css;
        }
        
        return null;
    }
}
```

### 10.3 查询优化技巧

**优化 1: 限制查询范围**

```java
// 不推荐：全库查询
String query = "SELECT * FROM [cq:ClientLibraryFolder] WHERE [categories] = '...'";

// 推荐：限制查询路径
String query = "SELECT * FROM [cq:ClientLibraryFolder] " +
               "WHERE [categories] = '...' " +
               "AND ISDESCENDANTNODE('/apps/myapp/clientlibs')";
```

**优化 2: 使用索引**

```xml
<!-- 在 .content.xml 中定义索引 -->
<jcr:root>
    <oak:index>
        <categoriesIndex>
            <jcr:primaryType>oak:QueryIndexDefinition</jcr:primaryType>
            <propertyNames>categories</propertyNames>
            <type>property</type>
        </categoriesIndex>
    </oak:index>
</jcr:root>
```

**优化 3: 批量查询**

```java
// 一次查询多个 categories
public Map<String, ClientLibrary> findMultipleClientLibs(List<String> categories) {
    String query = "SELECT * FROM [cq:ClientLibraryFolder] " +
                   "WHERE [categories] IN ('" + 
                   String.join("','", categories) + "')";
    
    // 执行一次查询，返回多个结果
    return executeQuery(query);
}
```

---

## 高级主题：错误处理与边界情况

### 11.1 常见错误场景

#### 场景 1: Category 不存在

**错误表现**:
- CSS 文件不加载
- 浏览器控制台无错误（静默失败）

**处理方式**:
```java
public String generateCssLink(String category) {
    ClientLibrary lib = findClientLib(category);
    if (lib == null) {
        // 记录警告日志
        logger.warn("ClientLib not found for category: " + category);
        
        // 开发环境：输出注释提示
        if (isDevelopment()) {
            return "<!-- ClientLib not found: " + category + " -->";
        }
        
        // 生产环境：静默失败
        return "";
    }
    
    return generateLinkTag(lib);
}
```

#### 场景 2: CSS 文件不存在

**错误表现**:
- ClientLib 存在但 `css/` 目录为空
- 生成空的 `<link>` 标签

**处理方式**:
```java
public List<File> collectCssFiles(Node clientLibNode) {
    List<File> files = new ArrayList<>();
    Node cssNode = clientLibNode.getNode("css");
    
    if (cssNode == null || !cssNode.hasNodes()) {
        logger.warn("No CSS files found in ClientLib: " + clientLibNode.getPath());
        return files; // 返回空列表
    }
    
    // 正常收集文件...
    return files;
}
```

#### 场景 3: 循环依赖

**错误表现**:
- 无限循环加载
- 栈溢出错误

**处理方式**:
```java
public List<ClientLibrary> resolveDependencies(ClientLibrary lib) {
    List<ClientLibrary> resolved = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    Stack<String> path = new Stack<>();
    
    if (hasCircularDependency(lib.getCategory(), visited, path)) {
        logger.error("Circular dependency detected: " + path);
        throw new CircularDependencyException("Circular dependency: " + path);
    }
    
    // 正常解析依赖...
    return resolved;
}
```

#### 场景 4: 文件读取失败

**错误表现**:
- 文件权限问题
- 文件损坏

**处理方式**:
```java
public String readFileContent(Node fileNode) {
    try {
        Node contentNode = fileNode.getNode("jcr:content");
        Binary binary = contentNode.getProperty("jcr:data").getBinary();
        return IOUtils.toString(binary.getStream(), StandardCharsets.UTF_8);
    } catch (Exception e) {
        logger.error("Failed to read file: " + fileNode.getPath(), e);
        
        // 返回空内容或默认内容
        return "/* Error reading file: " + fileNode.getPath() + " */";
    }
}
```

### 11.2 边界情况处理

#### 情况 1: 多个 ClientLib 使用相同 Category

**问题**: 多个 ClientLib 的 `categories` 属性包含相同的值

**处理**:
```java
public ClientLibrary findClientLib(String category) {
    QueryResult result = queryManager.createQuery(
        "SELECT * FROM [cq:ClientLibraryFolder] WHERE [categories] = '" + category + "'",
        Query.JCR_SQL2
    );
    
    NodeIterator nodes = result.getNodes();
    
    if (nodes.getSize() == 0) {
        return null;
    } else if (nodes.getSize() == 1) {
        return new ClientLibrary(nodes.nextNode());
    } else {
        // 多个匹配：按优先级选择
        logger.warn("Multiple ClientLibs found for category: " + category);
        
        // 优先级：/apps/ > /libs/ > /etc/
        List<Node> sorted = sortByPriority(nodes);
        return new ClientLibrary(sorted.get(0));
    }
}
```

#### 情况 2: 依赖的 ClientLib 不存在

**问题**: 声明的依赖找不到

**处理**:
```java
public List<ClientLibrary> resolveDependencies(ClientLibrary lib) {
    List<ClientLibrary> resolved = new ArrayList<>();
    
    for (String depCategory : lib.getDependencies()) {
        ClientLibrary depLib = findClientLib(depCategory);
        if (depLib == null) {
            logger.warn("Dependency not found: " + depCategory + 
                       " (required by " + lib.getCategory() + ")");
            // 继续处理其他依赖，不中断流程
            continue;
        }
        
        resolved.add(depLib);
        // 递归解析依赖的依赖
        resolved.addAll(resolveDependencies(depLib));
    }
    
    return resolved;
}
```

#### 情况 3: 文件编码问题

**问题**: CSS 文件使用非 UTF-8 编码

**处理**:
```java
public String readFileContent(Node fileNode) {
    try {
        Node contentNode = fileNode.getNode("jcr:content");
        
        // 1. 检查编码属性
        String encoding = "UTF-8";
        if (contentNode.hasProperty("jcr:encoding")) {
            encoding = contentNode.getProperty("jcr:encoding").getString();
        }
        
        // 2. 按指定编码读取
        Binary binary = contentNode.getProperty("jcr:data").getBinary();
        return IOUtils.toString(binary.getStream(), encoding);
    } catch (Exception e) {
        // 回退到 UTF-8
        return IOUtils.toString(binary.getStream(), StandardCharsets.UTF_8);
    }
}
```

---

## 高级主题：调试与故障排查

### 12.1 调试工具

#### 工具 1: ClientLibs Dump Tool

**访问**: `http://localhost:4502/libs/granite/ui/content/dumplibs.html`

**功能**:
- 列出所有 ClientLibs
- 显示 categories 和 dependencies
- 显示文件列表
- 显示加载顺序

**使用场景**:
- 验证 ClientLib 配置
- 检查依赖关系
- 调试加载顺序问题

#### 工具 2: CRX/DE Lite

**访问**: `http://localhost:4502/crx/de/index.jsp`

**功能**:
- 浏览 JCR 节点结构
- 查看节点属性
- 编辑配置
- 查看文件内容

**使用场景**:
- 手动检查 ClientLib 配置
- 验证文件路径
- 调试配置问题

#### 工具 3: 浏览器开发者工具

**Network 标签**:
- 查看 CSS 文件加载
- 检查 HTTP 状态码
- 分析加载时间
- 查看响应内容

**Console 标签**:
- 查看 JavaScript 错误
- 检查 CSS 解析错误

**Elements 标签**:
- 检查应用的样式
- 查看计算后的样式
- 调试样式冲突

### 12.2 日志分析

#### 启用调试日志

**配置**:
```xml
<!-- org.apache.sling.commons.log.LogManager.config -->
<configuration>
    <appender name="CONSOLE" class="org.apache.log4j.ConsoleAppender">
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"/>
        </layout>
    </appender>
    
    <logger name="com.day.cq.widget">
        <level value="DEBUG"/>
    </logger>
    
    <root>
        <level value="INFO"/>
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

#### 关键日志消息

**查找 ClientLib**:
```
DEBUG ClientLibraryService: Searching for ClientLib with category: myapp.components.hero
DEBUG ClientLibraryService: Found ClientLib at: /apps/myapp/clientlibs/components/hero
```

**解析依赖**:
```
DEBUG ClientLibraryService: Resolving dependencies for: myapp.components.hero
DEBUG ClientLibraryService: Found dependency: myapp.base
DEBUG ClientLibraryService: Resolving dependencies for: myapp.base
```

**加载文件**:
```
DEBUG ClientLibraryService: Loading CSS file: /apps/myapp/clientlibs/components/hero/css/hero.css
DEBUG ClientLibraryService: File size: 12345 bytes
```

### 12.3 故障排查清单

#### 问题 1: CSS 文件不加载

**检查清单**:
- [ ] Category 拼写是否正确
- [ ] ClientLib 是否存在
- [ ] `.content.xml` 配置是否正确
- [ ] `categories` 属性是否包含目标值
- [ ] `allowProxy` 是否为 `true`（生产环境）
- [ ] CSS 文件是否存在
- [ ] 文件权限是否正确
- [ ] 浏览器控制台是否有错误

#### 问题 2: CSS 加载顺序错误

**检查清单**:
- [ ] `dependencies` 配置是否正确
- [ ] 依赖的 ClientLib 是否存在
- [ ] 是否有循环依赖
- [ ] 多个 categories 的声明顺序

#### 问题 3: CSS 样式不生效

**检查清单**:
- [ ] CSS 文件是否加载
- [ ] 选择器是否正确
- [ ] 是否有样式冲突
- [ ] 是否有更高优先级的样式覆盖
- [ ] 浏览器缓存是否清除

---

## 高级主题：安全考虑

### 13.1 路径遍历防护

**风险**: 恶意构造的 category 可能导致路径遍历攻击

**防护**:
```java
public boolean isValidCategory(String category) {
    // 1. 检查是否包含路径分隔符
    if (category.contains("/") || category.contains("\\")) {
        return false;
    }
    
    // 2. 检查是否包含特殊字符
    if (category.contains("..") || category.contains("%")) {
        return false;
    }
    
    // 3. 检查格式（点分隔的小写字母和连字符）
    return category.matches("^[a-z0-9.-]+$");
}
```

### 13.2 内容安全策略 (CSP)

**配置 CSP**:
```java
public String generateCssLink(ClientLibrary lib) {
    String path = lib.getProxyPath();
    
    // 添加 nonce 或 hash 以符合 CSP
    if (hasCSPEnabled()) {
        String nonce = generateNonce();
        return "<link rel=\"stylesheet\" " +
               "href=\"" + path + "\" " +
               "nonce=\"" + nonce + "\">";
    }
    
    return generateLinkTag(path);
}
```

### 13.3 访问控制

**检查权限**:
```java
public boolean canAccessClientLib(String category, Session session) {
    ClientLibrary lib = findClientLib(category);
    if (lib == null) {
        return false;
    }
    
    // 检查读取权限
    try {
        AccessControlManager acm = session.getAccessControlManager();
        Privilege[] privileges = acm.getPrivileges(lib.getPath());
        return hasReadPrivilege(privileges);
    } catch (Exception e) {
        logger.error("Failed to check access", e);
        return false;
    }
}
```

---

## 高级主题：AEM 版本差异

### 14.1 AEM 6.x vs AEM Cloud Service

#### 查询方式差异

**AEM 6.x**:
```java
// 使用 JCR SQL2
String query = "SELECT * FROM [cq:ClientLibraryFolder] WHERE [categories] = '...'";
```

**AEM Cloud Service**:
```java
// 推荐使用 Resource API
ResourceResolver resolver = request.getResourceResolver();
Resource clientLib = resolver.getResource("/apps/myapp/clientlibs/components/hero");
```

#### 缓存机制差异

**AEM 6.x**: 使用内存缓存

**AEM Cloud Service**: 使用分布式缓存（Redis）

### 14.2 新特性支持

#### Channels（AEM 6.5+）

**用途**: 为不同渠道提供不同的 CSS

**配置**:
```xml
<jcr:root>
    <channels>
        <mobile>
            <css>mobile.css</css>
        </mobile>
        <desktop>
            <css>desktop.css</css>
        </desktop>
    </channels>
</jcr:root>
```

#### Embed（AEM 6.3+）

**用途**: 将其他 ClientLib 的内容嵌入到当前 ClientLib

**配置**:
```xml
<jcr:root>
    <embed>myapp.base</embed>
</jcr:root>
```

---

## 高级主题：性能监控

### 15.1 性能指标

#### 指标 1: CSS 加载时间

```java
public class PerformanceMonitor {
    public void monitorCssLoad(String category) {
        long startTime = System.currentTimeMillis();
        
        try {
            ClientLibrary lib = findClientLib(category);
            collectCssFiles(lib);
            generateProxyPath(lib);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录指标
            metrics.record("css.load.time", duration, "category", category);
            
            if (duration > 100) { // 超过 100ms 警告
                logger.warn("Slow CSS load: " + category + " took " + duration + "ms");
            }
        } catch (Exception e) {
            metrics.record("css.load.error", 1, "category", category);
        }
    }
}
```

#### 指标 2: 缓存命中率

```java
public class CacheMetrics {
    private long hits = 0;
    private long misses = 0;
    
    public double getHitRate() {
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0;
    }
    
    public void recordHit() {
        hits++;
    }
    
    public void recordMiss() {
        misses++;
    }
}
```

### 15.2 性能优化建议

#### 建议 1: 减少 ClientLib 数量

**问题**: 每个 ClientLib 都需要一次查询

**优化**: 合并相关 CSS 到同一个 ClientLib

#### 建议 2: 减少依赖深度

**问题**: 深层依赖链增加解析时间

**优化**: 扁平化依赖结构

#### 建议 3: 启用压缩

**问题**: 未压缩的 CSS 文件较大

**优化**: 生产环境启用 CSS 压缩

```java
if (isProduction()) {
    css = cssMinifier.minify(css);
}
```

---

## 高级主题：最佳实践

### 16.1 Categories 命名规范

**推荐命名**:
```
{project}.{module}.{component}
```

**示例**:
- ✅ `myapp.components.hero`
- ✅ `myapp.base`
- ✅ `myapp.theme.brand-a`

**避免**:
- ❌ `hero` (太简单，容易冲突)
- ❌ `myapp-hero` (使用连字符)
- ❌ `Hero` (大写开头)

### 16.2 依赖管理最佳实践

#### 实践 1: 明确声明依赖

```xml
<!-- 明确声明所有直接依赖 -->
<dependencies>[myapp.base, myapp.theme]</dependencies>
```

#### 实践 2: 避免深层依赖

**不推荐**:
```
A → B → C → D → E
```

**推荐**:
```
A → B, C, D, E
```

#### 实践 3: 避免循环依赖

**检测工具**:
```java
public boolean hasCircularDependency(String category) {
    return checkCircularDependency(category, new HashSet<>());
}
```

### 16.3 文件组织最佳实践

#### 实践 1: 按功能组织

```
clientlibs/
├── base/          # 基础样式
├── components/    # 组件样式
├── theme/         # 主题样式
└── vendor/        # 第三方库
```

#### 实践 2: 文件命名规范

**推荐**:
- `{component-name}.css`
- `{component-name}-{variant}.css`

**示例**:
- `hero.css`
- `hero-responsive.css`
- `hero-dark.css`

### 16.4 性能最佳实践

#### 实践 1: 合并小文件

**问题**: 多个小文件增加 HTTP 请求

**优化**: 合并相关 CSS 文件

#### 实践 2: 按需加载

**问题**: 加载不需要的 CSS

**优化**: 只在需要的页面/组件加载

#### 实践 3: 使用 CDN

**问题**: 服务器负载高

**优化**: 将 ClientLibs 部署到 CDN

---

## 高级主题：生产环境实战场景

### 17.1 大规模项目的架构设计

#### 场景 1: 多品牌/多站点架构

**问题**: 多个品牌共享组件，但样式不同

**解决方案**:
```xml
<!-- 基础组件样式 -->
/apps/myapp/clientlibs/components/hero/
  └── css/hero.css

<!-- 品牌 A 样式 -->
/apps/myapp/clientlibs/brands/brand-a/components/hero/
  └── css/hero-override.css

<!-- 品牌 B 样式 -->
/apps/myapp/clientlibs/brands/brand-b/components/hero/
  └── css/hero-override.css
```

**加载策略**:
```html
<!-- 基础样式 -->
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>

<!-- 品牌特定样式（通过页面属性动态加载） -->
<sly data-sly-call="${clientlib.css @ categories='myapp.brands.' + pageProperties.brand + '.components.hero'}"></sly>
```

#### 场景 2: 微前端架构集成

**问题**: AEM 组件需要与微前端应用共享样式

**解决方案**:
```javascript
// 1. 将 AEM ClientLibs 导出为独立 CSS 文件
const aemCss = await fetch('/etc.clientlibs/myapp/clientlibs/base.css');

// 2. 在微前端应用中动态加载
const link = document.createElement('link');
link.rel = 'stylesheet';
link.href = '/etc.clientlibs/myapp/clientlibs/base.css';
document.head.appendChild(link);
```

#### 场景 3: 多语言/多区域样式

**问题**: 不同语言/区域需要不同的样式（如 RTL）

**解决方案**:
```xml
<!-- 基础样式 -->
/apps/myapp/clientlibs/components/hero/css/hero.css

<!-- RTL 样式 -->
/apps/myapp/clientlibs/components/hero/css/hero-rtl.css
```

**条件加载**:
```html
<sly data-sly-call="${clientlib.css @ 
    categories='myapp.components.hero' + 
    (currentPage.language == 'ar' ? '.rtl' : '')}"></sly>
```

### 17.2 性能瓶颈分析与优化

#### 瓶颈 1: JCR 查询性能

**问题**: 大量组件导致频繁的 JCR 查询

**分析**:
```java
// 性能分析
public class PerformanceAnalyzer {
    public void analyzeQueryPerformance() {
        long startTime = System.currentTimeMillis();
        
        // 执行查询
        ClientLibrary lib = findClientLib(category);
        
        long duration = System.currentTimeMillis() - startTime;
        
        if (duration > 50) { // 超过 50ms 记录
            logger.warn("Slow JCR query: " + category + " took " + duration + "ms");
            metrics.record("jcr.query.slow", duration);
        }
    }
}
```

**优化方案**:
1. **预热缓存**: 系统启动时预加载常用 ClientLibs
2. **批量查询**: 一次查询多个 categories
3. **索引优化**: 为 categories 属性创建索引

#### 瓶颈 2: CSS 文件合并性能

**问题**: 大量 CSS 文件合并耗时

**分析**:
```java
public class MergePerformanceAnalyzer {
    public void analyzeMergePerformance(List<ClientLibrary> libs) {
        long startTime = System.currentTimeMillis();
        
        // 合并文件
        String merged = mergeCssFiles(libs);
        
        long duration = System.currentTimeMillis() - startTime;
        long fileCount = libs.stream().mapToLong(l -> l.getCssFiles().size()).sum();
        
        logger.info("Merged " + fileCount + " files in " + duration + "ms");
        
        if (duration > 100) {
            logger.warn("Slow merge: " + fileCount + " files took " + duration + "ms");
        }
    }
}
```

**优化方案**:
1. **异步合并**: 后台线程合并，缓存结果
2. **增量合并**: 只合并变更的文件
3. **并行处理**: 多线程并行读取文件

#### 瓶颈 3: 网络传输性能

**问题**: CSS 文件过大，传输慢

**优化方案**:
```java
// 1. 启用 Gzip 压缩
response.setHeader("Content-Encoding", "gzip");

// 2. 设置缓存头
response.setHeader("Cache-Control", "public, max-age=31536000");
response.setHeader("ETag", generateETag(cssContent));

// 3. 使用 HTTP/2 Server Push
if (supportsHttp2()) {
    pushResource("/etc.clientlibs/myapp/clientlibs/base.css");
}
```

### 17.3 容错与降级策略

#### 策略 1: 降级到 CDN

**场景**: AEM 服务器故障时，从 CDN 加载 CSS

**实现**:
```java
public String getCssPath(String category) {
    try {
        // 尝试从 AEM 获取
        ClientLibrary lib = findClientLib(category);
        return lib.getProxyPath();
    } catch (Exception e) {
        logger.error("Failed to get CSS from AEM, falling back to CDN", e);
        
        // 降级到 CDN
        return getCdnPath(category);
    }
}
```

#### 策略 2: 本地缓存备用

**场景**: 网络故障时，使用本地缓存

**实现**:
```javascript
// 浏览器端
async function loadCssWithFallback(category) {
    try {
        // 尝试从服务器加载
        await loadCss(`/etc.clientlibs/myapp/clientlibs/${category}.css`);
    } catch (error) {
        // 降级到本地缓存（Service Worker）
        const cached = await caches.match(`/css/${category}.css`);
        if (cached) {
            return cached.text();
        }
        
        // 最后降级到内联样式
        return getInlineStyles(category);
    }
}
```

---

## 高级主题：测试策略

### 18.1 单元测试

#### 测试 1: Category 解析

```java
@Test
public void testCategoryParsing() {
    // 测试单个 category
    List<String> categories = parseCategories("myapp.components.hero");
    assertEquals(1, categories.size());
    assertEquals("myapp.components.hero", categories.get(0));
    
    // 测试多个 categories（数组）
    categories = parseCategories("['myapp.base', 'myapp.components.hero']");
    assertEquals(2, categories.size());
    
    // 测试多个 categories（字符串）
    categories = parseCategories("myapp.base,myapp.components.hero");
    assertEquals(2, categories.size());
}
```

#### 测试 2: ClientLib 查找

```java
@Test
public void testClientLibFinding() {
    // 测试找到 ClientLib
    ClientLibrary lib = findClientLib("myapp.components.hero");
    assertNotNull(lib);
    assertEquals("/apps/myapp/clientlibs/components/hero", lib.getPath());
    
    // 测试找不到 ClientLib
    lib = findClientLib("nonexistent.category");
    assertNull(lib);
}
```

#### 测试 3: 依赖解析

```java
@Test
public void testDependencyResolution() {
    ClientLibrary lib = findClientLib("myapp.components.hero");
    List<ClientLibrary> deps = resolveDependencies(lib);
    
    // 验证依赖顺序
    assertEquals("myapp.base", deps.get(0).getCategory());
    assertEquals("myapp.components.hero", deps.get(1).getCategory());
}
```

### 18.2 集成测试

#### 测试 1: 端到端 CSS 加载

```java
@Test
public void testEndToEndCssLoading() {
    // 1. 创建测试页面
    Page testPage = createTestPage();
    
    // 2. 添加组件
    Component hero = testPage.addComponent("myapp/components/hero");
    
    // 3. 渲染页面
    String html = renderPage(testPage);
    
    // 4. 验证 CSS 链接存在
    assertTrue(html.contains("myapp.components.hero"));
    assertTrue(html.contains("/etc.clientlibs/myapp/clientlibs/components/hero.css"));
}
```

#### 测试 2: 依赖顺序验证

```java
@Test
public void testDependencyOrder() {
    Page testPage = createTestPage();
    Component card = testPage.addComponent("myapp/components/card");
    
    String html = renderPage(testPage);
    
    // 验证依赖在组件之前
    int baseIndex = html.indexOf("myapp.base");
    int cardIndex = html.indexOf("myapp.components.card");
    assertTrue(baseIndex < cardIndex);
}
```

### 18.3 性能测试

#### 测试 1: 加载时间测试

```java
@Test
public void testLoadTime() {
    long startTime = System.currentTimeMillis();
    
    // 加载 CSS
    ClientLibrary lib = findClientLib("myapp.components.hero");
    collectCssFiles(lib);
    
    long duration = System.currentTimeMillis() - startTime;
    
    // 验证加载时间在可接受范围内
    assertTrue("Load time too slow: " + duration + "ms", duration < 100);
}
```

#### 测试 2: 并发加载测试

```java
@Test
public void testConcurrentLoading() {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<Future<ClientLibrary>> futures = new ArrayList<>();
    
    // 并发加载 10 个不同的 ClientLibs
    for (int i = 0; i < 10; i++) {
        final String category = "myapp.components.component" + i;
        futures.add(executor.submit(() -> findClientLib(category)));
    }
    
    // 验证所有加载都成功
    for (Future<ClientLibrary> future : futures) {
        assertNotNull(future.get());
    }
}
```

---

## 高级主题：监控与告警

### 19.1 关键指标监控

#### 指标 1: CSS 加载成功率

```java
public class CssMetrics {
    private final Counter successCount = new Counter();
    private final Counter failureCount = new Counter();
    
    public void recordSuccess(String category) {
        successCount.increment("category", category);
    }
    
    public void recordFailure(String category, Exception error) {
        failureCount.increment("category", category, "error", error.getClass().getSimpleName());
    }
    
    public double getSuccessRate() {
        long total = successCount.getCount() + failureCount.getCount();
        return total > 0 ? (double) successCount.getCount() / total : 1.0;
    }
}
```

#### 指标 2: 平均加载时间

```java
public class LoadTimeMetrics {
    private final Histogram loadTime = new Histogram();
    
    public void recordLoadTime(String category, long duration) {
        loadTime.record(duration, "category", category);
    }
    
    public double getAverageLoadTime() {
        return loadTime.getSnapshot().getMean();
    }
    
    public double getP95LoadTime() {
        return loadTime.getSnapshot().get95thPercentile();
    }
}
```

#### 指标 3: 缓存命中率

```java
public class CacheMetrics {
    private final Counter hits = new Counter();
    private final Counter misses = new Counter();
    
    public void recordHit() {
        hits.increment();
    }
    
    public void recordMiss() {
        misses.increment();
    }
    
    public double getHitRate() {
        long total = hits.getCount() + misses.getCount();
        return total > 0 ? (double) hits.getCount() / total : 0.0;
    }
}
```

### 19.2 告警规则

#### 告警 1: 加载失败率过高

```java
public class AlertManager {
    public void checkFailureRate() {
        double failureRate = cssMetrics.getFailureRate();
        
        if (failureRate > 0.05) { // 失败率超过 5%
            sendAlert("CSS loading failure rate is high: " + 
                     (failureRate * 100) + "%");
        }
    }
}
```

#### 告警 2: 加载时间过长

```java
public void checkLoadTime() {
    double p95LoadTime = loadTimeMetrics.getP95LoadTime();
    
    if (p95LoadTime > 200) { // P95 加载时间超过 200ms
        sendAlert("CSS loading is slow: P95 = " + p95LoadTime + "ms");
    }
}
```

#### 告警 3: 缓存命中率过低

```java
public void checkCacheHitRate() {
    double hitRate = cacheMetrics.getHitRate();
    
    if (hitRate < 0.8) { // 缓存命中率低于 80%
        sendAlert("CSS cache hit rate is low: " + (hitRate * 100) + "%");
    }
}
```

---

## 高级主题：常见陷阱与反模式

### 20.1 常见陷阱

#### 陷阱 1: Category 命名冲突

**问题**: 多个项目使用相同的 category 名称

**错误示例**:
```xml
<!-- 项目 A -->
<categories>[myapp.components.hero]</categories>

<!-- 项目 B -->
<categories>[myapp.components.hero]</categories>
```

**正确做法**:
```xml
<!-- 项目 A -->
<categories>[myapp-a.components.hero]</categories>

<!-- 项目 B -->
<categories>[myapp-b.components.hero]</categories>
```

#### 陷阱 2: 循环依赖

**问题**: ClientLib A 依赖 B，B 依赖 A

**错误示例**:
```xml
<!-- ClientLib A -->
<dependencies>[myapp.b]</dependencies>

<!-- ClientLib B -->
<dependencies>[myapp.a]</dependencies>
```

**正确做法**: 提取公共依赖到独立的 ClientLib

#### 陷阱 3: 过度依赖

**问题**: 每个组件都依赖所有基础库

**错误示例**:
```xml
<!-- 每个组件都这样配置 -->
<dependencies>[myapp.base, myapp.theme, myapp.vendor, myapp.utils]</dependencies>
```

**正确做法**: 只声明直接依赖

#### 陷阱 4: 文件命名不一致

**问题**: CSS 文件命名不规范，导致加载顺序不确定

**错误示例**:
```
css/
├── hero.css
├── Hero.css        # 大小写不一致
├── hero-base.css
└── hero_base.css  # 下划线和连字符混用
```

**正确做法**: 统一使用小写和连字符
```
css/
├── hero.css
├── hero-base.css
└── hero-responsive.css
```

### 20.2 反模式

#### 反模式 1: 内联样式替代 ClientLibs

**问题**: 直接在 HTL 模板中写 `<style>` 标签

**错误示例**:
```html
<style>
.hero {
    color: red;
}
</style>
```

**正确做法**: 使用 ClientLibs
```html
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>
```

#### 反模式 2: 直接链接外部 CSS

**问题**: 直接在 HTML 中链接外部 CSS 文件

**错误示例**:
```html
<link rel="stylesheet" href="/content/dam/myapp/css/hero.css">
```

**正确做法**: 通过 ClientLibs 管理

#### 反模式 3: 重复定义样式

**问题**: 在多个 ClientLibs 中定义相同的样式

**错误示例**:
```css
/* ClientLib A */
.button { color: blue; }

/* ClientLib B */
.button { color: blue; }
```

**正确做法**: 提取到公共 ClientLib

---

## 高级主题：实际生产案例深度分析

### 21.1 案例：大型电商网站

#### 场景描述

- **规模**: 1000+ 组件
- **品牌**: 5 个不同品牌
- **语言**: 10+ 种语言
- **区域**: 全球部署

#### 架构设计

```
/apps/myapp/clientlibs/
├── base/                    # 基础样式（所有品牌共享）
├── brands/
│   ├── brand-a/            # 品牌 A 特定样式
│   ├── brand-b/            # 品牌 B 特定样式
│   └── ...
├── components/             # 组件样式
│   ├── product-card/
│   ├── hero/
│   └── ...
├── themes/                # 主题样式
│   ├── light/
│   └── dark/
└── locales/              # 区域特定样式
    ├── rtl/              # RTL 语言
    └── ...
```

#### 性能优化

1. **CDN 部署**: 所有 ClientLibs 部署到 CDN
2. **缓存策略**: 长期缓存基础库，短期缓存组件样式
3. **按需加载**: 只加载当前页面需要的 CSS
4. **压缩优化**: 生产环境启用 CSS 压缩和 Gzip

#### 监控指标

- CSS 加载成功率: 99.9%
- 平均加载时间: 50ms
- 缓存命中率: 95%
- 文件大小: 平均 50KB

### 21.2 案例：内容管理系统迁移

#### 场景描述

- **目标**: 从传统 CMS 迁移到 AEM
- **挑战**: 保持样式一致性
- **规模**: 500+ 页面，200+ 组件

#### 迁移策略

1. **阶段 1: 分析现有 CSS**
   - 收集所有 CSS 文件
   - 分析依赖关系
   - 识别可重用样式

2. **阶段 2: 重构为 ClientLibs**
   - 按功能组织 CSS
   - 创建 categories 映射
   - 建立依赖关系

3. **阶段 3: 逐步迁移**
   - 先迁移基础样式
   - 再迁移组件样式
   - 最后迁移页面特定样式

4. **阶段 4: 优化和测试**
   - 性能优化
   - 兼容性测试
   - 用户验收测试

#### 经验教训

- ✅ 提前规划 categories 命名规范
- ✅ 建立依赖关系文档
- ✅ 使用自动化工具收集 CSS
- ❌ 避免一次性迁移所有样式
- ❌ 不要忽略性能影响

---

## 总结

### 核心要点

1. **声明**: 组件/页面通过 `categories` 声明需要的 CSS
2. **查找**: AEM 通过 JCR 查询查找对应的 ClientLib
3. **解析**: 解析 ClientLib 配置，获取 CSS 文件路径
4. **收集**: 收集 CSS 文件内容
5. **依赖**: 解析和处理依赖关系
6. **合并**: 合并所有 CSS 文件
7. **代理**: 生成代理路径
8. **输出**: 输出 HTML <link> 标签

### 关键路径

```
categories → JCR 查询 → ClientLib 配置 → CSS 文件 → 代理路径 → HTML 输出
```

### 专家级要点

1. **性能优化**: 缓存机制、查询优化、批量处理
2. **错误处理**: 完善的错误处理和边界情况处理
3. **调试技巧**: 多种调试工具和故障排查方法
4. **安全考虑**: 路径遍历防护、CSP、访问控制
5. **版本差异**: 不同 AEM 版本的特性差异
6. **性能监控**: 指标收集和性能优化建议
7. **最佳实践**: 命名规范、依赖管理、文件组织

### 迁移准备清单

- ✅ 识别组件使用的 CSS categories
- ✅ 查找对应的 ClientLib 路径
- ✅ 收集所有 CSS 文件
- ✅ 分析依赖关系
- ✅ 提取 CSS 内容
- ✅ 转换 CSS 格式（如果需要）
- ✅ 在 React 项目中组织 CSS 文件
- ✅ 更新组件引用
- ✅ 性能优化和监控
- ✅ 错误处理和边界情况处理

### 相关文档

- [CSS 管理概述](./01-css-management-overview.md)
- [ClientLibs 配置详解](./02-clientlibs-configuration.md)
- [CSS 查找机制](./03-css-lookup-mechanism.md)
- [构建链与预处理器](./04-build-chain-and-preprocessors.md)
- [组件资源收集器](../14-aem-resource-collector/README.md)

