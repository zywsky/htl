# AEM 学习指南

## 🎯 学习目标

通过系统学习，掌握 Adobe Experience Manager (AEM) 从基础到高级的所有核心知识和技能。

## 📚 学习路径

### 阶段一：基础知识（第 1-2 周）

#### 1. AEM 基础概念
- [ ] 阅读：`01-basics/01-introduction.md`
- [ ] 理解 AEM 架构和技术栈
- [ ] 了解 JCR、Sling、OSGi 的关系
- [ ] 设置开发环境

#### 2. JCR 基础
- [ ] 阅读：`01-basics/02-jcr-basics.md`
- [ ] 理解节点和属性的概念
- [ ] 练习：创建、读取、更新、删除节点
- [ ] 练习：JCR 查询

#### 3. Sling 框架
- [ ] 阅读：`01-basics/03-sling-framework.md`
- [ ] 理解资源解析和 Servlet 解析
- [ ] 练习：创建 Sling Servlet
- [ ] 理解请求/响应处理

### 阶段二：组件开发（第 3-4 周）

#### 4. 组件结构
- [ ] 阅读：`02-components/01-component-structure.md`
- [ ] 理解组件的三要素：节点定义、对话框、模板
- [ ] 练习：创建第一个组件
- [ ] 练习：组件对话框开发

#### 5. HTL 模板语言
- [ ] 阅读：`03-htl/01-htl-basics.md`
- [ ] 掌握 HTL 语法和表达式
- [ ] 理解上下文（Context）的重要性
- [ ] 练习：编写组件模板

#### 6. Sling Models
- [ ] 阅读：`04-sling-models/01-sling-models-intro.md`
- [ ] 理解依赖注入和注解
- [ ] 练习：创建 Sling Model
- [ ] 练习：在 HTL 中使用 Model

### 阶段三：OSGi 和服务（第 5-6 周）

#### 7. OSGi 基础
- [ ] 阅读：`05-osgi/01-osgi-basics.md`
- [ ] 理解 Bundle 和 Service 的概念
- [ ] 练习：创建 OSGi 服务
- [ ] 练习：服务依赖注入

### 阶段四：高级主题（第 7-8 周）

#### 8. 工作流
- [ ] 阅读：`06-workflows/01-workflow-basics.md`
- [ ] 理解工作流模型和进程
- [ ] 练习：创建工作流进程步骤
- [ ] 练习：配置工作流模型

#### 9. 内容服务和 API
- [ ] 阅读：`07-apis/01-content-services.md`
- [ ] 理解 Sling Model Exporter
- [ ] 练习：创建 REST API
- [ ] 练习：内容导出

## 💻 实践项目建议

### 项目 1：个人博客组件（第 2-4 周）
创建一个完整的博客组件集：
- 文章列表组件
- 文章详情组件
- 分类组件
- 标签组件

### 项目 2：产品展示系统（第 5-6 周）
创建一个产品展示系统：
- 产品列表组件
- 产品详情组件
- 产品搜索功能
- 产品筛选功能

### 项目 3：内容管理工具（第 7-8 周）
创建内容管理相关的工具：
- 内容审批工作流
- 内容导出 API
- 内容统计服务
- 内容同步工具

## 📝 学习建议

### 1. 理论与实践结合
- 每学习一个概念，立即动手实践
- 修改代码，观察结果变化
- 遇到问题，查阅官方文档

### 2. 代码示例学习法
- 仔细阅读每个代码示例
- 理解每行代码的作用
- 尝试修改和扩展示例

### 3. 构建知识体系
- 理解各个概念之间的关系
- 绘制架构图
- 总结笔记

### 4. 持续练习
- 每天至少 1-2 小时编码
- 完成所有练习项目
- 尝试解决实际问题

## 🔍 学习资源

### 官方文档
- [Adobe Experience Manager 文档](https://experienceleague.adobe.com/docs/experience-manager.html)
- [Apache Sling 文档](https://sling.apache.org/)
- [OSGi 规范](https://www.osgi.org/)

### 社区资源
- Adobe Experience League 社区
- Stack Overflow (标签：aem, adobe-experience-manager)
- GitHub (搜索 AEM 相关项目)

## ✅ 学习检查清单

### 基础技能
- [ ] 能够创建和管理 AEM 项目
- [ ] 理解 JCR 节点结构
- [ ] 能够编写 Sling Servlet
- [ ] 能够创建基本组件

### 中级技能
- [ ] 熟练使用 HTL 模板语言
- [ ] 能够创建和使用 Sling Models
- [ ] 能够创建 OSGi 服务
- [ ] 能够配置组件对话框

### 高级技能
- [ ] 能够创建复杂的工作流
- [ ] 能够提供 REST API
- [ ] 能够优化性能
- [ ] 能够进行单元测试

## 🎓 进阶学习

完成基础学习后，可以继续学习：

1. **AEM Forms**
   - 表单开发
   - 表单数据处理
   - 表单验证

2. **AEM Sites**
   - 多站点管理 (MSM)
   - 内容片段
   - 体验片段

3. **AEM Assets**
   - 数字资产管理
   - 资产处理工作流
   - 资产 API

4. **性能优化**
   - 缓存策略
   - 查询优化
   - 资源优化

5. **测试和调试**
   - 单元测试
   - 集成测试
   - 调试技巧

## 💡 常见问题

### Q: 如何快速搭建 AEM 开发环境？
A: 参考 `01-basics/01-introduction.md` 中的环境设置部分。

### Q: HTL 和 JSP 有什么区别？
A: HTL 是 AEM 推荐的模板语言，更安全、更简洁。JSP 是旧的模板技术，不推荐新项目使用。

### Q: 如何调试 AEM 代码？
A: 可以使用日志、断点调试、AEM 开发者工具等方式。

### Q: 如何学习 AEM 的最佳实践？
A: 参考 Adobe 官方文档、代码示例、社区讨论等资源。

---

祝学习顺利！如有问题，请参考文档或查阅相关资源。🚀

