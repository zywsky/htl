# 学习路径指南

## 从零到专家：系统性学习 AEM 组件信息提取

本指南提供了一条清晰的学习路径，帮助你从零开始，逐步掌握如何提取和分析 AEM 组件信息。

---

## 阶段 1: 基础概念（1-2 天）

### 目标
理解 AEM 组件的基本概念和 JCR 结构。

### 学习内容

1. **AEM 组件结构**
   - 组件在 JCR 中的存储位置（`/apps` 和 `/libs`）
   - 组件节点的基本结构
   - 组件的关键属性

2. **JCR 基础**
   - JCR (Java Content Repository) 的概念
   - Node、Property、Value 等核心概念
   - JCR 路径和命名空间

### 实践任务

- [ ] 阅读 `README.md` 中的"核心概念"部分
- [ ] 在 AEM 中手动浏览几个组件节点（使用 CRXDE Lite）
- [ ] 理解 `component/.content.xml` 的结构

### 学习材料

- `README.md` - 核心概念章节
- AEM 官方文档：JCR 基础

---

## 阶段 2: JCR 访问基础（2-3 天）

### 目标
学会通过 Java 代码访问和遍历 JCR 节点。

### 学习内容

1. **JCR API 基础**
   - 获取 Session
   - 访问节点
   - 读取属性
   - 遍历子节点

2. **工具类使用**
   - `JCRUtil.java` 中的辅助方法
   - 安全的节点访问模式
   - 错误处理

### 实践任务

- [ ] 运行 `BasicJCRAccess.java` 示例
- [ ] 修改示例代码，访问不同的组件路径
- [ ] 练习使用 `JCRUtil` 中的方法

### 代码文件

- `src/main/java/com/aem/component/util/JCRUtil.java`
- `src/main/java/com/aem/component/info/examples/BasicJCRAccess.java`

### 练习

```java
// 练习 1: 访问一个组件节点并打印其路径
Session session = ...;
Node componentNode = JCRUtil.getNode(session, "/apps/myproject/components/mycomponent");
System.out.println("组件路径: " + JCRUtil.getPath(componentNode));

// 练习 2: 列出组件的所有属性
Map<String, String> properties = JCRUtil.getAllProperties(componentNode);
properties.forEach((key, value) -> System.out.println(key + " = " + value));

// 练习 3: 遍历组件的子节点
List<Node> children = JCRUtil.getChildNodes(componentNode);
children.forEach(child -> System.out.println("子节点: " + JCRUtil.getName(child)));
```

---

## 阶段 3: 组件属性提取（2-3 天）

### 目标
学会提取组件的基本属性和配置信息。

### 学习内容

1. **组件基本属性**
   - `sling:resourceType`
   - `jcr:title` 和 `jcr:description`
   - `componentGroup`
   - `sling:resourceSuperType`

2. **编辑配置**
   - `_cq_editConfig` 节点
   - 内联编辑配置
   - 监听器配置

3. **客户端库**
   - `cq:clientlibs` 节点
   - CSS 和 JavaScript 依赖

### 实践任务

- [ ] 阅读 `ComponentPropertyExtractor.java` 代码
- [ ] 提取一个简单组件的基本属性
- [ ] 分析组件的编辑配置

### 代码文件

- `src/main/java/com/aem/component/info/ComponentPropertyExtractor.java`

### 练习

```java
// 练习: 提取组件的基本属性
ComponentPropertyExtractor extractor = new ComponentPropertyExtractor();
Map<String, String> basicProps = 
    ComponentPropertyExtractor.extractBasicProperties(componentNode);

System.out.println("组件名称: " + basicProps.get("jcr:title"));
System.out.println("Resource Type: " + basicProps.get("sling:resourceType"));
System.out.println("组件分组: " + basicProps.get("componentGroup"));
```

---

## 阶段 4: 对话框分析（3-4 天）

### 目标
深入理解并提取组件的对话框配置，包括字段定义和验证规则。

### 学习内容

1. **触摸优化对话框**
   - `_cq_dialog` 节点结构
   - Granite UI 组件
   - 字段类型识别

2. **对话框字段**
   - 字段属性（name, label, required 等）
   - 字段选项（select, radio）
   - 验证规则

3. **经典对话框**
   - `dialog` 节点结构
   - ExtJS 组件（了解即可）

### 实践任务

- [ ] 阅读 `DialogAnalyzer.java` 代码
- [ ] 分析一个包含多种字段类型的对话框
- [ ] 提取对话框中的所有字段定义

### 代码文件

- `src/main/java/com/aem/component/info/DialogAnalyzer.java`

### 练习

```java
// 练习: 分析组件的对话框
Map<String, Object> dialogInfo = DialogAnalyzer.analyzeDialog(componentNode);

@SuppressWarnings("unchecked")
List<Map<String, Object>> fields = 
    (List<Map<String, Object>>) dialogInfo.get("fields");

fields.forEach(field -> {
    @SuppressWarnings("unchecked")
    Map<String, String> props = (Map<String, String>) field.get("properties");
    System.out.println("字段: " + props.get("name") + 
                      ", 标签: " + props.get("fieldLabel"));
});
```

---

