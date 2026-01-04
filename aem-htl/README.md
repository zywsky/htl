# Adobe AEM HTL 完整学习指南

## 学习路径概览

本指南将带你从零开始，系统性地学习 Adobe AEM 的 HTL (HTML Template Language) 知识，最终达到专家水平。

## 什么是 HTL？

HTL (HTML Template Language) 是 Adobe Experience Manager (AEM) 的模板语言，用于创建组件和页面模板。它是 AEM 推荐的客户端模板语言，替代了之前的 JSP 技术。

### HTL 的核心特点

1. **安全**: 自动转义，防止 XSS 攻击
2. **简洁**: 语法简单，易读易写
3. **声明式**: 使用 HTML5 data 属性
4. **类型安全**: 支持 Java Use API
5. **性能优化**: 支持客户端模板和服务器端渲染

## 学习路径

### 阶段 1: 基础语法 (01-basics/)
**学习目标**: 掌握 HTL 的核心语法和基本操作

1. **01-expressions.html** - HTL 表达式和变量
   - 表达式语法 `${}`
   - 变量引用和属性访问
   - 表达式上下文（properties, currentPage, resource 等）
   - 运算符和字符串操作
   - 自动转义和安全

2. **02-conditional-rendering.html** - 条件渲染
   - `data-sly-test` 指令
   - 逻辑运算符（&&, ||, !）
   - 条件表达式和三元运算符
   - 嵌套条件

3. **03-loops.html** - 循环
   - `data-sly-list` 和 `data-sly-repeat`
   - 循环变量（item, itemList）
   - 嵌套循环
   - 列表过滤和空列表处理

4. **04-attributes.html** - 属性操作
   - `data-sly-attribute` 指令
   - 动态属性设置
   - `data-sly-element` 动态元素
   - `data-sly-text` 文本设置
   - `data-sly-unescape` 使用（谨慎）

5. **05-variables.html** - 变量定义
   - `data-sly-set` 指令
   - 变量作用域
   - 变量和性能优化
   - 变量组合使用

**练习建议**: 
- 完成每个示例的理解
- 尝试修改示例代码
- 创建自己的简单组件

### 阶段 2: 组件开发 (02-components/)
**学习目标**: 理解 AEM 组件的结构和开发方式

1. **01-component-structure.html** - 组件结构
   - AEM 组件文件结构
   - 组件属性访问（properties）
   - component 对象使用
   - 组件样式类（component.cssClassNames）
   - 空状态处理

2. **02-resource-include.html** - Resource Include
   - `data-sly-resource` 指令
   - 组件嵌套和组合
   - parsys 使用
   - 参数传递
   - 条件包含

3. **03-data-passing.html** - 数据传递
   - 通过 properties 传递
   - 通过参数传递
   - 对象和数组传递
   - 多级数据传递
   - 数据验证和转换

**练习建议**:
- 创建自己的组件结构
- 实践组件嵌套
- 理解数据流

### 阶段 3: Use API (03-use-api/)
**学习目标**: 掌握 Use API 和 Sling Models 的使用

1. **01-use-api-basics.html** - Use API 基础
   - `data-sly-use` 指令
   - Java Use API 对象
   - 参数传递
   - 业务逻辑处理
   - 依赖注入基础

2. **02-sling-models.html** - Sling Models 深入
   - Sling Models 注解
   - 从 Resource 和 Request 适配
   - 依赖注入详解
   - OSGi 服务注入
   - 实际应用示例

**练习建议**:
- 创建简单的 Sling Model
- 实践依赖注入
- 将业务逻辑从模板移到 Model

### 阶段 4: 高级特性 (04-advanced/)
**学习目标**: 掌握 HTL 的高级特性和最佳实践

1. **01-templates.html** - 模板系统
   - `data-sly-template` 定义模板
   - `data-sly-call` 调用模板
   - 模板参数和默认值
   - 模板嵌套
   - 代码组织和重用

2. **02-clientlibs.html** - 客户端库
   - Client Libraries 概念
   - CSS 和 JS 引入
   - async 和 defer
   - 条件加载
   - 性能优化

3. **03-internationalization.html** - 国际化
   - i18n API 使用
   - 翻译文本获取
   - 日期和数字格式化
   - 多语言支持
   - RTL 语言处理

**练习建议**:
- 创建可重用模板
- 组织客户端库
- 实现多语言支持

### 阶段 5: 完整项目示例 (05-complete-project/)
**学习目标**: 综合运用所学知识，开发生产级组件

1. **01-card-component.html** - 卡片组件
   - 完整的组件实现
   - Sling Model 集成
   - 客户端库使用
   - 最佳实践应用

2. **02-product-list-component.html** - 产品列表组件
   - 复杂组件实现
   - 列表和循环
   - 过滤和分页
   - 交互功能

