# 实际案例研究

## 案例：分析一个真实的 AEM 组件

本文档通过一个完整的实际案例，展示如何分析一个真实的 AEM 组件，并提取所有相关信息。

---

## 案例组件：文本组件 (Text Component)

假设我们要分析 AEM 核心组件中的文本组件：
- **路径**: `/libs/core/wcm/components/text/v2/text`
- **资源类型**: `core/wcm/components/text/v2/text`

---

## 步骤 1: 提取基本属性

### 代码

```java
Session session = repository.loginAdministrative(null);
try {
    ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
    Map<String, Object> componentInfo = extractor.extractComponentInfo(
        "/libs/core/wcm/components/text/v2/text"
    );
    
    @SuppressWarnings("unchecked")
    Map<String, String> basicProps = 
        (Map<String, String>) componentInfo.get("basicProperties");
    
    System.out.println("组件名称: " + basicProps.get("jcr:title"));
    System.out.println("资源类型: " + basicProps.get("sling:resourceType"));
    System.out.println("组件分组: " + basicProps.get("componentGroup"));
} finally {
    session.logout();
}
```

### 预期输出

```
组件名称: Text
资源类型: core/wcm/components/text/v2/text
组件分组: Core Components
```

---

## 步骤 2: 分析对话框配置

### 代码

```java
@SuppressWarnings("unchecked")
Map<String, Object> dialog = (Map<String, Object>) componentInfo.get("dialog");

if (!"none".equals(dialog.get("type"))) {
    System.out.println("对话框类型: " + dialog.get("type"));
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> fields = 
        (List<Map<String, Object>>) dialog.get("fields");
    
    System.out.println("字段数量: " + fields.size());
    
    for (Map<String, Object> field : fields) {
        @SuppressWarnings("unchecked")
        Map<String, String> props = (Map<String, String>) field.get("properties");
        System.out.println("  - " + props.get("name") + ": " + props.get("fieldLabel"));
    }
}
```

### 预期输出

```
对话框类型: touch
字段数量: 5
  - ./text: Text
  - ./textIsRich: Rich Text
  - ./id: Element ID
  - ./textStyle: Style
  - ./textColor: Text Color
```

---

## 步骤 3: 提取字段详细信息

### 代码

```java
@SuppressWarnings("unchecked")
List<Map<String, Object>> fields = 
    (List<Map<String, Object>>) dialog.get("fields");

for (Map<String, Object> field : fields) {
    @SuppressWarnings("unchecked")
    Map<String, String> props = (Map<String, String>) field.get("properties");
    
    String fieldName = props.get("name");
    String fieldLabel = props.get("fieldLabel");
    String nodeType = (String) field.get("nodeType");
    Boolean required = (Boolean) field.get("required");
    
    System.out.println("\n字段: " + fieldName);
    System.out.println("  标签: " + fieldLabel);
    System.out.println("  类型: " + nodeType);
    System.out.println("  必填: " + (required != null && required));
    
    // 如果有选项
    @SuppressWarnings("unchecked")
    List<Map<String, String>> options = 
        (List<Map<String, String>>) field.get("options");
    if (options != null && !options.isEmpty()) {
        System.out.println("  选项:");
        for (Map<String, String> option : options) {
            System.out.println("    - " + option.get("text") + " = " + option.get("value"));
        }
    }
}
```

### 预期输出

```
字段: ./text
  标签: Text
  类型: granite/ui/components/coral/foundation/form/richtext
  必填: false

字段: ./textIsRich
  标签: Rich Text
  类型: granite/ui/components/coral/foundation/form/checkbox
  必填: false

字段: ./textStyle
  标签: Style
  类型: granite/ui/components/coral/foundation/form/select
  必填: false
  选项:
    - Default = default
    - Heading 1 = h1
    - Heading 2 = h2
```

---

## 步骤 4: 分析组件依赖

### 代码

```java
@SuppressWarnings("unchecked")
Map<String, Object> dependencies = 
    (Map<String, Object>) componentInfo.get("dependencies");

String superType = (String) dependencies.get("resourceSuperType");
Boolean superTypeExists = (Boolean) dependencies.get("resourceSuperTypeExists");

System.out.println("父组件: " + superType);
System.out.println("父组件存在: " + superTypeExists);

if (superTypeExists) {
    // 递归分析父组件
    Map<String, Object> parentInfo = extractor.extractComponentInfo(superType);
    System.out.println("父组件名称: " + 
        ((Map<String, String>) parentInfo.get("basicProperties")).get("jcr:title"));
}
```

### 预期输出

```
父组件: core/wcm/components/commons/v1/templates/page
父组件存在: true
父组件名称: Page Template
```

---

## 步骤 5: 查找组件使用情况

### 代码

```java
String resourceType = "core/wcm/components/text/v2/text";
List<Node> pages = ComponentQueryUtil.findPagesUsingComponent(session, resourceType);

System.out.println("使用该组件的页面数量: " + pages.size());

// 统计前 10 个页面
int count = 0;
for (Node pageContent : pages) {
    if (count >= 10) break;
    
    String pagePath = pageContent.getPath();
    if (pagePath.contains("/jcr:content")) {
        pagePath = pagePath.substring(0, pagePath.indexOf("/jcr:content"));
    }
    System.out.println("  - " + pagePath);
    count++;
}
```

### 预期输出

```
使用该组件的页面数量: 45
  - /content/myproject/en/home
  - /content/myproject/en/about
  - /content/myproject/en/products
  ...
```

---

## 步骤 6: 导出为 JSON

### 代码

```java
ComponentExporter exporter = new ComponentExporter();

// 导出完整信息
exporter.exportComponentToJson(componentInfo, "output/text-component.json");

// 生成 React 建议
exporter.exportReactComponentSuggestions(componentInfo, "output/text-component-react.md");
```

