# 数据模型说明

## 导出的 JSON 数据结构

本文档详细说明了 `ComponentExporter` 导出的 JSON 数据结构，帮助你理解每个字段的含义。

---

## 完整组件信息结构

```json
{
  "basicProperties": { ... },
  "properties": { ... },
  "dialog": { ... },
  "designDialog": { ... },
  "template": { ... },
  "dependencies": { ... },
  "usage": { ... },
  "extractedAt": 1234567890,
  "componentPath": "/apps/myproject/components/mycomponent",
  "componentName": "mycomponent"
}
```

---

## 1. basicProperties（基本属性）

组件的核心属性信息。

```json
{
  "basicProperties": {
    "sling:resourceType": "/apps/myproject/components/mycomponent",
    "jcr:title": "我的组件",
    "jcr:description": "这是一个示例组件",
    "componentGroup": "我的项目",
    "sling:resourceSuperType": "/libs/core/wcm/components/text/v2/text",
    "cq:icon": "[Icon]",
    "cq:tags": "tag1,tag2",
    "componentPath": "/apps/myproject/components/mycomponent",
    "componentName": "mycomponent",
    "primaryNodeType": "cq:Component",
    "isCqComponent": "true"
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `sling:resourceType` | String | 组件的资源类型，唯一标识符 |
| `jcr:title` | String | 组件显示名称（在组件浏览器中显示） |
| `jcr:description` | String | 组件描述 |
| `componentGroup` | String | 组件所属的组（组件浏览器的分类） |
| `sling:resourceSuperType` | String | 组件继承的父组件类型 |
| `cq:icon` | String | 组件图标标识 |
| `cq:tags` | String | 组件标签（逗号分隔） |
| `componentPath` | String | 组件在 JCR 中的完整路径 |
| `componentName` | String | 组件节点名称 |
| `primaryNodeType` | String | 节点的主类型 |
| `isCqComponent` | String | 是否是 cq:Component 类型 |

---

## 2. properties（所有属性）

包含组件的所有属性信息，包括基本属性、编辑配置、客户端库等。

```json
{
  "properties": {
    "basic": { ... },
    "editConfig": { ... },
    "clientLibraries": { ... },
    "rawProperties": { ... }
  }
}
```

### 2.1 basic（基本属性）

同 `basicProperties`，见上文。

### 2.2 editConfig（编辑配置）

组件的编辑配置信息。

```json
{
  "editConfig": {
    "exists": true,
    "properties": {
      "cq:actions": "[EDIT,COPYMOVE,DELETE,INSERT]",
      "cq:dialogMode": "floating"
    },
    "inplaceEditing": {
      "editorType": "text",
      "active": "true"
    },
    "listeners": {
      "afterdelete": "REFRESH_PAGE",
      "afteredit": "REFRESH_PAGE"
    },
    "formParameters": {
      "param1": "value1"
    }
  }
}
```

### 2.3 clientLibraries（客户端库）

组件依赖的 CSS 和 JavaScript 资源。

```json
{
  "clientLibraries": {
    "exists": true,
    "properties": {
      "categories": "[myproject.components]"
    },
    "htmlTag": "..."
  }
}
```

### 2.4 rawProperties（原始属性）

组件的所有原始属性（包括系统属性）。

```json
{
  "rawProperties": {
    "jcr:primaryType": "cq:Component",
    "jcr:created": "2024-01-01T00:00:00.000Z",
    "sling:resourceType": "/apps/myproject/components/mycomponent",
    ...
  }
}
```

---

## 3. dialog（对话框配置）

组件的对话框配置，定义了作者可以编辑的属性。

```json
{
  "dialog": {
    "type": "touch",
    "touchDialog": {
      "properties": { ... },
      "content": {
        "items": [ ... ]
      },
      "fields": [
        {
          "nodeName": "title",
          "nodeType": "granite/ui/components/coral/foundation/form/textfield",
          "path": "/apps/myproject/components/mycomponent/_cq_dialog/content/items/column/items/title",
          "properties": {
            "name": "./title",
            "fieldLabel": "标题",
            "required": "true",
            "value": "默认标题"
          },
          "name": "./title",
          "fieldLabel": "标题",
          "defaultValue": "默认标题",
          "required": true,
          "options": [
            {
              "text": "选项1",
              "value": "option1",
              "jcr:title": "选项1"
            }
          ],
          "validation": {
            "required": "true",
            "pattern": "^[A-Z].*"
          }
        }
      ]
    },
    "classicDialog": { ... }
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | String | 对话框类型：`"touch"`、`"classic"` 或 `"none"` |
| `touchDialog` | Object | 触摸优化对话框信息（如果存在） |
| `classicDialog` | Object | 经典对话框信息（如果存在） |
| `fields` | Array | 对话框中的所有字段列表 |

### 字段对象结构

每个字段对象包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `nodeName` | String | 字段节点名称 |
| `nodeType` | String | 字段节点类型（Granite UI 组件类型） |
| `path` | String | 字段在 JCR 中的路径 |
| `name` | String | 字段名称（对应组件属性名，如 `./title`） |
| `fieldLabel` | String | 字段标签（显示给用户） |
| `defaultValue` | String | 默认值 |
| `required` | Boolean | 是否必填 |
| `options` | Array | 选项列表（用于 select、radio 等字段） |
| `validation` | Object | 验证规则 |

---

## 4. designDialog（设计对话框）

设计对话框配置（用于配置设计模式属性）。

```json
{
  "designDialog": {
    "exists": true,
    "analysis": {
      "properties": { ... },
      "fields": [ ... ]
    }
  }
}
```

结构与 `dialog` 类似。

---

## 5. template（模板文件信息）

组件的模板文件信息。

```json
{
  "template": {
    "templateFiles": [
      "component.html",
      "template.html"
    ],
    "component.html_properties": {
      "jcr:primaryType": "nt:file",
      "jcr:mimeType": "text/html"
    },
    "hasTemplateFolder": false
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `templateFiles` | Array | 找到的模板文件列表 |
| `{filename}_properties` | Object | 模板文件的属性（如果存在） |
| `hasTemplateFolder` | Boolean | 是否有模板文件夹 |

---

## 6. dependencies（依赖关系）

组件的依赖信息。

```json
{
  "dependencies": {
    "resourceSuperType": "/libs/core/wcm/components/text/v2/text",
    "resourceSuperTypeExists": true,
    "clientLibraries": { ... }
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `resourceSuperType` | String | 父组件资源类型 |
| `resourceSuperTypeExists` | Boolean | 父组件是否存在 |
| `clientLibraries` | Object | 客户端库依赖（见 properties.clientLibraries） |

---

## 7. usage（使用信息）

组件的使用情况信息（需要额外的查询）。

```json
{
  "usage": {
    "resourceType": "/apps/myproject/components/mycomponent",
    "note": "使用信息提取需要额外的 JCR 查询，此处提供框架"
  }
}
```

**注意**: 实际使用信息需要使用 `ComponentQueryUtil.findPagesUsingComponent()` 获取。

---

## 8. 元数据

```json
{
  "extractedAt": 1704067200000,
  "componentPath": "/apps/myproject/components/mycomponent",
  "componentName": "mycomponent"
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `extractedAt` | Number | 提取时间戳（毫秒） |
| `componentPath` | String | 组件路径 |
| `componentName` | String | 组件名称 |

---

## 简化信息结构

使用 `extractComponentInfoSimple()` 方法时，返回简化结构：

```json
{
  "sling:resourceType": "/apps/myproject/components/mycomponent",
  "jcr:title": "我的组件",
  "jcr:description": "组件描述",
  "componentGroup": "我的项目",
  "sling:resourceSuperType": "/libs/core/wcm/components/text/v2/text",
  "componentPath": "/apps/myproject/components/mycomponent",
  "componentName": "mycomponent",
  "hasDialog": true
}
```

---

## 批量导出结构

批量导出时，会生成索引文件 `index.json`：

```json
{
  "exportedAt": 1704067200000,
  "totalComponents": 10,
  "components": [
    {
      "name": "mycomponent",
      "title": "我的组件",
      "resourceType": "/apps/myproject/components/mycomponent",
      "path": "/apps/myproject/components/mycomponent",
      "group": "我的项目",
      "file": "mycomponent.json"
    }
  ]
}
```

---

## React 组件建议结构

`exportReactComponentSuggestions()` 生成的 Markdown 文件包含：

```markdown
# 组件名称

**Resource Type:** `/apps/myproject/components/mycomponent`

**Description:** 组件描述

## React 组件建议

### Props 接口

```typescript
interface ComponentProps {
  title?: string; // 标题
  description: string; // 描述
  ...
}
```

### 组件结构建议

- `Component.tsx` - 主组件
- `Component.module.css` - 样式文件
- `Component.types.ts` - TypeScript 类型定义
- `index.ts` - 导出文件
```

---

## 数据类型映射

### JCR 属性类型到 JSON 类型

| JCR 类型 | JSON 类型 | 说明 |
|----------|-----------|------|
| String | String | 字符串 |
| Long | Number | 整数 |
| Double | Number | 浮点数 |
| Boolean | Boolean | 布尔值 |
| Date | String | ISO 8601 格式日期字符串 |
| Binary | String | "[二进制数据]" 或 base64 |

### 字段类型到 TypeScript 类型

| Granite UI 字段类型 | TypeScript 类型 |
|---------------------|-----------------|
| textfield | `string` |
| textarea | `string` |
| numberfield | `number` |
| checkbox | `boolean` |
| switch | `boolean` |
| datepicker | `Date \| string` |
| select | `string` (或 union type) |
| radio | `string` (或 union type) |

---

## 使用示例

### 解析导出的 JSON

```java
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> componentInfo = mapper.readValue(jsonFile, Map.class);

// 获取基本属性
@SuppressWarnings("unchecked")
Map<String, String> basicProps = 
    (Map<String, String>) componentInfo.get("basicProperties");
String title = basicProps.get("jcr:title");

// 获取对话框字段
@SuppressWarnings("unchecked")
Map<String, Object> dialog = (Map<String, Object>) componentInfo.get("dialog");
@SuppressWarnings("unchecked")
List<Map<String, Object>> fields = 
    (List<Map<String, Object>>) dialog.get("fields");
```

### 在 JavaScript/TypeScript 中使用

```typescript
interface ComponentInfo {
  basicProperties: {
    'sling:resourceType': string;
    'jcr:title': string;
    // ...
  };
  dialog: {
    type: 'touch' | 'classic' | 'none';
    fields: Array<{
      name: string;
      fieldLabel: string;
      required: boolean;
      // ...
    }>;
  };
  // ...
}

const componentInfo: ComponentInfo = JSON.parse(jsonString);
```

---

## 注意事项

1. **字段名称**: 某些字段使用 JCR 属性名（如 `jcr:title`），包含命名空间前缀
2. **空值处理**: 不存在的属性可能为 `null` 或不在 JSON 中
3. **数组格式**: 多值属性会转换为逗号分隔的字符串
4. **路径格式**: 所有路径使用 JCR 路径格式（以 `/` 开头）
5. **时间戳**: `extractedAt` 使用 Unix 时间戳（毫秒）

---

## 扩展数据结构

如果需要添加新的数据字段，可以：

1. 扩展 `ComponentInfoExtractor` 添加新的提取逻辑
2. 在返回的 Map 中添加新字段
3. 更新本文档说明新字段

---

希望这个文档帮助你理解导出的数据结构！📊

