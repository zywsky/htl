# AEM 组件信息提取完整指南

## 学习目标

从零开始，系统地学习如何从 Adobe Experience Manager (AEM) 中提取组件信息，以便用 React 重新实现这些组件。

## 学习路径

### 第一阶段：基础概念
1. **理解 JCR 结构**
   - JCR (Java Content Repository) 是 AEM 的底层存储
   - 组件存储在 `/apps` 或 `/libs` 路径下
   - 组件由多个部分组成：组件定义、对话框、客户端库等

2. **组件结构了解**
   ```
   /apps/myproject/components/mycomponent
   ├── .content.xml              # 组件定义
   ├── _cq_dialog.xml            # 触摸优化对话框
   ├── _cq_dialog/.content.xml   # 经典对话框
   ├── _cq_editConfig.xml        # 编辑配置
   └── mycomponent.html          # HTL 模板
   ```

### 第二阶段：JCR 访问基础
- 使用 Sling Repository API 访问 JCR
- 理解 Node、Property、Value 等核心概念
- 遍历组件节点树

### 第三阶段：组件信息提取
- 提取组件属性（sling:resourceType, componentGroup 等）
- 解析对话框配置（_cq_dialog）
- 提取组件编辑配置（_cq_editConfig）
- 分析组件依赖的客户端库（cq:clientlibs）

### 第四阶段：高级分析
- 组件继承关系（sling:resourceSuperType）
- 多站点/多语言支持分析
- 组件使用统计
- 组件依赖关系图

### 第五阶段：数据导出
- 将组件信息导出为 JSON/XML
- 生成 React 组件代码结构
- 创建组件文档

## 技术栈

- **Java 8+** - 主要开发语言
- **Sling JCR API** - JCR 访问
- **Apache Sling API** - Sling 框架
- **Jackson** - JSON 序列化（可选）
- **Maven** - 项目构建

## 项目结构

```
.
├── README.md                           # 本文件
├── pom.xml                            # Maven 构建配置
├── src/
│   └── main/
│       └── java/
│           └── com/aem/
│               └── component/
│                   ├── info/
│                   │   ├── ComponentInfoExtractor.java      # 核心提取器
│                   │   ├── DialogAnalyzer.java              # 对话框分析器
│                   │   ├── ComponentPropertyExtractor.java  # 属性提取器
│                   │   └── ComponentExporter.java           # 导出工具
│                   └── util/
│                       └── JCRUtil.java                     # JCR 工具类
└── examples/
    └── BasicJCRAccess.java            # 基础 JCR 访问示例
```

## 快速开始

### 1. 环境准备

确保你的开发环境可以访问 AEM 实例的 JCR。

### 2. 运行示例

```bash
mvn clean compile exec:java -Dexec.mainClass="com.aem.component.info.examples.BasicJCRAccess"
```

## 核心概念

### 组件的关键属性

- `sling:resourceType` - 组件的资源类型（唯一标识）
- `componentGroup` - 组件所属的组
- `jcr:title` - 组件显示名称
- `jcr:description` - 组件描述
- `sling:resourceSuperType` - 组件继承的父类型

### 对话框类型

1. **触摸优化对话框** (`_cq_dialog.xml`)
   - Granite UI 组件
   - 现代 AEM 标准

2. **经典对话框** (`_cq_dialog/.content.xml`)
   - ExtJS 组件
   - 旧版 AEM

## 注意事项

- JCR 访问需要适当的权限
- 某些系统组件可能在只读路径 (`/libs`)
- 大型组件树遍历可能需要优化
- 生产环境操作前请备份

## 下一步

按照代码示例的顺序学习：
1. 从 `examples/BasicJCRAccess.java` 开始
2. 理解 `JCRUtil.java` 中的工具方法
3. 深入学习 `ComponentInfoExtractor.java`
4. 掌握 `DialogAnalyzer.java` 的对话框解析
5. 使用 `ComponentExporter.java` 导出完整信息

## 完整文档列表

项目包含以下完整文档：

- **README.md** - 本文件，完整学习指南
- **QUICKSTART.md** - 快速入门指南（5分钟上手）
- **LEARNING_PATH.md** - 8个阶段的系统性学习路径
- **ADVANCED_GUIDE.md** - 高级功能指南（OSGi集成、性能优化等）
- **PROJECT_OVERVIEW.md** - 项目概览和使用场景
- **FAQ.md** - 25个常见问题解答
- **DATA_MODEL.md** - 导出的JSON数据模型说明
- **CASE_STUDY.md** - 实际案例研究（完整分析流程）
- **DEPLOYMENT_GUIDE.md** - 部署指南（4种部署方式）
- **TROUBLESHOOTING.md** - 故障排除指南（16个常见问题）
- **BEST_PRACTICES.md** - 最佳实践指南
- **USE_CASES.md** - 8个实际使用场景和解决方案

## 代码结构

- **核心功能**: `info/` 包下的提取器、分析器、导出工具
- **工具类**: `util/` 包下的JCR工具类、查询工具
- **示例代码**: `examples/` 包下的各种使用示例
- **模板代码**: `templates/` 包下的服务模板
- **工具程序**: `tools/` 包下的命令行工具
- **测试代码**: `test/` 包下的单元测试示例