### 导出的 JSON 结构（部分）

```json
{
  "basicProperties": {
    "sling:resourceType": "core/wcm/components/text/v2/text",
    "jcr:title": "Text",
    "componentGroup": "Core Components"
  },
  "dialog": {
    "type": "touch",
    "fields": [
      {
        "name": "./text",
        "fieldLabel": "Text",
        "nodeType": "granite/ui/components/coral/foundation/form/richtext",
        "required": false
      },
      {
        "name": "./textIsRich",
        "fieldLabel": "Rich Text",
        "nodeType": "granite/ui/components/coral/foundation/form/checkbox",
        "required": false
      }
    ]
  }
}
```

---

## 步骤 7: 生成 React 组件代码

基于提取的信息，可以生成 React 组件：

### TypeScript 接口

```typescript
interface TextComponentProps {
  text?: string;           // Text
  textIsRich?: boolean;   // Rich Text
  id?: string;           // Element ID
  textStyle?: string;     // Style
  textColor?: string;    // Text Color
}
```

### React 组件

```typescript
import React from 'react';
import styles from './TextComponent.module.css';

interface TextComponentProps {
  text?: string;
  textIsRich?: boolean;
  id?: string;
  textStyle?: string;
  textColor?: string;
}

export const TextComponent: React.FC<TextComponentProps> = ({
  text,
  textIsRich = false,
  id,
  textStyle = 'default',
  textColor
}) => {
  const content = textIsRich ? (
    <div dangerouslySetInnerHTML={{ __html: text || '' }} />
  ) : (
    <p>{text}</p>
  );

  return (
    <div 
      id={id} 
      className={`${styles.text} ${styles[textStyle]}`}
      style={{ color: textColor }}
    >
      {content}
    </div>
  );
};
```

---

## 完整示例代码

```java
package com.aem.component.info.examples;

import com.aem.component.info.ComponentExporter;
import com.aem.component.info.ComponentInfoExtractor;
import com.aem.component.info.ComponentQueryUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import java.util.List;
import java.util.Map;

@Component(service = ComponentAnalysisService.class)
public class ComponentAnalysisService {

    @Reference
    private SlingRepository repository;

    public void analyzeTextComponent() {
        String componentPath = "/libs/core/wcm/components/text/v2/text";
        
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            ComponentExporter exporter = new ComponentExporter();
            
            // 1. 提取组件信息
            Map<String, Object> componentInfo = extractor.extractComponentInfo(componentPath);
            
            // 2. 打印基本信息
            printBasicInfo(componentInfo);
            
            // 3. 分析对话框
            analyzeDialog(componentInfo);
            
            // 4. 查找使用情况
            findUsage(session, componentInfo);
            
            // 5. 导出信息
            exporter.exportComponentToJson(componentInfo, "output/text-component.json");
            exporter.exportReactComponentSuggestions(componentInfo, "output/text-component-react.md");
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.logout();
        }
    }
    
    private void printBasicInfo(Map<String, Object> componentInfo) {
        @SuppressWarnings("unchecked")
        Map<String, String> basicProps = 
            (Map<String, String>) componentInfo.get("basicProperties");
        
        System.out.println("=== 组件基本信息 ===");
        System.out.println("名称: " + basicProps.get("jcr:title"));
        System.out.println("资源类型: " + basicProps.get("sling:resourceType"));
        System.out.println("分组: " + basicProps.get("componentGroup"));
    }
    
    private void analyzeDialog(Map<String, Object> componentInfo) {
        @SuppressWarnings("unchecked")
        Map<String, Object> dialog = (Map<String, Object>) componentInfo.get("dialog");
        
        System.out.println("\n=== 对话框分析 ===");
        System.out.println("类型: " + dialog.get("type"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = 
            (List<Map<String, Object>>) dialog.get("fields");
        
        System.out.println("字段数量: " + fields.size());
        for (Map<String, Object> field : fields) {
            @SuppressWarnings("unchecked")
            Map<String, String> props = (Map<String, String>) field.get("properties");
            System.out.println("  - " + props.get("name") + ": " + props.get("fieldLabel"));
        }
    }
    
    private void findUsage(Session session, Map<String, Object> componentInfo) 
            throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> basicProps = 
            (Map<String, String>) componentInfo.get("basicProperties");
        String resourceType = basicProps.get("sling:resourceType");
        
        System.out.println("\n=== 使用情况 ===");
        List<Node> pages = ComponentQueryUtil.findPagesUsingComponent(session, resourceType);
        System.out.println("使用该组件的页面数量: " + pages.size());
    }
}
```

---

## 案例总结

通过这个案例，我们学会了：

1. ✅ 如何提取组件的基本属性
2. ✅ 如何分析对话框配置
3. ✅ 如何提取字段详细信息
4. ✅ 如何分析组件依赖关系
5. ✅ 如何查找组件使用情况
6. ✅ 如何导出为 JSON
7. ✅ 如何生成 React 组件代码

---

## 练习任务

尝试分析你自己的组件：

1. 选择一个 AEM 组件（可以是项目中的自定义组件）
2. 使用 `ComponentInfoExtractor` 提取信息
3. 分析对话框字段
4. 查找使用该组件的页面
5. 导出为 JSON
6. 生成 React 组件建议
7. 根据建议创建 React 组件

---

## 扩展练习

1. **批量分析**: 分析整个项目中的所有组件
2. **依赖图**: 构建组件依赖关系图
3. **使用统计**: 统计每个组件的使用频率
4. **配置对比**: 对比不同版本的组件配置
5. **迁移计划**: 基于分析结果制定迁移计划

---

祝你分析顺利！📊

