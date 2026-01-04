# AEM 快速开始指南

## 🚀 5 分钟快速开始

### 第一步：理解 AEM 架构（5 分钟）

快速了解 AEM 的核心概念：

```
AEM = JCR (存储) + Sling (框架) + OSGi (模块)
```

- **JCR**: 内容以节点树形式存储（类似文件系统）
- **Sling**: 将 HTTP 请求映射到 JCR 节点
- **OSGi**: 模块化 Java 应用框架

### 第二步：阅读第一个教程（15 分钟）

从 `01-basics/01-introduction.md` 开始，了解：
- AEM 是什么
- AEM 技术栈
- 项目结构

### 第三步：理解 JCR（20 分钟）

阅读 `01-basics/02-jcr-basics.md`，学习：
- 节点和属性的概念
- 如何读取和创建节点
- JCR 查询

### 第四步：理解 Sling（20 分钟）

阅读 `01-basics/03-sling-framework.md`，学习：
- 资源解析
- Sling Servlet 创建
- 请求处理

## 📚 学习顺序

### 新手路径（推荐）

1. **基础概念** (1-2 天)
   - `01-basics/01-introduction.md`
   - `01-basics/02-jcr-basics.md`
   - `01-basics/03-sling-framework.md`

2. **组件开发** (3-5 天)
   - `02-components/01-component-structure.md`
   - `03-htl/01-htl-basics.md`
   - `04-sling-models/01-sling-models-intro.md`

3. **OSGi 服务** (2-3 天)
   - `05-osgi/01-osgi-basics.md`

4. **高级主题** (1 周+)
   - `06-workflows/01-workflow-basics.md`
   - `07-apis/01-content-services.md`

### 快速参考

如果你已经熟悉某些概念，可以直接跳转到相关章节：

- **组件开发**: `02-components/`
- **模板语言**: `03-htl/`
- **业务逻辑**: `04-sling-models/`
- **服务开发**: `05-osgi/`
- **工作流**: `06-workflows/`
- **API 开发**: `07-apis/`

## 💻 第一个练习

### 创建第一个组件

1. 阅读 `02-components/01-component-structure.md`
2. 创建一个简单的文本组件
3. 添加对话框
4. 编写 HTL 模板
5. 创建 Sling Model

### 代码结构示例

```
/apps/myproject/components/mytextcomponent/
├── .content.xml              # 组件定义
├── _cq_dialog/
│   └── .content.xml          # 对话框
└── mytextcomponent.html      # HTL 模板
```

## 🎯 学习目标检查

完成基础学习后，你应该能够：

- [ ] 解释 AEM 的架构和核心组件
- [ ] 理解 JCR 节点结构
- [ ] 创建简单的 Sling Servlet
- [ ] 创建基本的 AEM 组件
- [ ] 编写 HTL 模板
- [ ] 使用 Sling Models
- [ ] 创建 OSGi 服务

## 📖 深入学习的下一步

完成快速开始后：

1. 阅读完整的 `LEARNING-GUIDE.md`
2. 按照学习路径系统学习
3. 完成实践项目
4. 参考官方文档深入学习

## 🔗 有用的链接

- **官方文档**: https://experienceleague.adobe.com/docs/experience-manager.html
- **Apache Sling**: https://sling.apache.org/
- **OSGi 规范**: https://www.osgi.org/

---

开始你的 AEM 学习之旅！🎉

