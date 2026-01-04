# AEM 组件依赖分析工具

## 概述

这些工具用于分析 AEM 组件的完整依赖信息，特别适用于将 AEM 组件迁移到 React 的场景。

## 工具列表

### component-dependency-analyzer.js

主要的组件依赖分析工具，可以提取组件的所有依赖信息。

## 安装和使用

### 前置要求

- Node.js 12+
- 访问 AEM 实例的权限

### 基本使用

```bash
# 分析单个组件
node component-dependency-analyzer.js /apps/myapp/components/card

# 指定输出文件
node component-dependency-analyzer.js /apps/myapp/components/card --output card-deps.json

# 生成 React 迁移报告
node component-dependency-analyzer.js /apps/myapp/components/card --format react --output react-migration.json

# 递归分析子组件
node component-dependency-analyzer.js /apps/myapp/components/card --recursive
```

### 环境变量配置

```bash
# 设置 AEM 连接信息
export AEM_HOST=http://localhost:4502
export AEM_USER=admin
export AEM_PASS=admin

# 然后运行分析
node component-dependency-analyzer.js /apps/myapp/components/card
```

## 输出格式

### JSON 格式

标准 JSON 输出包含以下信息：

```json
{
  "component": {
    "resourceType": "/apps/myapp/components/card",
    "resourceSuperType": "/apps/myapp/components/base",
    "path": "/apps/myapp/components/card",
    "template": {
      "path": "/apps/myapp/components/card/card.html",
      "type": "HTL",
      "content": "..."
    }
  },
  "clientlibs": {
    "categories": ["myapp.components.card", "myapp.base"],
    "css": [
      {
        "category": "myapp.components.card",
        "files": ["/apps/myapp/clientlibs/components/card/css/card.css"],
        "path": "/apps/myapp/clientlibs/components/card"
      }
    ],
    "js": [],
    "dependencies": {
      "myapp.components.card": ["myapp.base"]
    }
  },
  "slingModels": ["com.myapp.models.CardModel"],
  "childComponents": [
    {
      "path": "content",
      "resourceType": "foundation/components/parsys"
    }
  ],
  "dialog": { ... },
  "properties": { ... }
}
```

### React 迁移格式

使用 `--format react` 选项时，输出针对 React 迁移优化的格式：

```json
{
  "component": "/apps/myapp/components/card",
  "reactStructure": {
    "componentPath": "src/components/myapp/components/card",
    "stylesPath": "src/components/myapp/components/card/Component.module.css",
    "modelPath": "src/models/CardModel.js",
    "assetsPath": "src/components/myapp/components/card/assets"
  },
  "dependencies": {
    "cssFiles": [...],
    "jsFiles": [...],
    "models": [...],
    "childComponents": [...]
  },
  "migrationSteps": [
    "1. 创建 React 组件目录结构",
    "2. 迁移样式文件 (CSS)",
    ...
  ]
}
```

## 集成到工作流

### 1. 批量分析多个组件

创建脚本 `analyze-all-components.sh`:

```bash
#!/bin/bash

COMPONENTS=(
  "/apps/myapp/components/card"
  "/apps/myapp/components/header"
  "/apps/myapp/components/footer"
)

for component in "${COMPONENTS[@]}"; do
  component_name=$(basename "$component")
  node component-dependency-analyzer.js "$component" \
    --output "deps/${component_name}-deps.json"
done
```

### 2. 生成依赖关系图

使用输出的 JSON 文件生成可视化依赖图：

```javascript
// generate-dependency-graph.js
const fs = require('fs');
const deps = JSON.parse(fs.readFileSync('card-deps.json', 'utf8'));

// 使用 graphviz 或其他工具生成图表
console.log('digraph G {');
deps.clientlibs.dependencies.forEach((category, deps) => {
  console.log(`  "${category}" -> "${deps.join('", "')}"`);
});
console.log('}');
```

### 3. 导出资源文件

基于分析结果导出实际的 CSS、JS 文件：

```bash
# 导出 CSS 文件
for category in $(jq -r '.clientlibs.categories[]' card-deps.json); do
  curl -u admin:admin \
    "http://localhost:4502/etc.clientlibs/myapp/clientlibs/${category}.css" \
    -o "exported-styles/${category}.css"
done
```

## 注意事项

1. **权限**: 确保有足够的权限访问 AEM 的 JCR 节点
2. **性能**: 递归分析大型组件树可能需要较长时间
3. **网络**: 工具需要通过 HTTP 访问 AEM，确保网络连接正常
4. **版本兼容**: 工具针对 AEM 6.5+ 设计，旧版本可能需要调整

## 扩展和自定义

工具可以轻松扩展以支持：

- 其他输出格式（如 Markdown、HTML）
- 更多的依赖类型分析
- 与 CI/CD 集成
- 自动生成迁移代码

## 故障排除

### 无法连接到 AEM

检查：
- AEM 实例是否运行
- 主机和端口是否正确
- 防火墙设置
- 认证信息是否正确

### 无法找到组件

检查：
- 组件路径是否正确
- 是否有访问权限
- 组件是否已部署

### ClientLib 分析失败

某些 ClientLib 可能通过其他方式加载（如页面级别），需要手动补充到分析结果中。

## 相关文档

- [组件依赖解析机制详解](../04-component-dependency-resolution.md)
- [React 迁移最佳实践](../../08-migration/react-migration-guide.md)（如果存在）

