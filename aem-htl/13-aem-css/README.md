# AEM CSS 管理文档

本目录包含关于 AEM 系统中 CSS 管理机制的详细文档。

## 文档列表

### 1. [CSS 管理概述](./01-css-management-overview.md)
**核心问题解答：**
- 为什么 CSS 不在组件目录下？
- AEM 如何知道组件要用哪个 CSS？
- AEM 如何管理 CSS？

**内容：**
- ClientLibs 机制介绍
- 目录结构对比
- 查找和加载流程
- 实际应用示例
- 最佳实践

### 2. [ClientLibs 配置文件详解](./02-clientlibs-configuration.md)
**配置文件说明：**
- 配置文件结构
- 必需属性详解
- 可选属性详解
- 高级配置
- 常见配置模式

**内容：**
- `jcr:primaryType`
- `categories`
- `dependencies`
- `allowProxy`
- `embed`
- `cssProcessor` / `jsProcessor`

### 3. [CSS 查找机制详解](./03-css-lookup-mechanism.md)
**查找机制说明：**
- 查找流程详解
- 查找路径和优先级
- 依赖解析机制
- 调试和排查方法
- 常见问题和解决方案

**内容：**
- 9 步查找流程
- JCR 查询机制
- 依赖解析算法
- 调试工具使用
- 问题排查清单

### 4. [构建链与预处理器集成](./04-build-chain-and-preprocessors.md)
**构建链说明：**
- 为什么需要构建链
- ui.frontend 模块结构
- Sass / Less / PostCSS 集成
- Webpack / Vite 配置
- 编译输出到 ClientLibs
- Categories 命名一致性保证

**内容：**
- AEM 项目结构（ui.frontend 模块）
- 预处理器集成（Sass/SCSS、Less、PostCSS）
- 构建工具配置（Webpack、Vite）
- 输出路径映射规则
- 自动化一致性保证方案
- 实际项目配置示例
- CI/CD 集成
- 调试与验证方法

### 5. [CSS 发现与解析机制详解](./05-css-discovery-and-resolution.md) ⭐ **推荐阅读**
**完整流程说明：**
- 从组件声明到 CSS 加载的完整流程
- 9 步详细解析过程
- JCR 查询机制
- 依赖解析算法
- 代理路径生成
- 实际案例分析
- 迁移准备指南

**内容：**
- 组件/页面如何声明 CSS
- Categories 如何解析
- JCR 如何查询查找 ClientLib
- ClientLib 配置如何解析
- CSS 文件如何收集
- 依赖关系如何解析
- 文件如何合并和处理
- 代理路径如何生成
- HTML 如何输出
- 完整流程图和时间线
- 实际案例分析（简单组件、有依赖组件、多 categories）
- React 迁移准备指南

## 快速开始

### 问题 1: 为什么 CSS 不在组件目录下？

**答案：**
- CSS 文件放在 `/apps/{project}/clientlibs/` 目录
- 通过 ClientLibs 机制集中管理
- 支持依赖管理、文件合并、性能优化