3. **03-best-practices.md** - 最佳实践
   - 代码组织
   - 性能优化
   - 可访问性
   - 安全性
   - 测试和文档

4. **04-sling-model-example.java** - Sling Model 示例
   - 完整的 Model 接口定义
   - 注解使用
   - 方法实现示例

**练习建议**:
- 参考示例创建自己的组件
- 应用最佳实践
- 进行代码审查
- 编写测试

### 阶段 6: 故障排除 (06-troubleshooting/)
**学习目标**: 掌握调试技巧和问题解决方法

1. **01-debugging.html** - 调试技巧
   - 开发者模式使用
   - 变量输出调试
   - 错误处理模式
   - 性能调试
   - 常见调试场景

2. **02-common-issues.md** - 常见问题
   - 属性访问问题
   - 循环问题
   - 条件渲染问题
   - Use API 问题
   - 性能问题
   - 完整的解决方案

**练习建议**:
- 在实际项目中应用调试技巧
- 记录遇到的问题和解决方案
- 建立问题知识库

### 阶段 7: AEM 功能集成 (07-integration/)
**学习目标**: 学习 HTL 与 AEM 其他功能的集成

1. **01-editable-templates.html** - 可编辑模板
   - 模板结构定义
   - 结构组件使用
   - 容器组件使用
   - 策略配置
   - 响应式布局

2. **02-experience-fragments.html** - 体验片段
   - 体验片段嵌入
   - 变体支持
   - 响应式体验片段
   - 与组件结合

3. **03-content-fragments.html** - 内容片段
   - 内容片段模型使用
   - 元素访问
   - 内容片段列表
   - 变体和引用
   - 与体验片段结合

**练习建议**:
- 创建可编辑模板
- 使用体验片段和内容片段
- 理解 AEM 架构

### 阶段 8: 迁移指南 (08-migration/)
**学习目标**: 从其他技术迁移到 HTL

1. **jsp-to-htl.md** - JSP 到 HTL 迁移
   - 语法差异对比
   - 迁移步骤
   - 常见迁移模式
   - 迁移检查清单
   - 最佳实践

**练习建议**:
- 如果有旧项目，尝试迁移
- 理解迁移策略
- 学习迁移工具

### 阶段 9: 高级主题 (09-advanced-topics/)
**学习目标**: 深入学习高级主题和优化技巧

1. **01-performance-optimization.md** - 性能优化
   - 表达式优化
   - 缓存策略
   - 循环优化
   - 资源包含优化
   - 客户端库优化
   - 性能监控
   - 完整的优化指南

2. **02-accessibility.html** - 无障碍性
   - 语义化 HTML
   - ARIA 属性使用
   - 键盘导航支持
   - 屏幕阅读器支持
   - 表单可访问性
   - 完整的无障碍指南

3. **03-testing-strategies.md** - 测试策略
   - 单元测试
   - 集成测试
   - 功能测试
   - 可访问性测试
   - 测试工具和框架
   - 测试最佳实践

4. **04-seo-optimization.html** - SEO 优化
   - 语义化 HTML
   - Meta 标签
   - 结构化数据
   - 图片 SEO
   - 内容优化
   - 完整的 SEO 指南

5. **05-responsive-design.html** - 响应式设计
   - 响应式图片
   - 响应式布局
   - 响应式导航
   - 响应式表格
   - 移动优先设计
   - 完整的响应式指南

**练习建议**:
- 在实际项目中应用优化技巧
- 测量和监控性能
- 建立性能基准
- 进行无障碍性测试
- 实施 SEO 最佳实践

### 阶段 10: 代码审查 (10-code-review/)
**学习目标**: 学习如何进行有效的代码审查

1. **01-review-checklist.md** - 代码审查清单
   - 基础检查项
   - HTL 特定检查
   - 最佳实践检查
   - 常见问题
   - 审查流程

**练习建议**:
- 使用清单审查代码
- 参与代码审查
- 学习审查技巧

### 阶段 11: 设计模式 (11-patterns/)
**学习目标**: 学习常用的设计模式和组件模式

1. **01-design-patterns.md** - 设计模式
   - 容器/展示模式
   - 组合模式
   - 策略模式
   - 工厂模式
   - 适配器模式
   - 其他常用模式

2. **02-component-patterns.html** - 组件模式
   - 空状态模式
   - 加载状态模式
   - 错误处理模式
   - 分页模式
   - 标签页模式
   - 其他常用组件模式

**练习建议**:
- 在实际项目中应用模式
- 识别可以使用模式的地方
- 重构代码应用模式

### 阶段 12: 实用指南 (12-practical-guides/)
**学习目标**: 学习实际项目开发中的实用技巧和案例

