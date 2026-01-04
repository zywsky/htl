# 项目概览

## 项目简介

这是一个完整的 AEM 组件信息提取学习项目，旨在帮助你从零开始，系统地学习如何从 Adobe Experience Manager (AEM) 中提取组件信息，以便用 React 重新实现这些组件。

## 项目目标

1. ✅ 理解 AEM 组件在 JCR 中的存储结构
2. ✅ 掌握通过 Java/JCR API 访问组件节点的方法
3. ✅ 提取组件的所有关键信息（属性、对话框、模板等）
4. ✅ 将组件信息导出为结构化数据（JSON）
5. ✅ 生成 React 组件重构建议

## 项目结构

```
aem-component-info/
│
├── README.md                          # 完整学习指南
├── QUICKSTART.md                      # 快速入门指南
├── LEARNING_PATH.md                   # 系统性学习路径
├── ADVANCED_GUIDE.md                  # 高级功能指南
├── PROJECT_OVERVIEW.md                # 本文件 - 项目概览
├── pom.xml                            # Maven 构建配置
│
└── src/main/java/com/aem/component/
    │
    ├── util/
    │   └── JCRUtil.java               # JCR 工具类
    │                                   # - 安全节点访问
    │                                   # - 属性提取
    │                                   # - 节点遍历
    │
    ├── info/
    │   │
    │   ├── ComponentPropertyExtractor.java
    │   │                               # 组件属性提取器
    │   │                               # - 基本属性提取
    │   │                               # - 编辑配置提取
    │   │                               # - 客户端库提取
    │   │
    │   ├── DialogAnalyzer.java
    │   │                               # 对话框分析器
    │   │                               # - 触摸对话框分析
    │   │                               # - 经典对话框分析
    │   │                               # - 字段提取
    │   │
    │   ├── ComponentInfoExtractor.java
    │   │                               # 核心组件信息提取器
    │   │                               # - 完整信息提取
    │   │                               # - 批量提取
    │   │                               # - 模板分析
    │   │
    │   ├── ComponentExporter.java
    │   │                               # 组件信息导出器
    │   │                               # - JSON 导出
    │   │                               # - React 建议生成
    │   │                               # - 批量导出
    │   │
    │   └── examples/
    │       │
    │       ├── BasicJCRAccess.java
    │       │                           # 基础 JCR 访问示例
    │       │                           # - JCR 连接
    │       │                           # - 节点访问
    │       │                           # - 属性读取
    │       │
    │       ├── ComponentInfoExtractionExample.java
    │       │                           # 完整提取示例
    │       │                           # - 单个组件提取
    │       │                           # - 批量提取
    │       │                           # - 信息导出
    │       │
    │       └── OSGiComponentExtractorService.java
    │                                   # OSGi 服务示例
    │                                   # - 服务创建
    │                                   # - 依赖注入
    │                                   # - 实际应用
```

## 核心功能

### 1. JCR 工具类 (`JCRUtil`)

提供安全的 JCR 节点访问方法：
- `getProperty()` - 获取节点属性
- `getAllProperties()` - 获取所有属性
- `getChildNode()` - 获取子节点
- `getChildNodes()` - 获取所有子节点
- `nodeExists()` - 检查节点是否存在

### 2. 组件属性提取 (`ComponentPropertyExtractor`)

提取组件的各种属性：
- 基本属性（title, description, resourceType 等）
- 编辑配置（_cq_editConfig）
- 客户端库依赖
- 组件继承关系

### 3. 对话框分析 (`DialogAnalyzer`)

深入分析组件对话框：
- 触摸优化对话框解析
- 字段定义提取
- 字段类型识别
- 验证规则提取

### 4. 完整信息提取 (`ComponentInfoExtractor`)

整合所有功能：
- 提取完整组件信息
- 批量提取多个组件
- 模板文件分析
- 依赖关系分析

### 5. 数据导出 (`ComponentExporter`)

导出为可用格式：
- JSON 格式导出
- React 组件建议生成
- TypeScript 类型推断
- 批量导出支持

## 使用场景

### 场景 1: 组件迁移准备

在将 AEM 组件迁移到 React 之前，需要了解组件的完整配置：

```java
ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
Map<String, Object> info = extractor.extractComponentInfo("/apps/myproject/components/mycomponent");
ComponentExporter exporter = new ComponentExporter();
exporter.exportComponentToJson(info, "output/mycomponent.json");
```

### 场景 2: 组件文档生成

自动生成组件文档：

```java
List<Map<String, Object>> components = 
    extractor.extractComponentsFromPath("/apps/myproject/components");
exporter.exportComponentsToJson(components, "docs/components");
```

