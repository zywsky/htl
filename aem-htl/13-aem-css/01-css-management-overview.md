# AEM CSS 管理机制详解

## 目录
1. [为什么 CSS 不在组件目录下？](#为什么-css-不在组件目录下)
2. [AEM 如何知道组件要用哪个 CSS？](#aem-如何知道组件要用哪个-css)
3. [AEM 如何管理 CSS？](#aem-如何管理-css)
4. [ClientLibs 配置文件详解](#clientlibs-配置文件详解)
5. [完整的查找和加载流程](#完整的查找和加载流程)
6. [实际应用示例](#实际应用示例)
7. [最佳实践](#最佳实践)

---

## 为什么 CSS 不在组件目录下？

### 1.1 传统方式 vs AEM 方式

**传统方式（不推荐）：**
```
/components/hero/
  ├── hero.html
  ├── hero.css          ← CSS 直接放在组件目录下
  └── hero.js
```

**AEM 方式（推荐）：**
```
/components/hero/
  └── hero.html         ← 只有 HTL 模板

/clientlibs/components/hero/
  ├── .content.xml      ← ClientLib 配置
  ├── css/
  │   └── hero.css      ← CSS 放在 ClientLibs 目录
  └── js/
      └── hero.js
```

### 1.2 为什么采用 ClientLibs 方式？

#### 优势 1: 集中管理
- **统一位置**: 所有 CSS/JS 文件集中在 `/apps/{project}/clientlibs/` 目录
- **易于维护**: 不需要在每个组件目录下查找样式文件
- **清晰组织**: 按功能模块组织（components, base, theme, vendor）

#### 优势 2: 依赖管理
- **声明依赖**: 通过 `dependencies` 属性声明依赖关系
- **自动加载**: AEM 自动处理依赖顺序
- **避免重复**: 多个组件共享的基础样式只需加载一次

#### 优势 3: 性能优化
- **文件合并**: AEM 可以合并多个 CSS 文件
- **压缩优化**: 自动压缩 CSS/JS
- **缓存控制**: 通过代理路径实现版本控制和缓存

#### 优势 4: 安全性
- **代理路径**: 通过 `/etc.clientlibs/` 代理路径访问，不暴露内部结构
- **权限控制**: 可以控制哪些 ClientLibs 可以被访问

#### 优势 5: 灵活性
- **多 categories**: 一个 ClientLib 可以有多个 categories
- **条件加载**: 可以根据需要动态加载
- **主题切换**: 可以轻松切换不同的主题样式

### 1.3 目录结构对比

**组件目录结构（只有模板和配置）：**
```
/apps/myapp/components/hero/
  ├── .content.xml              # 组件定义
  ├── hero.html                 # HTL 模板
  ├── _cq_dialog/               # 对话框配置
  ├── _cq_editConfig.xml        # 编辑配置
  └── _cq_template/             # 初始内容
```

**ClientLibs 目录结构（CSS/JS 文件）：**
```
/apps/myapp/clientlibs/
  ├── base/                     # 基础样式
  │   ├── .content.xml
  │   └── css/
  │       └── base.css
  ├── components/               # 组件样式
  │   ├── hero/
  │   │   ├── .content.xml
  │   │   ├── css/
  │   │   │   └── hero.css
  │   │   └── js/
  │   │       └── hero.js
  │   └── card/
  │       ├── .content.xml
  │       └── css/
  │           └── card.css
  ├── theme/                    # 主题样式
  │   ├── .content.xml
  │   └── css/
  │       └── theme.css
  └── vendor/                   # 第三方库
      ├── jquery/
      │   ├── .content.xml
      │   └── js/
      │       └── jquery.min.js
```

---

## AEM 如何知道组件要用哪个 CSS？

### 2.1 核心机制：Categories（类别）

AEM 通过 **categories**（类别）来关联组件和 CSS。

**工作原理：**
1. 组件在 HTL 模板中声明需要的 categories
2. AEM 根据 categories 查找对应的 ClientLib
3. 加载找到的 ClientLib 中的 CSS/JS 文件

### 2.2 组件中声明 CSS

**在 HTL 模板中：**
```html
<!-- 引入 ClientLib 模板 -->
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>

<!-- 加载 CSS -->
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>

<!-- 组件内容 -->
<div class="hero-component">
    <h1>${properties.title}</h1>
</div>
```

**关键点：**
- `categories='myapp.components.hero'` 指定了要加载的 CSS 类别
- AEM 会查找 categories 属性包含 `myapp.components.hero` 的 ClientLib
- 找到后加载该 ClientLib 的 CSS 文件

### 2.3 ClientLib 配置中定义 Categories

**ClientLib 配置文件：**
```xml
<!-- /apps/myapp/clientlibs/components/hero/.content.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base]"/>
```

**关键点：**
- `categories="[myapp.components.hero]"` 定义了此 ClientLib 的标识符
- 当组件请求 `myapp.components.hero` 时，AEM 会找到这个配置
- `dependencies="[myapp.base]"` 声明了依赖关系

### 2.4 查找流程

```
组件模板请求 CSS
  ↓
指定 categories: 'myapp.components.hero'
  ↓
AEM 在 JCR 中查找
  ↓
搜索路径: /apps/myapp/clientlibs/**/.content.xml
  ↓
查找 categories 属性包含 'myapp.components.hero' 的节点
  ↓
找到: /apps/myapp/clientlibs/components/hero/.content.xml
  ↓
读取配置，获取 CSS 文件路径
  ↓
加载: /apps/myapp/clientlibs/components/hero/css/hero.css
  ↓
生成代理路径: /etc.clientlibs/myapp/clientlibs/components/hero.css
  ↓
输出 HTML: <link rel="stylesheet" href="/etc.clientlibs/...">
```

---

## AEM 如何管理 CSS？

### 3.1 ClientLibs 机制

**ClientLibs（客户端库）** 是 AEM 管理 CSS 和 JavaScript 的核心机制。

**核心概念：**
- **ClientLibraryFolder**: JCR 节点类型，标识这是一个客户端库
- **Categories**: 标识符，用于查找和引用
- **Dependencies**: 依赖关系，自动处理加载顺序
- **Proxy Path**: 代理路径，通过 `/etc.clientlibs/` 访问

### 3.2 配置文件结构

**基本配置文件：**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base,myapp.theme]"
          allowProxy="{Boolean}true"/>
```

**文件位置：**
```
/apps/myapp/clientlibs/components/hero/.content.xml
```

### 3.3 目录结构要求

**标准目录结构：**
```
/apps/myapp/clientlibs/components/hero/
  ├── .content.xml          # 配置文件（必需）
  ├── css/                  # CSS 文件目录（可选）
  │   ├── hero.css
  │   └── hero-responsive.css
  ├── js/                   # JavaScript 文件目录（可选）
  │   └── hero.js
  └── resources/            # 其他资源（可选）
      └── images/
```

**注意：**
- `.content.xml` 是必需的配置文件
- `css/` 和 `js/` 目录是可选的，根据实际需要创建
- 文件会按字母顺序加载

### 3.4 多文件管理

**一个 ClientLib 可以包含多个 CSS 文件：**
```
/apps/myapp/clientlibs/components/hero/
  ├── .content.xml
  └── css/
      ├── hero-base.css      # 基础样式
      ├── hero-layout.css    # 布局样式
      └── hero-theme.css     # 主题样式
```

**加载顺序：**
- 按文件名字母顺序加载
- 依赖的 ClientLibs 先加载
- 然后加载当前 ClientLib 的文件

---

## ClientLibs 配置文件详解

### 4.1 基本属性

#### jcr:primaryType
```xml
jcr:primaryType="cq:ClientLibraryFolder"
```
- **必需**: 标识这是一个客户端库文件夹
- **类型**: 必须是 `cq:ClientLibraryFolder`

#### categories
```xml
categories="[myapp.components.hero]"
```
- **必需**: 定义此 ClientLib 的标识符
- **格式**: 数组格式，用方括号包裹
- **单个值**: `categories="[myapp.components.hero]"`
- **多个值**: `categories="[myapp.components.hero,myapp.components.base]"`

#### dependencies
```xml
dependencies="[myapp.base,myapp.theme]"
```
- **可选**: 声明依赖的其他 ClientLib categories
- **格式**: 数组格式，用方括号包裹
- **作用**: AEM 会自动先加载依赖的 ClientLibs

#### allowProxy
```xml
allowProxy="{Boolean}true"
```
- **可选**: 是否允许通过代理路径访问
- **默认**: false
- **推荐**: true（允许通过 `/etc.clientlibs/` 访问）

### 4.2 高级属性

#### embed
```xml
embed="[myapp.components.card,myapp.components.button]"
```
- **可选**: 嵌入其他 ClientLibs 的内容
- **作用**: 将其他 ClientLibs 的文件合并到当前 ClientLib

#### channels
```xml
channels="[mobile,tablet,desktop]"
```
- **可选**: 定义支持的渠道
- **作用**: 可以根据设备类型加载不同的样式

#### jsProcessor
```xml
jsProcessor="[default:min:gcc]"
```
- **可选**: JavaScript 处理器配置
- **作用**: 指定如何压缩和处理 JavaScript

#### cssProcessor
```xml
cssProcessor="[default:none,default:min]"
```
- **可选**: CSS 处理器配置
- **作用**: 指定如何压缩和处理 CSS

### 4.3 完整配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base,myapp.theme]"
          allowProxy="{Boolean}true"
          embed="[myapp.components.card]"
          jsProcessor="[default:min:gcc]"
          cssProcessor="[default:min]"/>
```

---

## 完整的查找和加载流程

### 5.1 步骤详解

**步骤 1: 组件模板请求 CSS**
```html
<!-- hero.html -->
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>
```

**步骤 2: AEM 解析 categories**
- 解析得到: `myapp.components.hero`
- 准备查找对应的 ClientLib

**步骤 3: JCR 查询查找 ClientLib**
```
搜索路径:
- /apps/myapp/clientlibs/**/.content.xml
- /libs/myapp/clientlibs/**/.content.xml

查询条件:
- jcr:primaryType = 'cq:ClientLibraryFolder'
- categories 包含 'myapp.components.hero'
```

**步骤 4: 找到配置文件**
```
找到: /apps/myapp/clientlibs/components/hero/.content.xml

读取配置:
- categories: [myapp.components.hero]
- dependencies: [myapp.base]
- allowProxy: true
```

**步骤 5: 处理依赖**
```
发现依赖: myapp.base
  ↓
递归查找 myapp.base 的 ClientLib
  ↓
找到: /apps/myapp/clientlibs/base/.content.xml
  ↓
加载 myapp.base 的 CSS（在 hero 之前）
```

**步骤 6: 收集文件**
```
收集 CSS 文件:
1. /apps/myapp/clientlibs/base/css/base.css (依赖)
2. /apps/myapp/clientlibs/components/hero/css/hero.css (主库)
```

**步骤 7: 合并和生成**
```
合并文件（如果启用）:
- base.css + hero.css → merged.css

生成代理路径:
- /etc.clientlibs/myapp/clientlibs/components/hero.css
```

**步骤 8: 输出 HTML**
```html
<link rel="stylesheet" 
      href="/etc.clientlibs/myapp/clientlibs/components/hero.css"
      type="text/css">
```

### 5.2 依赖处理示例

**配置关系：**
```
hero ClientLib:
  categories: [myapp.components.hero]
  dependencies: [myapp.base]

base ClientLib:
  categories: [myapp.base]
  dependencies: [myapp.theme]

theme ClientLib:
  categories: [myapp.theme]
  dependencies: []
```

**加载顺序：**
```
1. myapp.theme (最底层依赖)
2. myapp.base (hero 的依赖)
3. myapp.components.hero (请求的 ClientLib)
```

---

## 实际应用示例

### 6.1 基础组件示例

**组件结构：**
```
/apps/myapp/components/hero/
  └── hero.html

/apps/myapp/clientlibs/components/hero/
  ├── .content.xml
  └── css/
      └── hero.css
```

**hero.html:**
```html
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>

<div class="hero-component">
    <h1 class="hero-title">${properties.title}</h1>
    <p class="hero-description">${properties.description}</p>
</div>
```

**hero.css:**
```css
.hero-component {
    padding: 2rem;
    background-color: #f0f0f0;
}

.hero-title {
    font-size: 2rem;
    color: #333;
}

.hero-description {
    font-size: 1rem;
    color: #666;
}
```

**ClientLib 配置 (.content.xml):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          allowProxy="{Boolean}true"/>
```

### 6.2 带依赖的组件示例

**配置关系：**
```
base ClientLib (基础样式):
  categories: [myapp.base]
  
hero ClientLib (组件样式):
  categories: [myapp.components.hero]
  dependencies: [myapp.base]
```

**base.css:**
```css
/* 基础重置样式 */
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: Arial, sans-serif;
    line-height: 1.6;
}
```

**hero.css:**
```css
/* 组件特定样式 */
.hero-component {
    padding: 2rem;
    background-color: #f0f0f0;
}
```

**加载结果：**
- 先加载 `myapp.base` 的基础样式
- 再加载 `myapp.components.hero` 的组件样式
- 最终 HTML 中会有两个 `<link>` 标签（或合并后的一个）

### 6.3 多文件 ClientLib 示例

**目录结构：**
```
/apps/myapp/clientlibs/components/hero/
  ├── .content.xml
  └── css/
      ├── hero-base.css
      ├── hero-layout.css
      └── hero-theme.css
```

**文件内容：**
```css
/* hero-base.css */
.hero-component {
    display: block;
}

/* hero-layout.css */
.hero-component {
    padding: 2rem;
    margin: 1rem 0;
}

/* hero-theme.css */
.hero-component {
    background-color: #f0f0f0;
    color: #333;
}
```

**加载顺序：**
- 按文件名字母顺序：`hero-base.css` → `hero-layout.css` → `hero-theme.css`

---

## 最佳实践

### 7.1 命名规范

**Categories 命名：**
```
✅ 好的命名:
- myapp.components.hero
- myapp.components.card
- myapp.base
- myapp.theme

❌ 不好的命名:
- hero (太简单，可能冲突)
- myapp-hero (使用连字符)
- Hero (大写开头)
```

**目录结构命名：**
```
✅ 好的结构:
/apps/myapp/clientlibs/
  ├── base/
  ├── components/
  │   ├── hero/
  │   └── card/
  ├── theme/
  └── vendor/

❌ 不好的结构:
/apps/myapp/clientlibs/
  ├── hero/ (应该放在 components 下)
  └── card/
```

### 7.2 组织方式

**按功能模块组织：**
```
/apps/myapp/clientlibs/
  ├── base/              # 基础样式（重置、工具类等）
  ├── components/        # 组件样式
  │   ├── hero/
  │   ├── card/
  │   └── button/
  ├── theme/            # 主题样式
  └── vendor/           # 第三方库
      ├── jquery/
      └── bootstrap/
```

### 7.3 依赖管理

**声明依赖：**
```xml
<!-- hero ClientLib -->
categories="[myapp.components.hero]"
dependencies="[myapp.base,myapp.theme]"
```

**避免循环依赖：**
```
❌ 错误示例:
hero → base → hero (循环依赖)

✅ 正确示例:
hero → base → theme (线性依赖)
```

### 7.4 性能优化

**合并文件：**
- 使用 `embed` 属性合并相关文件
- 启用 CSS 压缩 (`cssProcessor="[default:min]"`)

**按需加载：**
```html
<!-- 只在需要时加载 -->
<sly data-sly-test="${properties.showAdvanced}">
    <sly data-sly-call="${clientlib.css @ categories='myapp.components.advanced'}"></sly>
</sly>
```

**使用代理路径：**
```xml
allowProxy="{Boolean}true"
```
- 启用代理路径访问
- 支持缓存和版本控制

### 7.5 调试技巧

**查看 ClientLibs：**
- 访问: `http://localhost:4502/libs/granite/ui/content/dumplibs.html`
- 查看所有 ClientLibs 的 categories 和文件

**检查加载顺序：**
- 查看浏览器开发者工具的 Network 标签
- 检查 CSS 文件的加载顺序

**验证配置：**
- 检查 `.content.xml` 文件格式
- 确认 categories 拼写正确
- 验证文件路径存在

---

## 总结

### 关键要点

1. **CSS 不在组件目录下**，而是放在 `/apps/{project}/clientlibs/` 目录
2. **通过 categories 关联**，组件在 HTL 中声明 categories，AEM 查找对应的 ClientLib
3. **配置文件是 `.content.xml`**，定义了 categories、dependencies 等属性
4. **依赖自动处理**，AEM 自动处理依赖关系和加载顺序
5. **代理路径访问**，通过 `/etc.clientlibs/` 访问，不暴露内部结构

### 工作流程

```
组件模板 → 声明 categories → AEM 查找 ClientLib → 读取配置 → 
处理依赖 → 收集文件 → 合并生成 → 输出 HTML
```

### 核心文件

- **组件模板**: `/apps/myapp/components/{component}/{component}.html`
- **ClientLib 配置**: `/apps/myapp/clientlibs/{category}/.content.xml`
- **CSS 文件**: `/apps/myapp/clientlibs/{category}/css/*.css`

