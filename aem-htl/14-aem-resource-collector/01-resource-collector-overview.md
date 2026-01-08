# AEM 组件资源收集器概述

## 目录
1. [为什么需要资源收集器](#为什么需要资源收集器)
2. [资源收集器的目标](#资源收集器的目标)
3. [AEM 组件的完整信息结构](#aem-组件的完整信息结构)
4. [收集器的工作原理](#收集器的工作原理)
5. [收集流程](#收集流程)
6. [输出格式](#输出格式)
7. [使用场景](#使用场景)

---

## 为什么需要资源收集器

### 问题：AEM 组件信息分散

AEM 组件的信息分散在多个位置：

```
/apps/myapp/components/hero/
  ├── .content.xml              # 组件定义
  ├── hero.html                 # HTL 模板
  ├── _cq_dialog/               # 对话框配置
  ├── _cq_editConfig.xml        # 编辑配置
  └── ...

/apps/myapp/clientlibs/components/hero/
  ├── .content.xml              # ClientLib 配置
  ├── css/
  │   └── hero.css              # 样式文件
  └── js/
      └── hero.js               # JavaScript 文件

/apps/myapp/core/models/
  └── HeroModel.java            # Sling Model

/content/myapp/pages/example/
  └── jcr:content/
      └── hero/                 # 组件实例
          └── jcr:content      # 组件内容属性
```

### 挑战

1. **信息分散**: 组件定义、模板、样式、逻辑分散在不同位置
2. **依赖复杂**: 需要追踪 ClientLibs、Sling Models、子组件等依赖
3. **配置多样**: 多种配置文件（dialog、editConfig、htmlTag 等）
4. **手动收集困难**: 手动收集容易遗漏，效率低下

### 解决方案：自动化资源收集器

资源收集器能够：
- ✅ 自动发现和收集所有相关文件
- ✅ 分析依赖关系
- ✅ 提取配置信息
- ✅ 生成 React 迁移所需的结构化数据

---

## 资源收集器的目标

### 核心目标

1. **完整性**: 收集组件的所有相关信息，不遗漏任何文件
2. **准确性**: 准确识别文件类型、依赖关系、配置信息
3. **结构化**: 输出结构化的数据，便于后续处理
4. **可扩展**: 支持自定义收集规则和输出格式

### 具体目标

#### 1. 文件收集

- ✅ 收集所有模板文件（HTL、JSP）
- ✅ 收集所有 Java 类（Sling Models）
- ✅ 收集所有样式文件（CSS、SCSS）
- ✅ 收集所有脚本文件（JavaScript、TypeScript）
- ✅ 收集所有静态资源（图片、字体、图标）

#### 2. 配置收集

- ✅ 解析 `.content.xml` 获取组件定义
- ✅ 解析 `_cq_dialog` 获取对话框配置
- ✅ 解析 `_cq_editConfig` 获取编辑配置
- ✅ 解析 `_cq_htmlTag` 获取 HTML 标签配置
- ✅ 解析 `_cq_template` 获取默认内容

#### 3. 依赖分析

- ✅ 分析 ClientLibs 依赖（CSS/JS）
- ✅ 分析 Sling Model 依赖
- ✅ 分析子组件依赖
- ✅ 分析继承关系（resourceSuperType）
- ✅ 分析资源引用（图片、字体）

#### 4. React 迁移支持

- ✅ 生成组件 Props 定义（从对话框）
- ✅ 生成 TypeScript 类型定义
- ✅ 生成 React 组件骨架
- ✅ 生成样式迁移指南
- ✅ 生成依赖映射

---

## AEM 组件的完整信息结构

### 组件文件结构

```
/apps/myapp/components/hero/
├── .content.xml                    # 组件定义（必需）
│   ├── jcr:primaryType: cq:Component
│   ├── jcr:title: "Hero Component"
│   ├── sling:resourceSuperType: "myapp/components/base"
│   └── componentGroup: "MyApp - Content"
│
├── hero.html                        # HTL 模板（必需）
│   ├── HTL 代码
│   ├── data-sly-use 引用
│   ├── data-sly-resource 引用
│   └── clientlib 引用
│
├── _cq_dialog/                     # 编辑对话框（可选）
│   └── .content.xml
│       ├── 字段定义
│       ├── 验证规则
│       └── 字段类型
│
├── _cq_design_dialog/              # 设计对话框（可选）
│   └── .content.xml
│
├── _cq_editConfig.xml              # 编辑配置（可选）
│   ├── cq:dialogMode
│   ├── cq:dropTargets
│   └── cq:listeners
│
├── _cq_htmlTag/                    # HTML 标签配置（可选）
│   └── .content.xml
│       ├── tagName
│       └── classNames
│
├── _cq_template/                   # 默认内容模板（可选）
│   └── .content.xml
│       └── jcr:content
│
└── _cq_childEditConfig/            # 子组件编辑配置（可选）
    └── .content.xml
```

### 相关资源位置

#### 1. ClientLibs（CSS/JS）

```
/apps/myapp/clientlibs/
├── base/
│   ├── .content.xml
│   ├── css/
│   │   └── base.css
│   └── js/
│       └── base.js
│
└── components/
    └── hero/
        ├── .content.xml
        ├── css/
        │   └── hero.css
        └── js/
            └── hero.js
```

#### 2. Sling Models（Java）

```
/apps/myapp/core/
└── src/
    └── main/
        └── java/
            └── com/
                └── myapp/
                    └── core/
                        └── models/
                            └── HeroModel.java
```

#### 3. 静态资源

```
/apps/myapp/components/hero/
└── images/
    ├── hero-bg.jpg
    └── hero-icon.svg
```

### 组件信息分类

#### 1. 核心文件（必需）

- `.content.xml` - 组件定义
- `{component-name}.html` - HTL 模板

#### 2. 配置文件（可选）

- `_cq_dialog/` - 编辑对话框
- `_cq_design_dialog/` - 设计对话框
- `_cq_editConfig.xml` - 编辑配置
- `_cq_htmlTag/` - HTML 标签配置
- `_cq_template/` - 默认内容模板
- `_cq_childEditConfig/` - 子组件编辑配置

#### 3. 样式和脚本

- ClientLibs CSS 文件
- ClientLibs JavaScript 文件
- 内联样式（如果有）

#### 4. 业务逻辑

- Sling Model Java 类
- Use API 脚本（如果有）

#### 5. 静态资源

- 图片文件
- 字体文件
- 图标文件

#### 6. 依赖关系

- ClientLibs 依赖
- Sling Model 依赖
- 子组件依赖
- 继承关系

---

## 收集器的工作原理

### 架构设计

```
输入: 组件路径 (/apps/myapp/components/hero)
  ↓
1. 组件基本信息收集
   ├── 读取 .content.xml
   ├── 解析组件属性
   └── 识别组件类型
  ↓
2. 文件收集
   ├── HTL/HTML 模板
   ├── Java 类（Sling Models）
   ├── CSS 文件（通过 ClientLibs）
   ├── JavaScript 文件（通过 ClientLibs）
   └── 静态资源
  ↓
3. 配置收集
   ├── _cq_dialog
   ├── _cq_editConfig
   ├── _cq_htmlTag
   └── _cq_template
  ↓
4. 依赖分析
   ├── ClientLibs 依赖
   ├── Sling Model 依赖
   ├── 子组件依赖
   └── 继承关系
  ↓
5. 数据整合
   ├── 结构化数据
   ├── 依赖图
   └── 迁移映射
  ↓
输出: 结构化 JSON / React 代码
```

### 核心模块

#### 1. 组件发现器（Component Discoverer）

**功能**: 发现组件的所有文件

```javascript
class ComponentDiscoverer {
  discoverFiles(componentPath) {
    return {
      html: this.findHTLTemplates(componentPath),
      java: this.findJavaClasses(componentPath),
      configs: this.findConfigFiles(componentPath),
      assets: this.findAssets(componentPath)
    };
  }
}
```

#### 2. 配置解析器（Config Parser）

**功能**: 解析各种配置文件

```javascript
class ConfigParser {
  parseContentXml(path) { /* 解析 .content.xml */ }
  parseDialog(path) { /* 解析 _cq_dialog */ }
  parseEditConfig(path) { /* 解析 _cq_editConfig */ }
  parseHtmlTag(path) { /* 解析 _cq_htmlTag */ }
}
```

#### 3. 依赖分析器（Dependency Analyzer）

**功能**: 分析组件的依赖关系

```javascript
class DependencyAnalyzer {
  analyzeClientLibs(component) { /* 分析 ClientLibs 依赖 */ }
  analyzeSlingModels(component) { /* 分析 Sling Model 依赖 */ }
  analyzeChildComponents(component) { /* 分析子组件依赖 */ }
  analyzeInheritance(component) { /* 分析继承关系 */ }
}
```

#### 4. 资源提取器（Resource Extractor）

**功能**: 从 AEM 实例提取资源

```javascript
class ResourceExtractor {
  extractFile(path) { /* 提取文件内容 */ }
  extractJSON(path) { /* 提取 JSON 数据 */ }
  extractBinary(path) { /* 提取二进制文件 */ }
}
```

#### 5. React 生成器（React Generator）

**功能**: 生成 React 迁移代码

```javascript
class ReactGenerator {
  generateComponent(data) { /* 生成 React 组件 */ }
  generateTypes(data) { /* 生成 TypeScript 类型 */ }
  generateStyles(data) { /* 生成样式文件 */ }
}
```

---

## 收集流程

### 详细流程步骤

#### 步骤 1: 初始化

```javascript
// 1. 验证组件路径
const componentPath = '/apps/myapp/components/hero';
validateComponentPath(componentPath);

// 2. 连接 AEM 实例
const aemClient = new AEMClient({
  host: 'http://localhost:4502',
  username: 'admin',
  password: 'admin'
});
```

#### 步骤 2: 收集组件基本信息

```javascript
// 1. 读取 .content.xml
const contentXml = await aemClient.getJSON(`${componentPath}.json`);

// 2. 提取关键属性
const componentInfo = {
  path: componentPath,
  resourceType: contentXml['sling:resourceType'],
  title: contentXml['jcr:title'],
  description: contentXml['jcr:description'],
  resourceSuperType: contentXml['sling:resourceSuperType'],
  componentGroup: contentXml['componentGroup']
};
```

#### 步骤 3: 收集模板文件

```javascript
// 1. 查找 HTL 模板
const componentName = path.basename(componentPath);
const possibleTemplates = [
  `${componentPath}/${componentName}.html`,
  `${componentPath}/template.html`,
  `${componentPath}/${componentName}.jsp`
];

for (const templatePath of possibleTemplates) {
  try {
    const content = await aemClient.getText(templatePath);
    if (content) {
      templates.push({
        path: templatePath,
        type: templatePath.endsWith('.html') ? 'HTL' : 'JSP',
        content: content
      });
      break;
    }
  } catch (e) {
    // 继续尝试下一个
  }
}
```

#### 步骤 4: 收集配置文件

```javascript
// 1. 收集所有配置文件
const configs = {
  contentXml: await collectContentXml(componentPath),
  dialog: await collectDialog(componentPath),
  editConfig: await collectEditConfig(componentPath),
  htmlTag: await collectHtmlTag(componentPath),
  template: await collectTemplate(componentPath)
};

async function collectDialog(componentPath) {
  try {
    const dialogPath = `${componentPath}/_cq_dialog`;
    return await aemClient.getJSON(`${dialogPath}.json`);
  } catch (e) {
    return null; // 对话框可能不存在
  }
}
```

#### 步骤 5: 分析 HTL 模板内容

```javascript
// 1. 解析 ClientLibs 引用
const clientLibRegex = /clientlib\.(css|js|all)\s*@\s*categories=['"]([^'"]+)['"]/g;
const clientLibs = [];
let match;
while ((match = clientLibRegex.exec(templateContent)) !== null) {
  clientLibs.push({
    type: match[1],
    categories: match[2].split(',').map(c => c.trim())
  });
}

// 2. 解析 Sling Model 引用
const modelRegex = /data-sly-use\.\w+\s*=\s*['"]([^'"]+)['"]/g;
const slingModels = [];
while ((match = modelRegex.exec(templateContent)) !== null) {
  slingModels.push(match[1]);
}

// 3. 解析子组件引用
const resourceRegex = /data-sly-resource\s*=\s*['"]([^'"]+)['"][^>]*resourceType=['"]([^'"]+)['"]/g;
const childComponents = [];
while ((match = resourceRegex.exec(templateContent)) !== null) {
  childComponents.push({
    path: match[1],
    resourceType: match[2]
  });
}
```

#### 步骤 6: 收集 ClientLibs

```javascript
// 1. 查找 ClientLibs
for (const category of clientLibCategories) {
  const clientLib = await findClientLib(category);
  if (clientLib) {
    // 2. 收集 CSS 文件
    const cssFiles = await collectClientLibFiles(clientLib.path, 'css');
    // 3. 收集 JS 文件
    const jsFiles = await collectClientLibFiles(clientLib.path, 'js');
    // 4. 收集依赖
    const dependencies = clientLib.dependencies || [];
  }
}

async function findClientLib(category) {
  // 通过 JCR 查询查找 ClientLib
  const query = `SELECT * FROM [cq:ClientLibraryFolder] WHERE [categories] = '${category}'`;
  const results = await aemClient.query(query);
  return results[0];
}
```

#### 步骤 7: 收集 Sling Models

```javascript
// 1. 查找 Java 类文件
for (const modelClass of slingModelClasses) {
  const javaFile = await findJavaClass(modelClass);
  if (javaFile) {
    // 2. 提取类内容
    const javaContent = await aemClient.getText(javaFile.path);
    // 3. 解析类结构
    const classInfo = parseJavaClass(javaContent);
  }
}

async function findJavaClass(className) {
  // 将类名转换为文件路径
  // com.myapp.core.models.HeroModel -> 
  // /apps/myapp/core/src/main/java/com/myapp/core/models/HeroModel.java
  const parts = className.split('.');
  const fileName = parts.pop();
  const packagePath = parts.join('/');
  const filePath = `/apps/myapp/core/src/main/java/${packagePath}/${fileName}.java`;
  
  try {
    const content = await aemClient.getText(filePath);
    return { path: filePath, content: content };
  } catch (e) {
    return null;
  }
}
```

#### 步骤 8: 收集静态资源

```javascript
// 1. 从 HTL 模板中提取资源引用
const imageRegex = /src=['"]([^'"]+\.(jpg|png|gif|svg))['"]/gi;
const images = [];
let match;
while ((match = imageRegex.exec(templateContent)) !== null) {
  images.push(match[1]);
}

// 2. 下载资源文件
for (const imagePath of images) {
  const imageData = await aemClient.getBinary(imagePath);
  await saveFile(`./collected/assets/${path.basename(imagePath)}`, imageData);
}
```

#### 步骤 9: 生成输出

```javascript
// 1. 整合所有数据
const collectedData = {
  component: componentInfo,
  files: {
    html: templates,
    java: javaFiles,
    css: cssFiles,
    js: jsFiles,
    assets: assetFiles
  },
  configurations: configs,
  dependencies: {
    clientlibs: clientLibs,
    slingModels: slingModels,
    childComponents: childComponents
  }
};

// 2. 生成输出
if (outputFormat === 'json') {
  await writeJSON(outputPath, collectedData);
} else if (outputFormat === 'react') {
  await generateReactCode(collectedData);
}
```

---

## 输出格式

### JSON 格式示例

```json
{
  "component": {
    "path": "/apps/myapp/components/hero",
    "resourceType": "myapp/components/hero",
    "title": "Hero Component",
    "description": "Hero banner component with image and text",
    "resourceSuperType": "myapp/components/base",
    "componentGroup": "MyApp - Content"
  },
  "files": {
    "html": [
      {
        "path": "/apps/myapp/components/hero/hero.html",
        "type": "HTL",
        "content": "<div class=\"hero\">...</div>"
      }
    ],
    "java": [
      {
        "path": "/apps/myapp/core/src/main/java/com/myapp/core/models/HeroModel.java",
        "className": "com.myapp.core.models.HeroModel",
        "content": "package com.myapp.core.models;..."
      }
    ],
    "css": [
      {
        "path": "/apps/myapp/clientlibs/components/hero/css/hero.css",
        "category": "myapp.components.hero",
        "content": ".hero { ... }"
      }
    ],
    "js": [
      {
        "path": "/apps/myapp/clientlibs/components/hero/js/hero.js",
        "category": "myapp.components.hero",
        "content": "document.addEventListener('DOMContentLoaded', ...)"
      }
    ],
    "assets": [
      {
        "path": "/apps/myapp/components/hero/images/hero-bg.jpg",
        "type": "image",
        "size": 123456
      }
    ]
  },
  "configurations": {
    "contentXml": {
      "jcr:primaryType": "cq:Component",
      "jcr:title": "Hero Component",
      "sling:resourceSuperType": "myapp/components/base"
    },
    "dialog": {
      "jcr:primaryType": "nt:unstructured",
      "items": {
        "tabs": {
          "items": {
            "properties": {
              "items": {
                "title": {
                  "fieldLabel": "Title",
                  "fieldDescription": "Hero title",
                  "name": "./title",
                  "sling:resourceType": "granite/ui/components/coral/foundation/form/textfield"
                }
              }
            }
          }
        }
      }
    },
    "editConfig": {
      "cq:dialogMode": "floating",
      "cq:dropTargets": {
        "image": {
          "groups": ["media"],
          "propertyName": "./image"
        }
      }
    }
  },
  "dependencies": {
    "clientlibs": [
      {
        "category": "myapp.components.hero",
        "path": "/apps/myapp/clientlibs/components/hero",
        "cssFiles": ["hero.css"],
        "jsFiles": ["hero.js"],
        "dependencies": ["myapp.base"]
      }
    ],
    "slingModels": [
      {
        "className": "com.myapp.core.models.HeroModel",
        "path": "/apps/myapp/core/src/main/java/com/myapp/core/models/HeroModel.java"
      }
    ],
    "childComponents": [
      {
        "resourceType": "myapp/components/button",
        "path": "./button"
      }
    ]
  },
  "reactMigration": {
    "componentPath": "src/components/hero/Hero.tsx",
    "props": {
      "title": {
        "type": "string",
        "required": false,
        "description": "Hero title"
      },
      "subtitle": {
        "type": "string",
        "required": false,
        "description": "Hero subtitle"
      }
    },
    "styles": {
      "cssPath": "src/components/hero/Hero.module.css",
      "cssContent": ".hero { ... }"
    }
  }
}
```

---

## 使用场景

### 场景 1: 单个组件迁移

**目标**: 将单个 AEM 组件迁移到 React

```bash
node component-resource-collector.js \
  /apps/myapp/components/hero \
  --output ./migration/hero \
  --format react
```

**输出**:
- React 组件代码
- TypeScript 类型定义
- 样式文件
- Props 定义

### 场景 2: 批量组件迁移

**目标**: 迁移多个相关组件

```bash
# 收集所有组件
for component in hero card banner; do
  node component-resource-collector.js \
    /apps/myapp/components/$component \
    --output ./migration/$component
done

# 生成依赖关系图
node generate-dependency-graph.js ./migration
```

### 场景 3: 完整依赖收集

**目标**: 收集组件及其所有依赖

```bash
node component-resource-collector.js \
  /apps/myapp/components/product-list \
  --output ./migration/product-list \
  --include-dependencies \
  --recursive \
  --depth 3
```

### 场景 4: 分析和文档生成

**目标**: 生成组件文档和分析报告

```bash
node component-resource-collector.js \
  /apps/myapp/components/hero \
  --output ./analysis/hero \
  --format json \
  --generate-docs
```

---

## 总结

### 核心要点

1. **资源收集器的作用**: 自动化收集 AEM 组件的所有相关信息
2. **收集内容**: 文件、配置、依赖、资源
3. **输出格式**: JSON、React 代码、文档
4. **使用场景**: 单个组件迁移、批量迁移、依赖分析、文档生成

### 下一步

- 阅读 [组件结构分析](./02-component-structure-analysis.md) 了解组件的完整结构
- 阅读 [收集策略详解](./03-collection-strategies.md) 了解不同的收集策略
- 阅读 [现有工具分析](./04-existing-tools.md) 了解现有工具和解决方案
- 阅读 [实现指南](./05-implementation-guide.md) 开始实现自己的收集器

