# 快速入门指南

## 5 分钟快速开始

### 前提条件

- Java 8 或更高版本
- Maven 3.6 或更高版本
- 可以访问 AEM 实例（本地或远程）

### 第一步：了解项目结构

```
.
├── README.md                    # 完整的学习指南
├── QUICKSTART.md               # 本文件 - 快速入门
├── ADVANCED_GUIDE.md           # 高级功能指南
├── pom.xml                     # Maven 构建配置
└── src/main/java/com/aem/component/
    ├── info/
    │   ├── ComponentInfoExtractor.java      # 核心提取器
    │   ├── ComponentPropertyExtractor.java  # 属性提取
    │   ├── DialogAnalyzer.java              # 对话框分析
    │   └── ComponentExporter.java           # 导出工具
    ├── util/
    │   └── JCRUtil.java                     # JCR 工具类
    └── examples/
        ├── BasicJCRAccess.java              # 基础示例
        └── ComponentInfoExtractionExample.java  # 完整示例
```

### 第二步：编译项目

```bash
# 克隆或下载项目后，进入项目目录
cd "aem component info"

# 编译项目
mvn clean compile
```

### 第三步：在 AEM Bundle 中使用（推荐）

如果你有 AEM bundle 项目，最简单的方式是：

1. **将代码复制到你的 bundle 项目**

```bash
# 在你的 AEM bundle 项目中
cp -r src/main/java/com/aem/component/* your-bundle/src/main/java/com/yourcompany/
```

2. **创建 OSGi 服务**（参考 `OSGiComponentExtractorService.java`）

```java
@Reference
private SlingRepository repository;

public Map<String, Object> extractComponent(String path) {
    Session session = repository.loginAdministrative(null);
    try {
        ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        return extractor.extractComponentInfo(path);
    } finally {
        session.logout();
    }
}
```

3. **构建并部署 bundle**

```bash
mvn clean install -PautoInstallPackage
```

### 第四步：提取第一个组件信息

#### 方式 1: 在 Java 代码中使用

```java
@Reference
private SlingRepository repository;

public void extractMyFirstComponent() {
    Session session = repository.loginAdministrative(null);
    try {
        ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        
        // 替换为你的组件路径
        String componentPath = "/apps/myproject/components/mycomponent";
        Map<String, Object> info = extractor.extractComponentInfo(componentPath);
        
        // 打印基本信息
        @SuppressWarnings("unchecked")
        Map<String, String> basic = (Map<String, String>) info.get("basicProperties");
        System.out.println("组件名称: " + basic.get("jcr:title"));
        System.out.println("Resource Type: " + basic.get("sling:resourceType"));
        
    } finally {
        session.logout();
    }
}
```

#### 方式 2: 导出为 JSON

```java
ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
ComponentExporter exporter = new ComponentExporter();

Map<String, Object> componentInfo = extractor.extractComponentInfo(componentPath);
exporter.exportComponentToJson(componentInfo, "output/mycomponent.json");
```

### 第五步：查看导出的信息

导出的 JSON 文件包含以下信息：

```json
{
  "basicProperties": {
    "sling:resourceType": "/apps/myproject/components/mycomponent",
    "jcr:title": "我的组件",
    "componentGroup": "我的项目",
    "componentPath": "/apps/myproject/components/mycomponent"
  },
  "dialog": {
    "type": "touch",
    "fields": [
      {
        "name": "title",
        "fieldLabel": "标题",
        "nodeType": "granite/ui/components/coral/foundation/form/textfield",
        "properties": { ... }
      }
    ]
  },
  "template": {
    "templateFiles": ["component.html"]
  }
}
```

## 常用操作

### 提取单个组件

```java
ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
Map<String, Object> info = extractor.extractComponentInfo("/apps/myproject/components/mycomponent");
```

### 批量提取组件

```java
ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
List<Map<String, Object>> components = 
    extractor.extractComponentsFromPath("/apps/myproject/components");
```

### 导出所有组件

```java
ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
ComponentExporter exporter = new ComponentExporter();

List<Map<String, Object>> components = 
    extractor.extractComponentsFromPath("/apps/myproject/components");
exporter.exportComponentsToJson(components, "output/components");
```

### 生成 React 组件建议

```java
ComponentExporter exporter = new ComponentExporter();
exporter.exportReactComponentSuggestions(componentInfo, "output/react-suggestions.md");
```

## 关键概念

### 1. 组件路径

组件在 JCR 中的路径，例如：
- `/apps/myproject/components/mycomponent`
- `/libs/core/wcm/components/text/v2/text`

### 2. Resource Type

组件的唯一标识符，通常是组件路径，例如：
- `myproject/components/mycomponent`

### 3. 对话框类型

- **触摸对话框** (`_cq_dialog`): 现代 AEM 使用的 Granite UI 对话框
- **经典对话框** (`dialog`): 旧版 ExtJS 对话框

### 4. 组件属性

- `jcr:title`: 组件显示名称
- `sling:resourceType`: 组件资源类型
- `componentGroup`: 组件分组
- `sling:resourceSuperType`: 父组件类型

## 故障排除

### 问题：找不到组件

**检查**:
1. 组件路径是否正确？
2. 会话是否有读取权限？
3. 组件是否真的存在于 JCR 中？

**解决**:
```java
// 检查节点是否存在
if (JCRUtil.nodeExists(session, componentPath)) {
    // 组件存在，继续处理
} else {
    System.err.println("组件不存在: " + componentPath);
}
```

### 问题：权限错误

**解决**:
```java
// 使用管理会话（开发环境）
Session session = repository.loginAdministrative(null);

// 或者使用有权限的用户会话（生产环境推荐）
Session session = repository.loginService("service-user", null);
```

### 问题：编译错误

**检查**:
1. Java 版本是否正确（需要 Java 8+）？
2. Maven 依赖是否正确下载？
3. 是否缺少 AEM/Sling 依赖？

**解决**:
```bash
# 清理并重新编译
mvn clean compile

# 检查依赖
mvn dependency:tree
```

## 下一步

完成快速入门后，建议：

1. 📖 阅读 [README.md](README.md) 了解完整的学习路径
2. 🚀 查看 [ADVANCED_GUIDE.md](ADVANCED_GUIDE.md) 学习高级功能
3. 💻 运行示例代码理解实现细节
4. 🔧 根据自己的需求扩展功能

## 获取帮助

- 查看代码注释：所有代码都有详细的中文注释
- 运行示例：查看 `examples` 目录下的示例代码
- 阅读文档：参考 README 和高级指南

## 学习路径总结

```
快速入门 (本文件)
    ↓
基础示例 (BasicJCRAccess.java)
    ↓
核心功能 (ComponentInfoExtractor.java)
    ↓
对话框分析 (DialogAnalyzer.java)
    ↓
导出工具 (ComponentExporter.java)
    ↓
高级功能 (ADVANCED_GUIDE.md)
    ↓
实际应用 (OSGiComponentExtractorService.java)
```

开始你的 AEM 组件信息提取之旅吧！🎉