**详细说明：** 见 [01-css-management-overview.md](./01-css-management-overview.md#为什么-css-不在组件目录下)

### 问题 2: AEM 如何知道组件要用哪个 CSS？

**答案：**
- 通过 **categories（类别）** 机制
- 组件在 HTL 模板中声明 `categories`
- AEM 根据 categories 查找对应的 ClientLib

**示例：**
```html
<!-- 组件模板 -->
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>
```

**详细说明：** 见 [01-css-management-overview.md](./01-css-management-overview.md#aem-如何知道组件要用哪个-css)

### 问题 3: AEM 如何管理 CSS？有什么配置文件？

**答案：**
- 使用 **ClientLibs** 机制管理
- 配置文件是 `.content.xml`
- 位置：`/apps/{project}/clientlibs/{category}/.content.xml`

**配置文件示例：**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base]"
          allowProxy="{Boolean}true"/>
```

**详细说明：** 见 [02-clientlibs-configuration.md](./02-clientlibs-configuration.md)

## 核心概念

### ClientLibs（客户端库）

**定义：** AEM 管理 CSS 和 JavaScript 的核心机制

**关键概念：**
- **ClientLibraryFolder**: JCR 节点类型
- **Categories**: 标识符，用于查找和引用
- **Dependencies**: 依赖关系，自动处理加载顺序
- **Proxy Path**: 代理路径，通过 `/etc.clientlibs/` 访问

### Categories（类别）

**作用：** 关联组件和 CSS 的标识符

**格式：**
- 点分隔的层次结构
- 示例：`myapp.components.hero`

**使用：**
```html
<!-- 组件模板中 -->
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>
```

```xml
<!-- ClientLib 配置中 -->
categories="[myapp.components.hero]"
```

### 目录结构

**组件目录（只有模板和配置）：**
```
/apps/myapp/components/hero/
  ├── .content.xml
  ├── hero.html
  ├── _cq_dialog/
  └── _cq_editConfig.xml
```

**ClientLibs 目录（CSS/JS 文件）：**
```
/apps/myapp/clientlibs/
  ├── base/
  │   ├── .content.xml
  │   └── css/
  │       └── base.css
  ├── components/
  │   └── hero/
  │       ├── .content.xml
  │       └── css/
  │           └── hero.css
  └── theme/
      ├── .content.xml
      └── css/
          └── theme.css
```

## 工作流程

### 完整流程

```
1. 组件模板请求 CSS
   ↓
2. 指定 categories: 'myapp.components.hero'
   ↓
3. AEM 在 JCR 中查找
   ↓
4. 找到配置文件: /apps/myapp/clientlibs/components/hero/.content.xml
   ↓
5. 读取配置，获取 CSS 文件路径
   ↓
6. 处理依赖（如果有）
   ↓
7. 收集 CSS 文件
   ↓
8. 生成代理路径: /etc.clientlibs/myapp/clientlibs/components/hero.css
   ↓
9. 输出 HTML: <link rel="stylesheet" href="...">
```

### 依赖处理

```
请求: myapp.components.hero
  ↓
发现依赖: myapp.base
  ↓
递归查找依赖
  ↓
加载顺序: myapp.base → myapp.components.hero
```

## 配置文件

### 基本配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base]"
          allowProxy="{Boolean}true"/>
```

### 关键属性

| 属性 | 必需 | 说明 |
|------|------|------|
| `jcr:primaryType` | ✅ | 必须为 `cq:ClientLibraryFolder` |
| `categories` | ✅ | ClientLib 的标识符 |
| `dependencies` | ❌ | 依赖的其他 ClientLibs |
| `allowProxy` | ❌ | 是否允许代理路径访问 |
| `embed` | ❌ | 嵌入其他 ClientLibs |
| `cssProcessor` | ❌ | CSS 处理器配置 |
| `jsProcessor` | ❌ | JavaScript 处理器配置 |

## 调试方法

### 1. AEM ClientLibs 工具
```
URL: http://localhost:4502/libs/granite/ui/content/dumplibs.html
```

### 2. 浏览器开发者工具
- Network 标签：查看 CSS 文件加载
- Console 标签：查看错误信息

### 3. CRX/DE Lite
```
URL: http://localhost:4502/crx/de/index.jsp
```
检查配置文件和文件路径

### 4. 直接访问代理路径
```
URL: http://localhost:4502/etc.clientlibs/myapp/clientlibs/components/hero.css
```

## 常见问题

### CSS 文件不加载
- 检查 categories 拼写
- 检查配置文件是否存在
- 检查 CSS 文件路径
- 启用 allowProxy

### 依赖不加载
- 检查 dependencies 配置
- 检查依赖的 ClientLib 是否存在
- 避免循环依赖

### 加载顺序错误
- 检查 dependencies 配置
- 简化依赖链
- 检查 categories 冲突

## 最佳实践

### 命名规范
```
✅ 好的命名:
- myapp.components.hero
- myapp.base
- myapp.theme

❌ 不好的命名:
- hero (太简单)
- myapp-hero (使用连字符)
- Hero (大写开头)
```

### 目录组织
```
/apps/myapp/clientlibs/
  ├── base/              # 基础样式
  ├── components/        # 组件样式
  ├── theme/            # 主题样式
  └── vendor/           # 第三方库
```

### 依赖管理
- 合理声明 dependencies
- 避免循环依赖
- 使用 embed 优化性能

### 性能优化
- 启用 allowProxy（生产环境）
- 启用 CSS/JS 压缩
- 使用 embed 合并文件
- 按需加载

## 相关文档

- [HTL 模板中的 ClientLibs](../04-advanced/02-clientlibs.html)
- [组件依赖解析](../04-advanced/04-component-dependency-resolution.md)
- [组件 HTL 模板](../02-components/05-component-htl-template.html)

## 学习路径

1. **入门**: 阅读 [01-css-management-overview.md](./01-css-management-overview.md)
   - 理解为什么 CSS 不在组件目录下
   - 理解 AEM 如何知道组件要用哪个 CSS
   - 理解 AEM 如何管理 CSS

2. **配置**: 阅读 [02-clientlibs-configuration.md](./02-clientlibs-configuration.md)
   - 学习配置文件结构
   - 学习各种配置属性
   - 学习常见配置模式

3. **深入**: 阅读 [03-css-lookup-mechanism.md](./03-css-lookup-mechanism.md)
   - 理解查找机制
   - 学习调试方法
   - 解决常见问题

4. **专家**: 阅读 [04-build-chain-and-preprocessors.md](./04-build-chain-and-preprocessors.md)
   - 理解构建链的必要性
   - 学习 ui.frontend 模块设计
   - 掌握预处理器集成
   - 学习 Webpack/Vite 配置
   - 掌握 Categories 命名一致性保证
   - 学习 CI/CD 集成

5. **迁移准备**: 阅读 [05-css-discovery-and-resolution.md](./05-css-discovery-and-resolution.md) ⭐
   - 理解完整的 CSS 查找流程
   - 掌握从声明到加载的每个步骤
   - 学习如何识别组件使用的 CSS
   - 学习如何查找对应的 ClientLib
   - 学习如何收集 CSS 文件
   - 学习如何分析依赖关系
   - 学习如何迁移到 React

## 总结

### 核心要点

1. **CSS 不在组件目录下**，而是放在 `/apps/{project}/clientlibs/` 目录
2. **通过 categories 关联**，组件在 HTL 中声明 categories，AEM 查找对应的 ClientLib
3. **配置文件是 `.content.xml`**，定义了 categories、dependencies 等属性
4. **依赖自动处理**，AEM 自动处理依赖关系和加载顺序
5. **代理路径访问**，通过 `/etc.clientlibs/` 访问，不暴露内部结构
6. **构建链集成**，实际项目使用预处理器（Sass/SCSS）和构建工具（Webpack/Vite）编译 CSS
7. **一致性保证**，通过配置文件、自动生成、验证脚本确保编译输出与 categories 命名一致

### 工作流程

```
组件模板 → 声明 categories → AEM 查找 ClientLib → 读取配置 → 
处理依赖 → 收集文件 → 合并生成 → 输出 HTML
```

### 核心文件

- **组件模板**: `/apps/myapp/components/{component}/{component}.html`
- **ClientLib 配置**: `/apps/myapp/clientlibs/{category}/.content.xml`
- **CSS 文件**: `/apps/myapp/clientlibs/{category}/css/*.css`

