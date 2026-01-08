# AEM 组件资源收集器

## 概述

AEM 组件资源收集器是一个专门用于收集 AEM 组件完整信息的工具，旨在支持将 AEM 组件迁移到 React。它能够从 AEM 实例中提取组件的所有相关资源，包括：

- HTL/HTML 模板文件
- Java Sling Models
- CSS 样式文件
- JavaScript 文件
- 配置文件（.content.xml, _cq_dialog, _cq_editConfig 等）
- ClientLibs 依赖
- 子组件依赖
- 资源引用（图片、字体等）

## 目录结构

```
14-aem-resource-collector/
├── README.md                          # 本文档
├── 01-resource-collector-overview.md   # 资源收集器概述和原理
├── 02-component-structure-analysis.md # 组件结构深度分析
├── 03-collection-strategies.md        # 收集策略详解
├── 04-existing-tools.md               # 现有工具分析
├── 05-implementation-guide.md          # 实现指南
├── tools/
│   ├── component-resource-collector.js    # Node.js 实现
│   ├── component-resource-collector.java  # Java 实现
│   └── react-migration-generator.js       # React 迁移代码生成器
└── examples/
    └── collected-component-example.json   # 收集结果示例
```

## 快速开始

### 使用 Node.js 版本

```bash
# 安装依赖
npm install axios cheerio xml2js

# 运行收集器
node tools/component-resource-collector.js /apps/myapp/components/hero \
  --output ./collected/hero \
  --format json \
  --include-dependencies
```

### 使用 Java 版本

```bash
# 编译
javac tools/ComponentResourceCollector.java

# 运行
java ComponentResourceCollector \
  /apps/myapp/components/hero \
  --output ./collected/hero \
  --format json
```

## 核心功能

### 1. 组件文件收集

- ✅ HTL/HTML 模板文件
- ✅ JSP 文件（如果存在）
- ✅ Java Sling Model 类
- ✅ CSS 文件（通过 ClientLibs）
- ✅ JavaScript 文件（通过 ClientLibs）
- ✅ 静态资源（图片、字体等）

### 2. 配置文件收集

- ✅ `.content.xml` - 组件定义
- ✅ `_cq_dialog/.content.xml` - 编辑对话框
- ✅ `_cq_design_dialog/.content.xml` - 设计对话框
- ✅ `_cq_editConfig.xml` - 编辑配置
- ✅ `_cq_htmlTag/.content.xml` - HTML 标签配置
- ✅ `_cq_template/` - 默认内容模板
- ✅ `_cq_childEditConfig/` - 子组件编辑配置

### 3. 依赖分析

- ✅ ClientLibs 依赖（CSS/JS）
- ✅ Sling Model 依赖
- ✅ 子组件依赖
- ✅ 资源引用（图片、字体）
- ✅ 继承关系（resourceSuperType）

### 4. React 迁移支持

- ✅ 生成组件结构映射
- ✅ 提取 Props 定义（从对话框）
- ✅ 生成 TypeScript 类型定义
- ✅ 生成 React 组件骨架
- ✅ 生成样式迁移指南

## 使用场景

### 场景 1: 单个组件迁移

```bash
node tools/component-resource-collector.js \
  /apps/myapp/components/hero \
  --output ./migration/hero \
  --format react
```

### 场景 2: 批量组件迁移

```bash
# 收集多个组件
for component in hero card banner; do
  node tools/component-resource-collector.js \
    /apps/myapp/components/$component \
    --output ./migration/$component
done
```

### 场景 3: 完整依赖收集

```bash
node tools/component-resource-collector.js \
  /apps/myapp/components/product-list \
  --output ./migration/product-list \
  --include-dependencies \
  --recursive \
  --depth 3
```

## 输出格式

### JSON 格式

```json
{
  "component": {
    "path": "/apps/myapp/components/hero",
    "resourceType": "myapp/components/hero",
    "title": "Hero Component",
    "description": "Hero banner component",
    "resourceSuperType": "myapp/components/base"
  },
  "files": {
    "html": [...],
    "java": [...],
    "css": [...],
    "js": [...],
    "assets": [...]
  },
  "configurations": {
    "contentXml": {...},
    "dialog": {...},
    "editConfig": {...}
  },
  "dependencies": {
    "clientlibs": [...],
    "slingModels": [...],
    "childComponents": [...]
  },
  "reactMigration": {
    "componentPath": "src/components/hero/Hero.tsx",
    "props": {...},
    "styles": {...}
  }
}
```

### React 格式

生成可直接使用的 React 组件代码和配置文件。

## 文档导航

1. **[资源收集器概述](./01-resource-collector-overview.md)** - 了解资源收集器的原理和架构
2. **[组件结构分析](./02-component-structure-analysis.md)** - 深度分析 AEM 组件的完整结构
3. **[收集策略详解](./03-collection-strategies.md)** - 了解不同的收集策略和最佳实践
4. **[现有工具分析](./04-existing-tools.md)** - 分析现有工具和解决方案
5. **[实现指南](./05-implementation-guide.md)** - 详细的实现指南和代码示例

## 相关文档

- [组件依赖解析](../04-advanced/04-component-dependency-resolution.md)
- [组件结构详解](../02-components/04-component-content-xml.html)
- [CSS 管理机制](../13-aem-css/README.md)

