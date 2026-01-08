# AEM 组件结构深度分析

## 目录
1. [组件节点结构](#组件节点结构)
2. [文件类型详解](#文件类型详解)
3. [配置文件详解](#配置文件详解)
4. [依赖关系分析](#依赖关系分析)
5. [资源引用分析](#资源引用分析)
6. [收集策略](#收集策略)

---

## 组件节点结构

### 标准组件结构

```
/apps/myapp/components/hero/
├── .content.xml                    # 组件定义（必需）
├── hero.html                       # HTL 模板（必需）
├── _cq_dialog/                     # 编辑对话框（可选）
│   └── .content.xml
├── _cq_design_dialog/              # 设计对话框（可选）
│   └── .content.xml
├── _cq_editConfig.xml              # 编辑配置（可选）
├── _cq_htmlTag/                    # HTML 标签配置（可选）
│   └── .content.xml
├── _cq_template/                   # 默认内容模板（可选）
│   └── .content.xml
├── _cq_childEditConfig/            # 子组件编辑配置（可选）
│   └── .content.xml
└── [其他文件]                      # 自定义文件
```

### 组件节点属性（.content.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          jcr:primaryType="cq:Component"
          
          <!-- 基本信息 -->
          jcr:title="Hero Component"
          jcr:description="Hero banner component"
          
          <!-- 资源类型 -->
          sling:resourceType="myapp/components/hero"
          sling:resourceSuperType="myapp/components/base"
          
          <!-- 组件分组 -->
          componentGroup="MyApp - Content"
          
          <!-- 容器属性 -->
          cq:isContainer="{Boolean}false"
          
          <!-- 使用限制 -->
          allowParents="[myapp/components/container]"
          allowedChildren="[myapp/components/button]"
          
          <!-- 其他属性 -->
          cq:noDecoration="{Boolean}false"
          cq:emptyText="Configure Hero Component"
          cq:inherit="{Boolean}false"
/>
```

### 关键属性说明

| 属性 | 类型 | 必需 | 说明 | React 迁移映射 |
|------|------|------|------|---------------|
| `jcr:primaryType` | String | ✅ | 必须为 `cq:Component` | 组件类型标识 |
| `jcr:title` | String | ❌ | 组件显示名称 | 组件文档名称 |
| `jcr:description` | String | ❌ | 组件描述 | 组件文档说明 |
| `sling:resourceType` | String | ✅ | 组件资源类型（通常等于路径） | React 组件路径 |
| `sling:resourceSuperType` | String | ❌ | 父组件路径（继承） | React 组件继承/组合 |
| `componentGroup` | String | ❌ | 组件分组 | React 组件分类 |
| `cq:isContainer` | Boolean | ❌ | 是否为容器组件 | 组件类型标识 |
| `allowParents` | String[] | ❌ | 允许的父组件 | 组件使用限制 |
| `allowedChildren` | String[] | ❌ | 允许的子组件 | 组件组合限制 |

---

## 文件类型详解

### 1. HTL 模板文件

#### 文件位置和命名

```
/apps/myapp/components/hero/
└── hero.html          # 标准命名：{component-name}.html
```

**可能的文件名**:
- `{component-name}.html` - 标准命名
- `template.html` - 替代命名
- `{component-name}.jsp` - JSP 模板（旧版本）

#### HTL 模板内容分析

**需要提取的信息**:

1. **ClientLibs 引用**
```html
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.hero'}"></sly>
<sly data-sly-call="${clientlib.js @ categories='myapp.components.hero'}"></sly>
```

2. **Sling Model 引用**
```html
<sly data-sly-use.model="com.myapp.core.models.HeroModel"></sly>
```

3. **子组件引用**
```html
<sly data-sly-resource="${'./button' @ resourceType='myapp/components/button'}"></sly>
```

4. **资源引用**
```html
<img src="${properties.imageReference}" alt="${properties.altText}">
```

5. **模板调用**
```html
<sly data-sly-call="${templates.card @ title=properties.title}"></sly>
```

### 2. Java Sling Model 文件

#### 文件位置

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

#### Sling Model 内容分析

**需要提取的信息**:

1. **类定义**
```java
@Model(adaptables = Resource.class)
public interface HeroModel {
    // ...
}
```

2. **属性注入**
```java
@Inject
String getTitle();

@Inject @Optional
String getSubtitle();

@Inject @Named("jcr:title")
String getPageTitle();
```

3. **方法定义**
```java
@PostConstruct
void init() {
    // 初始化逻辑
}

String getFormattedDate();
```

4. **依赖注入**
```java
@Inject
ResourceResolver resourceResolver;

@Inject
SlingHttpServletRequest request;

@OSGiService
MyService myService;
```

### 3. CSS 样式文件

#### 文件位置

```
/apps/myapp/clientlibs/
└── components/
    └── hero/
        ├── .content.xml
        └── css/
            └── hero.css
```

#### ClientLib 配置分析

**需要提取的信息**:

1. **Categories**
```xml
categories="[myapp.components.hero]"
```

2. **Dependencies**
```xml
dependencies="[myapp.base]"
```

3. **CSS 文件列表**
```xml
<!-- 通过 .content.xml 或文件系统获取 -->
```

### 4. JavaScript 文件

#### 文件位置

```
/apps/myapp/clientlibs/
└── components/
    └── hero/
        └── js/
            └── hero.js
```

#### JavaScript 内容分析

**需要提取的信息**:

1. **模块导出**
```javascript
export default class Hero {
    // ...
}
```

2. **依赖导入**
```javascript
import { Component } from './base';
```

3. **事件监听**
```javascript
document.addEventListener('DOMContentLoaded', () => {
    // ...
});
```

### 5. 静态资源文件

#### 文件位置

```
/apps/myapp/components/hero/
└── images/
    ├── hero-bg.jpg
    └── hero-icon.svg
```

**资源类型**:
- 图片文件（jpg, png, gif, svg, webp）
- 字体文件（woff, woff2, ttf, otf）
- 图标文件（svg, ico）
- 其他二进制文件

---

## 配置文件详解

### 1. .content.xml（组件定义）

**位置**: `/apps/myapp/components/hero/.content.xml`

**内容结构**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          jcr:primaryType="cq:Component"
          jcr:title="Hero Component"
          sling:resourceType="myapp/components/hero"
          sling:resourceSuperType="myapp/components/base"
          componentGroup="MyApp - Content"
          cq:isContainer="{Boolean}false"
          allowParents="[myapp/components/container]"
          allowedChildren="[myapp/components/button]"/>
```

**收集方法**:
```javascript
// 通过 JSON API 获取
const contentXml = await aemClient.getJSON(`${componentPath}.json`);

// 或直接读取 XML 文件
const contentXml = await aemClient.getText(`${componentPath}/.content.xml`);
```

### 2. _cq_dialog（编辑对话框）

**位置**: `/apps/myapp/components/hero/_cq_dialog/.content.xml`

**内容结构**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
          xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          jcr:primaryType="nt:unstructured"
          jcr:title="Hero"
          sling:resourceType="cq/gui/components/authoring/dialog">
  <content>
    <items>
      <tabs>
        <items>
          <properties>
            <items>
              <title>
                <fieldLabel>Title</fieldLabel>
                <name>./title</name>
                <sling:resourceType="granite/ui/components/coral/foundation/form/textfield"/>
              </title>
              <subtitle>
                <fieldLabel>Subtitle</fieldLabel>
                <name>./subtitle</name>
                <sling:resourceType="granite/ui/components/coral/foundation/form/textfield"/>
              </subtitle>
              <image>
                <fieldLabel>Image</fieldLabel>
                <name>./imageReference</name>
                <sling:resourceType="granite/ui/components/coral/foundation/form/pathfield"/>
              </image>
            </items>
          </properties>
        </items>
      </tabs>
    </items>
  </content>
</jcr:root>
```

**收集方法**:
```javascript
// 通过 JSON API 获取
const dialog = await aemClient.getJSON(`${componentPath}/_cq_dialog.json`);

// 提取字段定义
const fields = extractDialogFields(dialog);
```

**字段提取**:
```javascript
function extractDialogFields(dialog) {
  const fields = [];
  
  function traverse(node, path = '') {
    if (node['sling:resourceType']) {
      // 这是一个字段节点
      fields.push({
        name: node.name || path,
        label: node.fieldLabel,
        type: node['sling:resourceType'],
        required: node.required === true,
        description: node.fieldDescription
      });
    }
    
    // 递归遍历子节点
    if (node.items) {
      Object.keys(node.items).forEach(key => {
        traverse(node.items[key], path ? `${path}.${key}` : key);
      });
    }
  }
  
  traverse(dialog);
  return fields;
}
```

### 3. _cq_editConfig（编辑配置）

**位置**: `/apps/myapp/components/hero/_cq_editConfig.xml`

**内容结构**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:EditConfig">
  <cq:dialogMode>floating</cq:dialogMode>
  <cq:dropTargets jcr:primaryType="nt:unstructured">
    <image jcr:primaryType="cq:DropTargetConfig"
           groups="[media]"
           propertyName="./imageReference"/>
  </cq:dropTargets>
  <cq:listeners jcr:primaryType="nt:unstructured"
                afteredit="REFRESH_PAGE"
                afterinsert="REFRESH_PAGE"/>
</jcr:root>
```

**收集方法**:
```javascript
const editConfig = await aemClient.getJSON(`${componentPath}/_cq_editConfig.json`);
```

**提取信息**:
- `cq:dialogMode` - 对话框模式（floating, auto）
- `cq:dropTargets` - 拖放目标配置
- `cq:listeners` - 事件监听器配置

### 4. _cq_htmlTag（HTML 标签配置）

**位置**: `/apps/myapp/components/hero/_cq_htmlTag/.content.xml`

**内容结构**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="nt:unstructured"
          tagName="section"
          classNames="hero-component"
          id="${component.id}"/>
```

**收集方法**:
```javascript
const htmlTag = await aemClient.getJSON(`${componentPath}/_cq_htmlTag.json`);
```

**提取信息**:
- `tagName` - HTML 标签名
- `classNames` - CSS 类名
- `id` - HTML ID
- 其他属性

### 5. _cq_template（默认内容模板）

**位置**: `/apps/myapp/components/hero/_cq_template/.content.xml`

**内容结构**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
          jcr:primaryType="nt:unstructured">
  <jcr:content jcr:primaryType="nt:unstructured"
               title="Default Hero Title"
               subtitle="Default Hero Subtitle"
               imageReference="/content/dam/myapp/default-hero.jpg"/>
</jcr:root>
```

**收集方法**:
```javascript
const template = await aemClient.getJSON(`${componentPath}/_cq_template.json`);
```

**提取信息**:
- 默认属性值
- 默认子节点结构

---

## 依赖关系分析

### 1. ClientLibs 依赖

#### 查找方法

**方法 1: 从 HTL 模板提取**
```javascript
// 解析 HTL 模板中的 clientlib 引用
const clientLibRegex = /clientlib\.(css|js|all)\s*@\s*categories=['"]([^'"]+)['"]/g;
const categories = [];
let match;
while ((match = clientLibRegex.exec(htlContent)) !== null) {
  categories.push(...match[2].split(',').map(c => c.trim()));
}
```

**方法 2: 通过 JCR 查询**
```javascript
// 查询 ClientLib
const query = `
  SELECT * FROM [cq:ClientLibraryFolder] 
  WHERE [categories] = 'myapp.components.hero'
`;
const results = await aemClient.query(query);
```

#### 依赖链分析

```javascript
async function analyzeClientLibDependencies(category) {
  const visited = new Set();
  const dependencies = [];
  
  async function traverse(cat) {
    if (visited.has(cat)) return;
    visited.add(cat);
    
    const clientLib = await findClientLib(cat);
    if (clientLib && clientLib.dependencies) {
      for (const dep of clientLib.dependencies) {
        dependencies.push({
          category: cat,
          dependency: dep
        });
        await traverse(dep);
      }
    }
  }
  
  await traverse(category);
  return dependencies;
}
```

### 2. Sling Model 依赖

#### 查找方法

**方法 1: 从 HTL 模板提取**
```javascript
// 解析 data-sly-use 引用
const modelRegex = /data-sly-use\.\w+\s*=\s*['"]([^'"]+)['"]/g;
const models = [];
let match;
while ((match = modelRegex.exec(htlContent)) !== null) {
  models.push(match[1]);
}
```

**方法 2: 查找 Java 文件**
```javascript
// 将类名转换为文件路径
function findJavaFile(className) {
  const parts = className.split('.');
  const fileName = parts.pop();
  const packagePath = parts.join('/');
  return `/apps/myapp/core/src/main/java/${packagePath}/${fileName}.java`;
}
```

#### 依赖分析

```javascript
async function analyzeSlingModelDependencies(modelClass) {
  const javaFile = await findJavaFile(modelClass);
  const javaContent = await aemClient.getText(javaFile);
  
  // 解析导入语句
  const importRegex = /import\s+([\w.]+);/g;
  const imports = [];
  let match;
  while ((match = importRegex.exec(javaContent)) !== null) {
    imports.push(match[1]);
  }
  
  // 解析依赖注入
  const injectRegex = /@Inject\s+([\w.]+)\s+(\w+);/g;
  const injections = [];
  while ((match = injectRegex.exec(javaContent)) !== null) {
    injections.push({
      type: match[1],
      name: match[2]
    });
  }
  
  return { imports, injections };
}
```

### 3. 子组件依赖

#### 查找方法

**从 HTL 模板提取**
```javascript
// 解析 data-sly-resource 引用
const resourceRegex = /data-sly-resource\s*=\s*['"]([^'"]+)['"][^>]*resourceType=['"]([^'"]+)['"]/g;
const childComponents = [];
let match;
while ((match = resourceRegex.exec(htlContent)) !== null) {
  childComponents.push({
    path: match[1],
    resourceType: match[2]
  });
}
```

#### 递归分析

```javascript
async function analyzeChildComponents(componentPath, depth = 0, maxDepth = 3) {
  if (depth > maxDepth) return [];
  
  const htlContent = await getHTLContent(componentPath);
  const children = extractChildComponents(htlContent);
  
  const result = [];
  for (const child of children) {
    const childInfo = {
      resourceType: child.resourceType,
      path: child.path
    };
    
    // 递归分析子组件
    if (depth < maxDepth) {
      childInfo.children = await analyzeChildComponents(
        child.resourceType,
        depth + 1,
        maxDepth
      );
    }
    
    result.push(childInfo);
  }
  
  return result;
}
```

### 4. 继承关系

#### 查找方法

```javascript
async function analyzeInheritance(componentPath) {
  const inheritanceChain = [];
  let currentPath = componentPath;
  
  while (currentPath) {
    const contentXml = await aemClient.getJSON(`${currentPath}.json`);
    inheritanceChain.push({
      path: currentPath,
      title: contentXml['jcr:title'],
      resourceSuperType: contentXml['sling:resourceSuperType']
    });
    
    currentPath = contentXml['sling:resourceSuperType'];
    if (!currentPath || currentPath.startsWith('foundation/')) {
      break;
    }
  }
  
  return inheritanceChain;
}
```

---

## 资源引用分析

### 1. 图片引用

#### 从 HTL 模板提取

```javascript
// 提取图片引用
const imageRegex = /(?:src|data-src)=['"]([^'"]+\.(jpg|png|gif|svg|webp))['"]/gi;
const images = [];
let match;
while ((match = imageRegex.exec(htlContent)) !== null) {
  images.push(match[1]);
}
```

#### 从 Sling Model 提取

```javascript
// 从 Java 代码中提取资源路径
const resourcePathRegex = /['"](\/content\/dam\/[^'"]+)['"]/g;
const resourcePaths = [];
let match;
while ((match = resourcePathRegex.exec(javaContent)) !== null) {
  resourcePaths.push(match[1]);
}
```

### 2. 字体引用

#### 从 CSS 文件提取

```javascript
// 提取 @font-face 中的字体文件
const fontFaceRegex = /@font-face\s*\{[^}]*url\(['"]?([^'")]+\.(woff|woff2|ttf|otf))['"]?\)/gi;
const fonts = [];
let match;
while ((match = fontFaceRegex.exec(cssContent)) !== null) {
  fonts.push(match[1]);
}
```

### 3. 其他资源

#### 从各种文件提取

```javascript
// 提取所有资源引用
function extractResourceReferences(content, fileType) {
  const resources = [];
  
  if (fileType === 'html') {
    // HTML 中的资源引用
    const htmlRegex = /(?:src|href|data-[\w-]+)=['"]([^'"]+)['"]/gi;
    let match;
    while ((match = htmlRegex.exec(content)) !== null) {
      resources.push(match[1]);
    }
  } else if (fileType === 'css') {
    // CSS 中的资源引用
    const cssRegex = /url\(['"]?([^'")]+)['"]?\)/gi;
    let match;
    while ((match = cssRegex.exec(content)) !== null) {
      resources.push(match[1]);
    }
  } else if (fileType === 'java') {
    // Java 中的资源路径
    const javaRegex = /['"](\/[^'"]+)['"]/g;
    let match;
    while ((match = javaRegex.exec(content)) !== null) {
      resources.push(match[1]);
    }
  }
  
  return resources;
}
```

---

## 收集策略

### 策略 1: 广度优先收集

**适用场景**: 需要快速了解组件结构

```javascript
async function breadthFirstCollect(componentPath) {
  const queue = [componentPath];
  const collected = new Set();
  
  while (queue.length > 0) {
    const current = queue.shift();
    if (collected.has(current)) continue;
    collected.add(current);
    
    // 收集当前组件
    const componentData = await collectComponent(current);
    
    // 添加子组件到队列
    for (const child of componentData.childComponents) {
      queue.push(child.resourceType);
    }
  }
  
  return Array.from(collected).map(path => collectedData[path]);
}
```

### 策略 2: 深度优先收集

**适用场景**: 需要完整的依赖链

```javascript
async function depthFirstCollect(componentPath, depth = 0, maxDepth = 5) {
  if (depth > maxDepth) return null;
  
  const componentData = await collectComponent(componentPath);
  
  // 递归收集子组件
  for (const child of componentData.childComponents) {
    componentData.children = componentData.children || [];
    componentData.children.push(
      await depthFirstCollect(child.resourceType, depth + 1, maxDepth)
    );
  }
  
  return componentData;
}
```

### 策略 3: 按需收集

**适用场景**: 只需要特定类型的信息

```javascript
async function selectiveCollect(componentPath, options = {}) {
  const result = {};
  
  if (options.includeFiles !== false) {
    result.files = await collectFiles(componentPath);
  }
  
  if (options.includeConfigs !== false) {
    result.configs = await collectConfigs(componentPath);
  }
  
  if (options.includeDependencies !== false) {
    result.dependencies = await collectDependencies(componentPath);
  }
  
  return result;
}
```

---

## 总结

### 核心要点

1. **组件结构**: 了解组件的完整文件结构
2. **文件类型**: 识别不同类型的文件及其作用
3. **配置解析**: 理解各种配置文件的格式和内容
4. **依赖分析**: 分析组件的各种依赖关系
5. **资源提取**: 提取组件引用的所有资源

### 收集清单

- ✅ 组件定义文件（.content.xml）
- ✅ HTL 模板文件
- ✅ Java Sling Model 文件
- ✅ CSS 样式文件（通过 ClientLibs）
- ✅ JavaScript 文件（通过 ClientLibs）
- ✅ 配置文件（dialog、editConfig、htmlTag 等）
- ✅ 静态资源（图片、字体等）
- ✅ 依赖关系（ClientLibs、Sling Models、子组件）
- ✅ 继承关系（resourceSuperType）

