# AEM CSS 查找机制详解

## 目录
1. [查找机制概述](#查找机制概述)
2. [查找流程详解](#查找流程详解)
3. [查找路径和优先级](#查找路径和优先级)
4. [依赖解析机制](#依赖解析机制)
5. [调试和排查方法](#调试和排查方法)
6. [常见问题和解决方案](#常见问题和解决方案)

---

## 查找机制概述

### 核心问题

**问题**: AEM 如何知道组件要用哪个 CSS？

**答案**: 通过 **categories（类别）** 机制。

### 工作原理

```
组件模板声明 categories
  ↓
AEM 在 JCR 中查找匹配的 ClientLib
  ↓
找到对应的 .content.xml 配置文件
  ↓
读取配置，获取 CSS 文件路径
  ↓
加载 CSS 文件
  ↓
生成 HTML <link> 标签
```

---

## 查找流程详解

### 步骤 1: 组件模板声明

**HTL 模板代码：**
```html
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>
```

**关键点：**
- `categories='myapp.components.hero'` 指定了要查找的类别
- AEM 会解析这个 categories 值

### 步骤 2: 解析 categories

**解析过程：**
```
输入: categories='myapp.components.hero'
  ↓
解析: "myapp.components.hero"
  ↓
准备查找: 查找 categories 属性包含 "myapp.components.hero" 的节点
```

**支持格式：**
```html
<!-- 单个 category -->
categories='myapp.components.hero'

<!-- 多个 categories（数组） -->
categories="['myapp.components.hero', 'myapp.components.base']"

<!-- 多个 categories（字符串） -->
categories='myapp.components.hero,myapp.components.base'
```

### 步骤 3: JCR 查询

**查询位置：**
```
/apps/{project}/clientlibs/**/.content.xml
/libs/{project}/clientlibs/**/.content.xml
```

**查询条件：**
```
节点类型: jcr:primaryType = 'cq:ClientLibraryFolder'
categories 属性: 包含 'myapp.components.hero'
```

**查询示例（概念性）：**
```sql
SELECT * FROM [cq:ClientLibraryFolder] 
WHERE categories CONTAINS 'myapp.components.hero'
```

### 步骤 4: 匹配配置文件

**找到的配置文件：**
```
路径: /apps/myapp/clientlibs/components/hero/.content.xml
```

**配置文件内容：**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base]"/>
```

**匹配结果：**
- ✅ categories 包含 `myapp.components.hero`
- ✅ 节点类型正确
- ✅ 配置文件有效

### 步骤 5: 读取配置

**读取的信息：**
```
categories: [myapp.components.hero]
dependencies: [myapp.base]
allowProxy: true
文件路径: /apps/myapp/clientlibs/components/hero/css/hero.css
```

### 步骤 6: 处理依赖

**依赖处理：**
```
发现依赖: myapp.base
  ↓
递归查找 myapp.base 的 ClientLib
  ↓
找到: /apps/myapp/clientlibs/base/.content.xml
  ↓
读取配置
  ↓
检查是否有依赖（递归）
```

**依赖链示例：**
```
hero → base → theme
  ↓
递归处理:
1. 查找 theme (base 的依赖)
2. 查找 base (hero 的依赖)
3. 查找 hero (请求的 ClientLib)
```

### 步骤 7: 收集文件

**收集 CSS 文件：**
```
依赖文件:
- /apps/myapp/clientlibs/base/css/base.css

主文件:
- /apps/myapp/clientlibs/components/hero/css/hero.css
```

**文件顺序：**
1. 依赖的文件先加载
2. 主文件后加载
3. 按依赖顺序排列

### 步骤 8: 生成代理路径

**代理路径生成：**
```
实际路径: /apps/myapp/clientlibs/components/hero/css/hero.css
  ↓
代理路径: /etc.clientlibs/myapp/clientlibs/components/hero.css
```

**代理路径规则：**
- 移除 `/apps/` 或 `/libs/` 前缀
- 添加 `/etc.clientlibs/` 前缀
- 合并 CSS 文件（如果启用）

### 步骤 9: 输出 HTML

**生成的 HTML：**
```html
<link rel="stylesheet" 
      href="/etc.clientlibs/myapp/clientlibs/components/hero.css"
      type="text/css">
```

---

## 查找路径和优先级

### 查找路径

**主要路径：**
```
1. /apps/{project}/clientlibs/**/.content.xml
2. /libs/{project}/clientlibs/**/.content.xml
```

**路径优先级：**
1. **/apps/** 优先于 **/libs/**
   - `/apps/` 是应用程序层，可以覆盖系统默认配置
   - `/libs/` 是核心库层，包含系统默认配置

**示例：**
```
如果两个位置都有相同的 categories:
- /apps/myapp/clientlibs/base/.content.xml (优先)
- /libs/myapp/clientlibs/base/.content.xml (备用)

AEM 会使用 /apps/ 中的配置
```

### 查找范围

**递归查找：**
```
/apps/myapp/clientlibs/
  ├── base/
  │   └── .content.xml          ← 查找这里
  ├── components/
  │   ├── hero/
  │   │   └── .content.xml      ← 查找这里
  │   └── card/
  │       └── .content.xml      ← 查找这里
  └── theme/
      └── .content.xml          ← 查找这里
```

**查找深度：**
- 递归查找所有子目录
- 不限制深度
- 查找所有 `.content.xml` 文件

### 匹配规则

**完全匹配：**
```xml
<!-- 配置 -->
categories="[myapp.components.hero]"

<!-- 请求 -->
categories='myapp.components.hero'
✅ 匹配
```

**部分匹配（多个 categories）：**
```xml
<!-- 配置 -->
categories="[myapp.components.hero,myapp.components.base]"

<!-- 请求 -->
categories='myapp.components.hero'
✅ 匹配（包含在数组中）
```

**不匹配：**
```xml
<!-- 配置 -->
categories="[myapp.components.card]"

<!-- 请求 -->
categories='myapp.components.hero'
❌ 不匹配
```

---

## 依赖解析机制

### 依赖声明

**配置文件：**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base,myapp.theme]"/>
```

**依赖关系：**
```
hero → base
hero → theme
```

### 依赖解析流程

**步骤 1: 发现依赖**
```
请求: myapp.components.hero
  ↓
找到配置: categories="[myapp.components.hero]"
  ↓
发现依赖: dependencies="[myapp.base,myapp.theme]"
```

**步骤 2: 递归查找依赖**
```
查找 myapp.base:
  ↓
找到: /apps/myapp/clientlibs/base/.content.xml
  ↓
读取配置
  ↓
检查是否有依赖（递归）

查找 myapp.theme:
  ↓
找到: /apps/myapp/clientlibs/theme/.content.xml
  ↓
读取配置
  ↓
检查是否有依赖（递归）
```

**步骤 3: 构建依赖树**
```
hero
├── base
│   └── (无依赖)
└── theme
    └── (无依赖)
```

**步骤 4: 确定加载顺序**
```
1. base (最底层依赖)
2. theme (最底层依赖)
3. hero (请求的 ClientLib)
```

### 依赖链示例

**复杂依赖链：**
```
hero → base → theme → reset
```

**解析过程：**
```
1. 查找 hero
   - 发现依赖: base

2. 查找 base
   - 发现依赖: theme

3. 查找 theme
   - 发现依赖: reset

4. 查找 reset
   - 无依赖

5. 加载顺序:
   reset → theme → base → hero
```

### 循环依赖检测

**循环依赖示例：**
```
hero → base → hero (循环)
```

**AEM 处理：**
- AEM 会检测循环依赖
- 避免无限递归
- 可能抛出错误或警告

**避免循环依赖：**
```
✅ 正确:
hero → base → theme (线性)

❌ 错误:
hero → base → hero (循环)
```

---

## 调试和排查方法

### 方法 1: 使用 AEM ClientLibs 工具

**访问工具：**
```
URL: http://localhost:4502/libs/granite/ui/content/dumplibs.html
```

**功能：**
- 查看所有 ClientLibs
- 查看 categories 和文件
- 查看依赖关系
- 测试加载

**使用步骤：**
1. 打开工具页面
2. 搜索 categories
3. 查看配置和文件
4. 验证依赖关系

### 方法 2: 浏览器开发者工具

**Network 标签：**
```
1. 打开浏览器开发者工具
2. 切换到 Network 标签
3. 刷新页面
4. 过滤 CSS 文件
5. 查看加载的文件和顺序
```

**检查项：**
- CSS 文件是否加载
- 加载顺序是否正确
- 文件路径是否正确
- 是否有 404 错误

**Console 标签：**
```
检查 JavaScript 错误:
- ClientLib 加载错误
- 依赖解析错误
- 文件路径错误
```

### 方法 3: CRX/DE Lite

**访问工具：**
```
URL: http://localhost:4502/crx/de/index.jsp
```

**检查步骤：**
1. 导航到 `/apps/myapp/clientlibs/`
2. 查找对应的 ClientLib 目录
3. 检查 `.content.xml` 文件
4. 验证 categories 和 dependencies
5. 检查 CSS 文件是否存在

**检查项：**
- 文件是否存在
- 配置文件格式是否正确
- categories 拼写是否正确
- 文件路径是否正确

### 方法 4: 日志文件

**查看日志：**
```
路径: /crx-quickstart/logs/error.log
```

**搜索关键词：**
```
- ClientLib
- categories
- ClientLibraryFolder
- 404
```

**常见日志：**
```
ERROR: ClientLib not found: myapp.components.hero
WARN: Circular dependency detected: hero → base → hero
```

### 方法 5: 直接访问代理路径

**测试代理路径：**
```
URL: http://localhost:4502/etc.clientlibs/myapp/clientlibs/components/hero.css
```

**预期结果：**
- 200 OK: 文件存在且可访问
- 404 Not Found: 文件不存在或路径错误
- 403 Forbidden: 权限问题

**检查项：**
- 文件内容是否正确
- 是否包含合并的文件
- 是否被压缩

---

## 常见问题和解决方案

### 问题 1: CSS 文件不加载

**症状：**
- 页面没有样式
- Network 标签显示 404 错误

**可能原因：**
1. categories 拼写错误
2. ClientLib 配置文件不存在
3. CSS 文件路径错误
4. allowProxy 未启用

**解决方案：**
```
1. 检查 categories 拼写
   - 模板中的 categories
   - 配置文件中的 categories
   - 确保完全匹配

2. 检查配置文件是否存在
   - /apps/myapp/clientlibs/components/hero/.content.xml
   - 文件格式是否正确

3. 检查 CSS 文件路径
   - /apps/myapp/clientlibs/components/hero/css/hero.css
   - 文件是否存在

4. 启用 allowProxy
   - allowProxy="{Boolean}true"
```

### 问题 2: 依赖不加载

**症状：**
- 基础样式未加载
- 组件样式缺少依赖

**可能原因：**
1. dependencies 配置错误
2. 依赖的 ClientLib 不存在
3. 循环依赖

**解决方案：**
```
1. 检查 dependencies 配置
   - 格式是否正确
   - categories 拼写是否正确

2. 检查依赖的 ClientLib 是否存在
   - /apps/myapp/clientlibs/base/.content.xml
   - 配置文件是否正确

3. 检查循环依赖
   - 使用依赖图工具
   - 避免循环依赖
```

### 问题 3: 加载顺序错误

**症状：**
- 样式被覆盖
- 依赖的样式在组件样式之后加载

**可能原因：**
1. dependencies 配置错误
2. 依赖链过长
3. 多个 ClientLibs 冲突

**解决方案：**
```
1. 检查 dependencies 配置
   - 确保所有依赖都声明
   - 依赖顺序正确

2. 简化依赖链
   - 减少不必要的依赖
   - 使用 embed 合并文件

3. 检查 categories 冲突
   - 确保 categories 唯一
   - 避免重复定义
```

### 问题 4: 代理路径 404

**症状：**
- 直接访问代理路径返回 404
- 文件存在但无法访问

**可能原因：**
1. allowProxy 未启用
2. 路径配置错误
3. 权限问题

**解决方案：**
```
1. 启用 allowProxy
   - allowProxy="{Boolean}true"

2. 检查路径配置
   - 确保路径正确
   - 检查代理路径规则

3. 检查权限
   - 确保有读取权限
   - 检查 ACL 配置
```

### 问题 5: 文件合并失败

**症状：**
- 多个文件未合并
- 生成了多个 <link> 标签

**可能原因：**
1. embed 配置错误
2. 合并功能未启用
3. 文件格式问题

**解决方案：**
```
1. 检查 embed 配置
   - 格式是否正确
   - categories 拼写是否正确

2. 启用合并功能
   - 检查 AEM 配置
   - 确保合并功能启用

3. 检查文件格式
   - CSS 文件格式正确
   - 无语法错误
```

---

## 调试检查清单

### 配置检查

- [ ] `.content.xml` 文件存在
- [ ] `jcr:primaryType="cq:ClientLibraryFolder"`
- [ ] `categories` 属性存在且格式正确
- [ ] `dependencies` 配置正确（如果有）
- [ ] `allowProxy` 已启用（生产环境）

### 文件检查

- [ ] CSS 文件存在
- [ ] 文件路径正确
- [ ] 文件格式正确
- [ ] 文件有读取权限

### 模板检查

- [ ] HTL 模板中正确引入 clientlib
- [ ] `categories` 参数正确
- [ ] 拼写无误

### 依赖检查

- [ ] 所有依赖的 ClientLibs 存在
- [ ] 依赖关系正确
- [ ] 无循环依赖

### 访问检查

- [ ] 代理路径可访问
- [ ] 浏览器 Network 标签显示正常
- [ ] 无 404 或 403 错误

---

## 总结

### 查找机制要点

1. **通过 categories 查找**: AEM 根据 categories 在 JCR 中查找匹配的 ClientLib
2. **路径优先级**: `/apps/` 优先于 `/libs/`
3. **递归查找**: 在所有子目录中查找
4. **依赖自动处理**: AEM 自动处理依赖关系和加载顺序
5. **代理路径访问**: 通过 `/etc.clientlibs/` 访问文件

### 调试要点

1. **使用工具**: AEM ClientLibs 工具、浏览器开发者工具、CRX/DE Lite
2. **检查配置**: 配置文件格式、categories 拼写、依赖关系
3. **检查文件**: 文件存在、路径正确、格式正确
4. **检查访问**: 代理路径可访问、无权限问题

### 常见问题

1. **CSS 不加载**: 检查 categories、配置文件、文件路径
2. **依赖不加载**: 检查 dependencies 配置、依赖的 ClientLib 是否存在
3. **加载顺序错误**: 检查 dependencies 配置、依赖链
4. **代理路径 404**: 检查 allowProxy、路径配置、权限