1. **01-component-dialog.md** - 组件对话框开发
   - 对话框类型
   - 属性访问
   - 字段类型
   - 验证处理
   - 实际应用示例

2. **02-real-world-examples.md** - 实际项目案例
   - 企业新闻组件
   - 产品展示组件
   - 动态内容列表组件
   - 表单组件
   - 完整实现示例

3. **03-security-best-practices.md** - 安全最佳实践
   - XSS 防护
   - 输入验证
   - 路径遍历防护
   - CSRF 防护
   - 权限检查
   - 安全配置
   - 安全检查清单

4. **04-common-pitfalls.md** - 常见陷阱和错误
   - 表达式陷阱
   - 循环陷阱
   - 条件渲染陷阱
   - 属性操作陷阱
   - Use API 陷阱
   - 性能陷阱
   - 如何避免陷阱

**练习建议**:
- 参考案例实现自己的组件
- 理解实际项目需求
- 应用所学知识解决实际问题
- 遵循安全最佳实践
- 避免常见陷阱

## 快速参考

查看 **QUICK_REFERENCE.md** 获取常用语法和模式的快速参考。

## 目录结构

```
.
├── 01-basics/           # 基础语法示例
├── 02-components/       # 组件开发
├── 03-use-api/          # Use API
├── 04-advanced/         # 高级特性
├── 05-complete-project/ # 完整项目示例
├── 06-troubleshooting/  # 故障排除和调试
├── 07-integration/      # AEM 功能集成
├── 08-migration/        # 迁移指南
├── 09-advanced-topics/  # 高级主题
├── 10-code-review/      # 代码审查
├── 11-patterns/         # 设计模式
└── 12-practical-guides/ # 实用指南
```

## 开始学习

1. **新手指南**: 查看 `LEARNING_GUIDE.md` 获取详细的学习步骤和时间规划
2. **快速参考**: 查看 `QUICK_REFERENCE.md` 快速查找常用语法
3. **按顺序学习**: 按照数字顺序学习每个阶段的内容，每个文件都包含详细的代码注释和解释

## 学习资源

- 📖 **学习指南** (`LEARNING_GUIDE.md`): 详细的学习路径和步骤
- 📝 **快速参考** (`QUICK_REFERENCE.md`): 常用语法快速查找
- 💡 **最佳实践** (`05-complete-project/03-best-practices.md`): 开发最佳实践
- 🐛 **故障排除** (`06-troubleshooting/`): 调试技巧和常见问题解决方案
- ⚡ **性能优化** (`09-advanced-topics/01-performance-optimization.md`): 完整的性能优化指南
- ♿ **无障碍性** (`09-advanced-topics/02-accessibility.html`): 无障碍访问完整指南
- 🧪 **测试策略** (`09-advanced-topics/03-testing-strategies.md`): 测试方法和最佳实践
- 🔍 **SEO 优化** (`09-advanced-topics/04-seo-optimization.html`): SEO 优化完整指南
- 📱 **响应式设计** (`09-advanced-topics/05-responsive-design.html`): 响应式设计完整指南
- 🎨 **设计模式** (`11-patterns/`): 常用设计模式和组件模式
- 📋 **组件对话框** (`12-practical-guides/01-component-dialog.md`): 对话框开发指南
- 💼 **实际案例** (`12-practical-guides/02-real-world-examples.md`): 真实项目案例
- 🔒 **安全实践** (`12-practical-guides/03-security-best-practices.md`): 安全最佳实践
- ⚠️ **常见陷阱** (`12-practical-guides/04-common-pitfalls.md`): 常见陷阱和错误避免
- ✅ **代码审查** (`10-code-review/01-review-checklist.md`): 代码审查清单和流程
- 🔄 **迁移指南** (`08-migration/jsp-to-htl.md`): 从 JSP 迁移到 HTL
- 🔧 **示例代码**: 每个目录都包含详细的代码示例和注释

## 学习成果

完成本学习路径后，你将能够：

✅ 熟练使用 HTL 语法和指令  
✅ 开发完整的 AEM 组件  
✅ 使用 Sling Models 处理业务逻辑  
✅ 实现组件嵌套和数据传递  
✅ 应用最佳实践优化代码  
✅ 调试和解决常见问题  
✅ 集成 AEM 高级功能（模板、片段等）  
✅ 性能优化和监控  
✅ 实现无障碍访问支持  
✅ 编写和执行测试  
✅ SEO 优化  
✅ 响应式设计实现  
✅ 应用设计模式  
✅ 开发组件对话框  
✅ 参考实际项目案例  
✅ 遵循安全最佳实践  
✅ 避免常见陷阱和错误  
✅ 进行代码审查  
✅ 独立开发生产级组件  

**开始你的 HTL 学习之旅吧！** 🚀

