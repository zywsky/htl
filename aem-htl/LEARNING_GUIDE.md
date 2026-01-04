# HTL 学习指南

## 如何开始学习

### 第一步：了解 HTL 基础概念

1. 阅读 `README.md` 了解 HTL 是什么
2. 理解 HTL 的核心特点和安全机制

### 第二步：按顺序学习基础语法

从 `01-basics/` 目录开始，按照以下顺序学习：

1. **01-expressions.html** - 理解表达式语法
2. **02-conditional-rendering.html** - 掌握条件渲染
3. **03-loops.html** - 学会使用循环
4. **04-attributes.html** - 理解属性操作
5. **05-variables.html** - 掌握变量使用

**学习时间**: 每个文件建议 1-2 小时，共 5-10 小时

### 第三步：学习组件开发

进入 `02-components/` 目录：

1. **01-component-structure.html** - 理解 AEM 组件结构
2. **02-resource-include.html** - 掌握组件包含
3. **03-data-passing.html** - 理解数据传递

**学习时间**: 每个文件建议 2-3 小时，共 6-9 小时

### 第四步：掌握 Use API

进入 `03-use-api/` 目录：

1. **01-use-api-basics.html** - Use API 基础
2. **02-sling-models.html** - Sling Models 深入

**学习时间**: 每个文件建议 3-4 小时，共 6-8 小时

**重要**: 这部分需要 Java 知识，建议同时学习 Sling Models 的 Java 实现

### 第五步：学习高级特性

进入 `04-advanced/` 目录：

1. **01-templates.html** - 模板系统
2. **02-clientlibs.html** - 客户端库
3. **03-internationalization.html** - 国际化

**学习时间**: 每个文件建议 2-3 小时，共 6-9 小时

### 第六步：实践完整项目

查看 `05-complete-project/` 目录：

1. **01-card-component.html** - 分析完整组件实现
2. **02-product-list-component.html** - 学习复杂组件
3. **03-best-practices.md** - 掌握最佳实践
4. **04-sling-model-example.java** - 理解 Sling Model 实现

**学习时间**: 建议 4-6 小时

## 学习建议

### 1. 实践为主

- 不要只看代码，要动手实践
- 在 AEM 环境中创建实际组件
- 修改示例代码，观察效果

### 2. 循序渐进

- 不要跳过基础章节
- 每个概念都要理解透彻
- 遇到问题及时查阅文档

### 3. 结合项目

- 在学习过程中思考实际应用场景
- 尝试将所学应用到实际项目中
- 总结常见模式和最佳实践

### 4. 查阅参考

- 使用 `QUICK_REFERENCE.md` 作为快速参考
- 遇到问题先查阅参考手册
- 理解每个指令的用法和注意事项

## 学习路径总览

```
开始
  ↓
阶段 1: 基础语法 (5个文件, 5-10小时)
  ├─ 表达式和变量
  ├─ 条件渲染
  ├─ 循环
  ├─ 属性操作
  └─ 变量定义
  ↓
阶段 2: 组件开发 (3个文件, 6-9小时)
  ├─ 组件结构
  ├─ Resource Include
  └─ 数据传递
  ↓
阶段 3: Use API (2个文件, 6-8小时)
  ├─ Use API 基础
  └─ Sling Models
  ↓
阶段 4: 高级特性 (3个文件, 6-9小时)
  ├─ 模板系统
  ├─ 客户端库
  └─ 国际化
  ↓
阶段 5: 完整项目 (4个文件, 4-6小时)
  ├─ 卡片组件示例
  ├─ 产品列表组件
  ├─ 最佳实践
  └─ Sling Model 示例
  ↓
完成 (总计: 27-42小时)
```

## 检验学习成果

完成学习后，你应该能够：

1. ✅ 理解 HTL 语法和表达式
2. ✅ 使用 HTL 指令创建组件
3. ✅ 理解 AEM 组件结构
4. ✅ 使用 Sling Models 处理业务逻辑
5. ✅ 创建可重用的模板
6. ✅ 管理客户端库
7. ✅ 实现国际化
8. ✅ 遵循最佳实践开发组件
9. ✅ 调试和优化组件
10. ✅ 独立开发生产级组件

## 进阶学习

完成基础学习后，可以进一步学习：

1. **AEM Core Components**: 研究官方核心组件
2. **Editable Templates**: 学习可编辑模板
3. **Experience Fragments**: 体验片段
4. **Content Fragments**: 内容片段
5. **SPA Editor**: SPA 编辑器集成
6. **Headless**: 无头 CMS 开发

## 资源推荐

- Adobe AEM 官方文档
- AEM 社区论坛
- Sling Models 文档
- HTL 规范文档

## 常见问题

### Q: 需要 Java 知识吗？
A: 基础学习不需要，但学习 Use API 和 Sling Models 时需要 Java 知识。

### Q: 需要 AEM 环境吗？
A: 是的，建议在 AEM 环境中实践。可以使用 AEM SDK 本地运行。

### Q: 学习周期多长？
A: 按照本指南，大约需要 27-42 小时。根据个人基础和学习速度调整。

### Q: 如何验证学习效果？
A: 尝试独立开发一个完整的组件，包含对话框、Sling Model、样式和脚本。

## 开始你的学习之旅

现在就开始从 `01-basics/01-expressions.html` 开始学习吧！

记住：**实践是掌握 HTL 的关键！**

