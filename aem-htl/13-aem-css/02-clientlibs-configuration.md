# ClientLibs 配置文件详解

## 目录
1. [配置文件结构](#配置文件结构)
2. [必需属性](#必需属性)
3. [可选属性](#可选属性)
4. [高级配置](#高级配置)
5. [配置文件示例](#配置文件示例)
6. [常见配置模式](#常见配置模式)

---

## 配置文件结构

### 基本结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          <!-- 属性定义 -->
          />
```

### 命名空间说明

- **xmlns:jcr**: JCR 命名空间，用于 JCR 标准属性
- **xmlns:cq**: CQ/AEM 命名空间，用于 AEM 特定属性
- **jcr:primaryType**: JCR 节点类型

---

## 必需属性

### jcr:primaryType

```xml
jcr:primaryType="cq:ClientLibraryFolder"
```

**说明：**
- **必需**: 必须设置为 `cq:ClientLibraryFolder`
- **作用**: 标识这是一个客户端库文件夹
- **类型**: 字符串，固定值

**错误示例：**
```xml
❌ jcr:primaryType="nt:unstructured"  <!-- 错误 -->
❌ jcr:primaryType="cq:Component"     <!-- 错误 -->
```

### categories

```xml
categories="[myapp.components.hero]"
```

**说明：**
- **必需**: 定义此 ClientLib 的标识符
- **格式**: 数组格式，用方括号 `[]` 包裹
- **作用**: 用于在 HTL 模板中引用此 ClientLib

**单个值：**
```xml
categories="[myapp.components.hero]"
```

**多个值：**
```xml
categories="[myapp.components.hero,myapp.components.base]"
```

**命名规范：**
```
✅ 好的命名:
- myapp.components.hero
- myapp.base
- myapp.theme.dark

❌ 不好的命名:
- hero (太简单)
- myapp-hero (使用连字符)
- Hero (大写开头)
```

---

## 可选属性

### dependencies

```xml
dependencies="[myapp.base,myapp.theme]"
```

**说明：**
- **可选**: 声明依赖的其他 ClientLib categories
- **格式**: 数组格式，用方括号 `[]` 包裹
- **作用**: AEM 会自动先加载依赖的 ClientLibs

**示例：**
```xml
<!-- hero ClientLib 依赖 base 和 theme -->
categories="[myapp.components.hero]"
dependencies="[myapp.base,myapp.theme]"
```

**依赖顺序：**
- 依赖的 ClientLibs 会先加载
- 如果有嵌套依赖，会递归处理
- 最终按依赖顺序加载

**依赖链示例：**
```
hero → base → theme
加载顺序: theme → base → hero
```

### allowProxy

```xml
allowProxy="{Boolean}true"
```

**说明：**
- **可选**: 是否允许通过代理路径访问
- **默认**: `false`
- **推荐**: `true`（生产环境推荐）

**代理路径：**
```
实际路径: /apps/myapp/clientlibs/components/hero/css/hero.css
代理路径: /etc.clientlibs/myapp/clientlibs/components/hero.css
```

**优势：**
- 不暴露内部路径结构
- 支持版本控制和缓存
- 允许跨域访问

**使用建议：**
```xml
<!-- 开发环境 -->
allowProxy="{Boolean}false"

<!-- 生产环境 -->
allowProxy="{Boolean}true"
```

### embed

```xml
embed="[myapp.components.card,myapp.components.button]"
```

**说明：**
- **可选**: 嵌入其他 ClientLibs 的内容
- **格式**: 数组格式，用方括号 `[]` 包裹
- **作用**: 将其他 ClientLibs 的文件合并到当前 ClientLib

**示例：**
```xml
<!-- 将 card 和 button 的 CSS 合并到 hero 中 -->
categories="[myapp.components.hero]"
embed="[myapp.components.card,myapp.components.button]"
```

**使用场景：**
- 需要将多个组件的样式合并到一个文件中
- 减少 HTTP 请求数量
- 优化性能

**注意：**
- `embed` 和 `dependencies` 的区别：
  - `dependencies`: 单独加载，生成多个 `<link>` 标签
  - `embed`: 合并加载，生成一个 `<link>` 标签

### channels

```xml
channels="[mobile,tablet,desktop]"
```

**说明：**
- **可选**: 定义支持的渠道
- **格式**: 数组格式，用方括号 `[]` 包裹
- **作用**: 可以根据设备类型加载不同的样式

**示例：**
```xml
categories="[myapp.components.hero]"
channels="[mobile,tablet,desktop]"
```

**使用场景：**
- 响应式设计
- 不同设备使用不同的样式文件
- 移动端优化

### jsProcessor

```xml
jsProcessor="[default:min:gcc]"
```

**说明：**
- **可选**: JavaScript 处理器配置
- **格式**: 数组格式，用方括号 `[]` 包裹
- **作用**: 指定如何压缩和处理 JavaScript

**可用选项：**
- `default`: 默认处理器
- `min`: 最小化（压缩）
- `gcc`: Google Closure Compiler
- `none`: 不处理

**示例：**
```xml
<!-- 使用默认处理器压缩 -->
jsProcessor="[default:min]"

<!-- 使用 Google Closure Compiler -->
jsProcessor="[default:min:gcc]"

<!-- 不处理 -->
jsProcessor="[none]"
```

### cssProcessor

```xml
cssProcessor="[default:min]"
```

**说明：**
- **可选**: CSS 处理器配置
- **格式**: 数组格式，用方括号 `[]` 包裹
- **作用**: 指定如何压缩和处理 CSS

**可用选项：**
- `default`: 默认处理器
- `min`: 最小化（压缩）
- `none`: 不处理

**示例：**
```xml
<!-- 压缩 CSS -->
cssProcessor="[default:min]"

<!-- 不处理 -->
cssProcessor="[none]"
```

---

## 高级配置

### 完整配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base,myapp.theme]"
          allowProxy="{Boolean}true"
          embed="[myapp.components.card]"
          channels="[mobile,tablet,desktop]"
          jsProcessor="[default:min:gcc]"
          cssProcessor="[default:min]"/>
```

### 条件配置

**根据环境配置：**
```xml
<!-- 开发环境 -->
allowProxy="{Boolean}false"
jsProcessor="[none]"
cssProcessor="[none]"

<!-- 生产环境 -->
allowProxy="{Boolean}true"
jsProcessor="[default:min:gcc]"
cssProcessor="[default:min]"
```

### 多 categories 配置

```xml
categories="[myapp.components.hero,myapp.components.base,myapp.theme]"
```

**使用场景：**
- 一个 ClientLib 可以被多个 categories 引用
- 简化配置管理
- 减少重复配置

---

## 配置文件示例

### 示例 1: 基础组件 ClientLib

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          allowProxy="{Boolean}true"/>
```

**目录结构：**
```
/apps/myapp/clientlibs/components/hero/
  ├── .content.xml
  └── css/
      └── hero.css
```

### 示例 2: 带依赖的 ClientLib

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base,myapp.theme]"
          allowProxy="{Boolean}true"/>
```

**依赖关系：**
```
hero → base
hero → theme
```

**加载顺序：**
1. myapp.base
2. myapp.theme
3. myapp.components.hero

### 示例 3: 嵌入其他 ClientLibs

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base]"
          embed="[myapp.components.card,myapp.components.button]"
          allowProxy="{Boolean}true"/>
```

**效果：**
- 加载 `myapp.base`（作为依赖）
- 合并 `myapp.components.card` 和 `myapp.components.button` 的内容
- 加载 `myapp.components.hero` 的内容
- 最终生成一个合并的 CSS 文件

### 示例 4: 带压缩的 ClientLib

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          allowProxy="{Boolean}true"
          jsProcessor="[default:min:gcc]"
          cssProcessor="[default:min]"/>
```

**效果：**
- CSS 文件会被压缩
- JavaScript 文件会被压缩并使用 Google Closure Compiler 优化

### 示例 5: 多渠道 ClientLib

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          channels="[mobile,tablet,desktop]"
          allowProxy="{Boolean}true"/>
```

**目录结构：**
```
/apps/myapp/clientlibs/components/hero/
  ├── .content.xml
  └── css/
      ├── hero-mobile.css
      ├── hero-tablet.css
      └── hero-desktop.css
```

---

## 常见配置模式

### 模式 1: 基础样式库

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.base]"
          allowProxy="{Boolean}true"
          cssProcessor="[default:min]"/>
```

**特点：**
- 没有依赖（基础库）
- 通常被其他 ClientLibs 依赖
- 包含重置样式、工具类等

### 模式 2: 组件样式库

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.hero]"
          dependencies="[myapp.base]"
          allowProxy="{Boolean}true"/>
```

**特点：**
- 依赖基础样式库
- 包含组件特定的样式
- 通常只被组件模板引用

### 模式 3: 主题样式库

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.theme]"
          dependencies="[myapp.base]"
          allowProxy="{Boolean}true"/>
```

**特点：**
- 依赖基础样式库
- 包含主题相关的样式（颜色、字体等）
- 可以被多个组件共享

### 模式 4: 第三方库

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.vendor.jquery]"
          allowProxy="{Boolean}true"/>
```

**特点：**
- 通常没有依赖
- 包含第三方库的文件
- 可以被多个组件引用

### 模式 5: 合并库

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[myapp.components.all]"
          embed="[myapp.components.hero,myapp.components.card,myapp.components.button]"
          dependencies="[myapp.base]"
          allowProxy="{Boolean}true"
          cssProcessor="[default:min]"/>
```

**特点：**
- 嵌入多个组件库
- 合并成一个文件
- 减少 HTTP 请求
- 适合生产环境

---

## 配置验证

### 检查清单

**必需项：**
- [ ] `jcr:primaryType="cq:ClientLibraryFolder"`
- [ ] `categories` 属性存在且格式正确

**推荐项：**
- [ ] `allowProxy="{Boolean}true"`（生产环境）
- [ ] `dependencies` 声明了所有依赖
- [ ] `cssProcessor` 和 `jsProcessor` 配置（生产环境）

**验证方法：**
1. 检查 XML 格式是否正确
2. 验证 categories 命名是否符合规范
3. 确认依赖关系没有循环
4. 测试 ClientLib 是否能正常加载

### 常见错误

**错误 1: 缺少 jcr:primaryType**
```xml
❌ <jcr:root categories="[myapp.components.hero]"/>
✅ <jcr:root jcr:primaryType="cq:ClientLibraryFolder" categories="[myapp.components.hero]"/>
```

**错误 2: categories 格式错误**
```xml
❌ categories="myapp.components.hero"  <!-- 缺少方括号 -->
❌ categories="[myapp.components.hero"  <!-- 缺少右括号 -->
✅ categories="[myapp.components.hero]"
```

**错误 3: 循环依赖**
```xml
❌ hero → base → hero  <!-- 循环依赖 -->
✅ hero → base → theme  <!-- 线性依赖 -->
```

---

## 总结

### 关键配置属性

1. **jcr:primaryType**: 必须设置为 `cq:ClientLibraryFolder`
2. **categories**: 必需，定义标识符
3. **dependencies**: 可选，声明依赖关系
4. **allowProxy**: 可选，推荐设置为 `true`
5. **embed**: 可选，合并其他 ClientLibs
6. **cssProcessor/jsProcessor**: 可选，配置压缩处理

### 配置原则

1. **必需属性不能缺少**
2. **命名规范要统一**
3. **依赖关系要清晰**
4. **避免循环依赖**
5. **生产环境启用压缩和代理**

### 最佳实践

1. 使用有意义的 categories 命名
2. 合理声明 dependencies
3. 生产环境启用 allowProxy 和压缩
4. 使用 embed 优化性能
5. 定期检查和验证配置