### 场景 3: 组件审计

分析项目中所有组件的配置：

```java
// 批量提取所有组件
List<Map<String, Object>> components = 
    extractor.extractComponentsFromPath("/apps/myproject/components");

// 分析组件配置一致性
for (Map<String, Object> component : components) {
    // 检查是否有对话框
    // 检查是否有文档
    // 检查配置是否完整
}
```

### 场景 4: React 重构辅助

生成 React 组件代码结构建议：

```java
Map<String, Object> componentInfo = extractor.extractComponentInfo(componentPath);
exporter.exportReactComponentSuggestions(componentInfo, "output/react-suggestion.md");
```

## 技术栈

- **Java 8+** - 主要开发语言
- **Maven** - 项目构建和依赖管理
- **Apache Sling API** - Sling 框架
- **JCR API** - Java Content Repository 访问
- **Jackson** - JSON 处理
- **SLF4J** - 日志记录

## 快速开始

1. **克隆或下载项目**

2. **编译项目**
   ```bash
   mvn clean compile
   ```

3. **阅读文档**
   - 新手：从 `QUICKSTART.md` 开始
   - 系统学习：按照 `LEARNING_PATH.md` 学习
   - 深入理解：阅读 `README.md` 和 `ADVANCED_GUIDE.md`

4. **运行示例**
   - 查看 `examples` 目录下的示例代码
   - 在 AEM bundle 中集成代码

## 学习路径

### 阶段 1: 基础（1-2 天）
- 理解 AEM 组件结构
- 学习 JCR 基础

### 阶段 2: 实践（3-5 天）
- JCR 访问
- 属性提取
- 对话框分析

### 阶段 3: 应用（2-3 天）
- 完整信息提取
- 数据导出
- OSGi 服务集成

### 阶段 4: 进阶（持续）
- 性能优化
- 功能扩展
- 实际项目应用

详细学习路径请参考 `LEARNING_PATH.md`。

## 代码质量

- ✅ 所有代码都有详细的中文注释
- ✅ 遵循 Java 编码规范
- ✅ 包含错误处理
- ✅ 提供使用示例
- ✅ 模块化设计，易于扩展

## 扩展性

项目设计考虑了扩展性：

1. **工具类可复用**: `JCRUtil` 可以在其他项目中使用
2. **模块化设计**: 各个提取器可以独立使用
3. **易于扩展**: 可以轻松添加新的提取功能
4. **格式支持**: 可以添加新的导出格式

## 注意事项

1. **权限**: 确保 JCR 会话有足够的读取权限
2. **性能**: 批量操作时注意性能优化
3. **环境**: 某些功能需要在 AEM OSGi 环境中运行
4. **版本**: 代码基于 AEM 6.x / Sling API 2.x

## 贡献指南

虽然这是一个学习项目，但如果你有改进建议：

1. 阅读代码理解现有实现
2. 提出改进建议或问题
3. 遵循现有的代码风格
4. 添加必要的注释

## 许可证

本项目仅用于学习目的。

## 参考资源

- [AEM 官方文档](https://experienceleague.adobe.com/docs/experience-manager.html)
- [Apache Sling 文档](https://sling.apache.org/)
- [JCR API 文档](https://jackrabbit.apache.org/jcr/jcr-api.html)
- [Granite UI 文档](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/granite-ui/api/index.html)

## 常见问题

**Q: 如何在实际 AEM 项目中使用？**  
A: 参考 `OSGiComponentExtractorService.java` 示例，创建一个 OSGi 服务。

**Q: 可以提取组件模板内容吗？**  
A: 可以，参考 `ADVANCED_GUIDE.md` 中的扩展功能章节。

**Q: 性能如何优化？**  
A: 参考 `ADVANCED_GUIDE.md` 中的性能优化章节。

**Q: 如何导出为其他格式？**  
A: 可以扩展 `ComponentExporter` 类，添加新的导出方法。

## 更新日志

### v1.0.0 (当前版本)
- ✅ 完整的 JCR 工具类
- ✅ 组件属性提取功能
- ✅ 对话框分析功能
- ✅ 完整信息提取功能
- ✅ JSON 导出功能
- ✅ React 组件建议生成
- ✅ 完整的文档和示例

---

**开始你的 AEM 组件信息提取之旅吧！** 🚀

建议学习顺序：
1. `QUICKSTART.md` - 快速了解
2. `LEARNING_PATH.md` - 系统性学习
3. `README.md` - 深入理解
4. `ADVANCED_GUIDE.md` - 高级功能

