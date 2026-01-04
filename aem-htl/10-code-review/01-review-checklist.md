# HTL 代码审查清单

## 代码审查概述

代码审查是确保代码质量和一致性的重要环节。本清单帮助审查 HTL 组件代码。

## 基础检查

### 1. 语法和格式

- [ ] HTL 语法正确
- [ ] 代码格式一致
- [ ] 缩进正确
- [ ] 没有语法错误
- [ ] 注释清晰有用

### 2. 命名规范

- [ ] 变量名有意义
- [ ] 组件名符合规范
- [ ] 类名和 ID 有意义
- [ ] 使用一致的命名约定

## HTL 特定检查

### 3. 表达式

- [ ] 表达式语法正确
- [ ] 使用默认值处理空值
- [ ] 避免复杂的嵌套表达式
- [ ] 使用可选链操作符（?.）

### 4. 条件渲染

- [ ] 正确使用 `data-sly-test`
- [ ] 避免不必要的条件检查
- [ ] 处理所有边界情况
- [ ] 空状态有适当处理

### 5. 循环

- [ ] 正确使用 `data-sly-list` 或 `data-sly-repeat`
- [ ] 在循环前检查列表是否存在
- [ ] 处理空列表情况
- [ ] 避免在循环中进行复杂计算

### 6. 变量

- [ ] 变量名有意义
- [ ] 变量作用域正确
- [ ] 避免重复计算
- [ ] 使用变量提高可读性

### 7. 属性操作

- [ ] 正确使用 `data-sly-attribute`
- [ ] 属性值安全
- [ ] 避免硬编码属性值
- [ ] 使用对象语法设置多个属性

## 组件结构检查

### 8. 组件组织

- [ ] 组件结构清晰
- [ ] 使用语义化 HTML
- [ ] 组件职责单一
- [ ] 代码组织合理

### 9. 资源包含

- [ ] 正确使用 `data-sly-resource`
- [ ] 资源类型正确
- [ ] 参数传递正确
- [ ] 避免过度嵌套

### 10. Use API

- [ ] 正确使用 `data-sly-use`
- [ ] Use API 类名正确
- [ ] 参数传递正确
- [ ] 错误处理适当

## 最佳实践检查

### 11. 性能

- [ ] 避免在模板中进行复杂计算
- [ ] 使用 Use API 预处理数据
- [ ] 优化循环
- [ ] 使用条件渲染减少 DOM

### 12. 安全性

- [ ] 避免使用 `data-sly-unescape`（除非必要）
- [ ] 验证用户输入
- [ ] 防止 XSS 攻击
- [ ] 安全地处理数据

### 13. 可访问性

- [ ] 使用语义化 HTML
- [ ] 提供 ARIA 属性
- [ ] 图片有 alt 文本
- [ ] 表单标签正确关联
- [ ] 键盘导航支持

### 14. SEO

- [ ] 使用正确的标题层次
- [ ] Meta 标签设置
- [ ] 结构化数据标记
- [ ] 图片 alt 文本有意义
- [ ] URL 结构友好

## 客户端库检查

### 15. CSS/JS 管理

- [ ] 正确使用客户端库
- [ ] CSS 和 JS 分离
- [ ] 使用 async/defer
- [ ] 条件加载适当

## 错误处理检查

### 16. 错误处理

- [ ] 处理空值情况
- [ ] 处理错误情况
- [ ] 提供有意义的错误消息
- [ ] 记录错误（如需要）

## 文档检查

### 17. 文档

- [ ] 代码有注释
- [ ] 复杂逻辑有解释
- [ ] 组件有使用说明
- [ ] 参数有文档

## 测试检查

### 18. 测试

- [ ] 有单元测试
- [ ] 有集成测试
- [ ] 测试覆盖主要功能
- [ ] 测试通过

## 代码审查流程

### 1. 准备审查

- 确保代码可以运行
- 运行所有测试
- 检查 lint 错误

### 2. 审查代码

- 按照清单逐项检查
- 记录问题和建议
- 提供具体的改进建议

### 3. 讨论和反馈

- 与开发者讨论问题
- 解释为什么需要修改
- 提供替代方案

### 4. 跟进

- 确保问题已解决
- 验证修改正确
- 批准合并

## 常见问题

### 问题 1: 过度复杂的表达式

**不好:**
```html
<div class="${properties.isActive && properties.isVisible && properties.isPublished ? 'active visible published' : 'inactive'}">
```

**好:**
```html
<sly data-sly-set.isActiveAndVisible="${properties.isActive && properties.isVisible}"></sly>
<div class="${isActiveAndVisible && properties.isPublished ? 'active visible published' : 'inactive'}">
```

### 问题 2: 缺少空值检查

**不好:**
```html
<div>${properties.title.value}</div>
```

**好:**
```html
<div>${properties.title?.value || '默认值'}</div>
```

### 问题 3: 在循环中计算

**不好:**
```html
<ul>
    <li data-sly-list="${properties.items}">
        ${model.expensiveCalculation(item)}
    </li>
</ul>
```

**好:**
```html
<sly data-sly-use.model="${'com.example.Model' @ items=properties.items}"></sly>
<ul>
    <li data-sly-list="${model.processedItems}">
        ${item.result}
    </li>
</ul>
```

## 审查工具

- **IDE 插件**: HTL 语法检查
- **Linters**: 代码质量检查
- **测试工具**: 自动化测试
- **代码审查工具**: GitHub, GitLab 等

## 总结

代码审查应该：
1. **建设性**: 提供有用的反馈
2. **具体**: 指出具体问题和位置
3. **教育性**: 解释为什么需要修改
4. **尊重**: 尊重开发者的工作
5. **及时**: 及时提供反馈