## 阶段 5: 完整信息提取（2-3 天）

### 目标
整合所有功能，提取组件的完整信息。

### 学习内容

1. **ComponentInfoExtractor 使用**
   - 提取完整的组件信息
   - 批量提取组件
   - 性能优化

2. **模板文件分析**
   - HTL 模板文件
   - 模板中的变量和逻辑

3. **依赖关系**
   - 组件继承关系
   - 客户端库依赖

### 实践任务

- [ ] 阅读 `ComponentInfoExtractor.java` 代码
- [ ] 提取一个复杂组件的完整信息
- [ ] 批量提取项目中的所有组件

### 代码文件

- `src/main/java/com/aem/component/info/ComponentInfoExtractor.java`
- `src/main/java/com/aem/component/info/examples/ComponentInfoExtractionExample.java`

### 练习

```java
// 练习: 提取组件的完整信息
ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
Map<String, Object> componentInfo = extractor.extractComponentInfo(componentPath);

// 查看不同部分的信息
System.out.println("基本属性: " + componentInfo.get("basicProperties"));
System.out.println("对话框: " + componentInfo.get("dialog"));
System.out.println("模板: " + componentInfo.get("template"));
```

---

## 阶段 6: 数据导出（1-2 天）

### 目标
将提取的组件信息导出为可用格式，为 React 重构做准备。

### 学习内容

1. **JSON 导出**
   - 使用 Jackson 序列化
   - 格式化输出
   - 批量导出

2. **React 组件建议**
   - 从对话框字段生成 Props 接口
   - TypeScript 类型推断
   - 组件结构建议

### 实践任务

- [ ] 阅读 `ComponentExporter.java` 代码
- [ ] 导出几个组件的 JSON 信息
- [ ] 生成 React 组件建议文档

### 代码文件

- `src/main/java/com/aem/component/info/ComponentExporter.java`

### 练习

```java
// 练习: 导出组件信息
ComponentExporter exporter = new ComponentExporter();

// 导出为 JSON 文件
exporter.exportComponentToJson(componentInfo, "output/component.json");

// 导出为 JSON 字符串
String json = exporter.exportComponentToJsonString(componentInfo);
System.out.println(json);

// 生成 React 建议
exporter.exportReactComponentSuggestions(componentInfo, "output/react-suggestion.md");
```

---

## 阶段 7: 实际应用（3-5 天）

### 目标
在真实的 AEM 项目中使用这些工具。

### 学习内容

1. **OSGi 服务集成**
   - 创建 OSGi 服务
   - 依赖注入
   - 服务生命周期

2. **HTTP Servlet 创建**
   - 通过 REST API 访问
   - 请求参数处理
   - 响应格式化

3. **性能优化**
   - 会话管理
   - 批量处理
   - 缓存策略

### 实践任务

- [ ] 在 AEM bundle 中创建 OSGi 服务
- [ ] 创建 HTTP Servlet 提供 REST API
- [ ] 优化提取性能

### 代码文件

- `src/main/java/com/aem/component/info/examples/OSGiComponentExtractorService.java`
- `ADVANCED_GUIDE.md` - OSGi 服务章节

---

## 阶段 8: 高级功能（持续学习）

### 目标
扩展功能，解决复杂场景。

### 学习内容

1. **组件使用统计**
   - JCR 查询
   - 使用情况分析

2. **依赖关系图**
   - 构建组件依赖图
   - 循环依赖检测

3. **模板内容分析**
   - HTL 模板解析
   - 数据模型提取

4. **自定义导出格式**
   - CSV 导出
   - XML 导出
   - 自定义格式

### 参考材料

- `ADVANCED_GUIDE.md` - 扩展功能章节
- AEM 官方文档
- JCR 查询 API 文档

---

## 学习检查清单

### 基础阶段（阶段 1-2）
- [ ] 理解 AEM 组件的基本结构
- [ ] 能够通过 JCR API 访问节点
- [ ] 熟悉 `JCRUtil` 工具类

### 中级阶段（阶段 3-5）
- [ ] 能够提取组件的基本属性
- [ ] 能够分析对话框配置
- [ ] 能够提取完整的组件信息

### 高级阶段（阶段 6-8）
- [ ] 能够导出组件信息为 JSON
- [ ] 能够创建 OSGi 服务
- [ ] 能够优化性能
- [ ] 能够扩展功能

---

## 学习建议

1. **循序渐进**: 按照阶段顺序学习，不要跳跃
2. **动手实践**: 每学一个概念，立即编写代码验证
3. **阅读代码**: 仔细阅读代码注释，理解实现细节
4. **修改示例**: 修改示例代码，尝试不同的场景
5. **记录问题**: 遇到问题及时记录和解决
6. **查看文档**: 参考 AEM 官方文档深入学习

## 预计学习时间

- **快速学习**（每天 2-3 小时）: 2-3 周
- **深入学习**（每天 1-2 小时）: 1-2 个月
- **专家水平**（持续实践）: 3-6 个月

## 下一步

完成当前阶段后：

1. 查看对应阶段的代码文件
2. 运行示例代码
3. 完成实践任务
4. 进入下一阶段

**记住**: 学习编程最重要的是实践。多写代码，多尝试，多思考！

