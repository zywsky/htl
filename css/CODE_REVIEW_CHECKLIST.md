# CSS代码审查清单

## ✅ 代码质量检查

### 1. 代码结构

- [ ] 代码组织清晰，有逻辑分组
- [ ] 使用注释分隔不同部分
- [ ] 文件结构合理（重置、变量、组件、工具类）
- [ ] 代码易于阅读和维护

**示例：**
```css
/* ============================================
   导航栏样式
   ============================================ */
.navbar { }

/* ============================================
   按钮样式
   ============================================ */
.button { }
```

---

### 2. 命名规范

- [ ] 使用语义化类名
- [ ] 命名清晰易懂
- [ ] 保持命名一致性
- [ ] 避免使用缩写（除非通用）

**好的命名：**
```css
.card-title { }
.nav-link { }
.button-primary { }
```

**避免的命名：**
```css
.c1 { }
.red-box { }
.big-text { }
```

---

### 3. CSS变量使用

- [ ] 使用CSS变量管理颜色和尺寸
- [ ] 变量命名清晰
- [ ] 在:root中定义全局变量
- [ ] 避免硬编码值

**检查：**
```css
:root {
    --primary-color: #2196F3;
    --spacing-md: 20px;
}

.button {
    background-color: var(--primary-color);
    padding: var(--spacing-md);
}
```

---

### 4. 选择器质量

- [ ] 避免过度嵌套（不超过3层）
- [ ] 使用高效的选择器
- [ ] 避免使用通配符*
- [ ] 选择器优先级合理

**检查：**
```css
/* 好的做法 */
.card-title { }

/* 避免 */
.container .wrapper .content .card .title { }
```

---

### 5. 代码复用

- [ ] 提取公共样式
- [ ] 使用工具类（适度）
- [ ] 避免重复代码
- [ ] 使用继承和组合

---

## 🎨 样式检查

### 6. 响应式设计

- [ ] 移动优先设计
- [ ] 使用相对单位（rem、em、%、vw、vh）
- [ ] 媒体查询合理
- [ ] 测试不同设备尺寸

**检查：**
```css
/* 移动优先 */
.container {
    padding: 10px;
}

@media (min-width: 768px) {
    .container {
        padding: 20px;
    }
}
```

---

### 7. 浏览器兼容性

- [ ] 检查Can I Use支持情况
- [ ] 使用Autoprefixer
- [ ] 提供降级方案
- [ ] 测试主流浏览器

**检查：**
```css
.element {
    display: flex;
}

@supports not (display: flex) {
    .element {
        display: table;
    }
}
```

---

### 8. 性能优化

- [ ] 使用transform代替位置属性
- [ ] 避免触发重排的属性
- [ ] 使用contain限制范围
- [ ] 优化选择器性能

**检查：**
```css
/* 好的做法 */
.element {
    transform: translateX(100px);
}

/* 避免 */
.element {
    left: 100px;  /* 触发重排 */
}
```

---

### 9. 动画性能

- [ ] 使用高性能属性（transform、opacity）
- [ ] 避免过度动画
- [ ] 使用will-change提示
- [ ] 尊重用户偏好（prefers-reduced-motion）

**检查：**
```css
@keyframes slide {
    from { transform: translateX(0); }
    to { transform: translateX(100px); }
}

@media (prefers-reduced-motion: reduce) {
    * {
        animation: none !important;
    }
}
```

---

## 📱 响应式检查

### 10. 移动端适配

- [ ] viewport设置正确
- [ ] 触摸目标足够大（至少44x44px）
- [ ] 文字大小可读（至少16px）
- [ ] 避免水平滚动

**检查：**
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```

---

### 11. 图片优化

- [ ] 图片响应式（max-width: 100%）
- [ ] 使用合适的图片格式
- [ ] 添加alt属性
- [ ] 考虑懒加载

**检查：**
```css
img {
    max-width: 100%;
    height: auto;
}
```

---

## 🔧 代码规范

### 12. 格式规范

- [ ] 一致的缩进（2空格或4空格）
- [ ] 一致的引号风格（单引号或双引号）
- [ ] 属性顺序合理
- [ ] 分号使用一致

**建议顺序：**
```css
.element {
    /* 定位 */
    position: relative;
    top: 0;
    left: 0;
    
    /* 盒模型 */
    display: block;
    width: 100%;
    padding: 20px;
    margin: 10px;
    
    /* 视觉 */
    background-color: white;
    border: 1px solid #ddd;
    border-radius: 5px;
    
    /* 文字 */
    font-size: 16px;
    color: #333;
    text-align: center;
    
    /* 其他 */
    transition: all 0.3s;
}
```

---

### 13. 注释质量

- [ ] 复杂代码有注释
- [ ] 注释清晰有用
- [ ] 使用注释分隔部分
- [ ] 避免无意义的注释

**好的注释：**
```css
/* 使用Flexbox实现完全居中 */
.center {
    display: flex;
    justify-content: center;
    align-items: center;
}
```

---

### 14. 代码简洁

- [ ] 避免不必要的代码
- [ ] 使用简写属性
- [ ] 合并相同的规则
- [ ] 删除未使用的样式

**检查：**
```css
/* 好的做法 */
.element {
    margin: 10px 20px;
}

/* 避免 */
.element {
    margin-top: 10px;
    margin-right: 20px;
    margin-bottom: 10px;
    margin-left: 20px;
}
```

---

## 🎯 功能检查

### 15. 功能完整性

- [ ] 所有功能正常工作
- [ ] 交互效果正常
- [ ] 动画流畅
- [ ] 表单验证正常

---

### 16. 可访问性

- [ ] 颜色对比度足够（WCAG AA标准）
- [ ] 键盘导航支持
- [ ] 焦点状态可见
- [ ] 语义化HTML

**检查：**
```css
.button:focus-visible {
    outline: 2px solid blue;
    outline-offset: 2px;
}
```

---

### 17. 错误处理

- [ ] 处理边界情况
- [ ] 提供降级方案
- [ ] 错误信息清晰
- [ ] 优雅降级

---

## 📊 性能检查

### 18. 文件大小

- [ ] CSS文件大小合理
- [ ] 删除未使用的样式
- [ ] 压缩生产环境代码
- [ ] 考虑代码分割

---

### 19. 加载性能

- [ ] 关键CSS内联
- [ ] 非关键CSS异步加载
- [ ] 减少HTTP请求
- [ ] 使用CDN（如适用）

---

### 20. 运行时性能

- [ ] 避免强制同步布局
- [ ] 减少重排和重绘
- [ ] 使用GPU加速
- [ ] 优化动画性能

---

## 🔍 测试检查

### 21. 浏览器测试

- [ ] Chrome测试
- [ ] Firefox测试
- [ ] Safari测试
- [ ] Edge测试
- [ ] 移动浏览器测试

---

### 22. 设备测试

- [ ] 手机测试
- [ ] 平板测试
- [ ] 桌面测试
- [ ] 不同分辨率测试

---

### 23. 功能测试

- [ ] 所有链接正常
- [ ] 表单提交正常
- [ ] 动画效果正常
- [ ] 交互响应正常

---

## 📝 文档检查

### 24. 代码文档

- [ ] README文件完整
- [ ] 代码注释清晰
- [ ] 使用说明清楚
- [ ] 更新日志记录

---

### 25. 版本控制

- [ ] 提交信息清晰
- [ ] 代码已提交
- [ ] 分支管理合理
- [ ] 代码审查完成

---

## ✅ 最终检查

### 提交前检查

- [ ] 所有检查项完成
- [ ] 代码可以运行
- [ ] 没有控制台错误
- [ ] 性能指标良好
- [ ] 代码已测试

---

## 💡 审查建议

1. **使用工具**
   - CSS Linter（Stylelint）
   - 浏览器开发者工具
   - 性能分析工具

2. **代码审查**
   - 自我审查
   - 同行审查
   - 使用检查清单

3. **持续改进**
   - 学习最佳实践
   - 关注代码质量
   - 定期重构

---

**记住：代码审查是提高代码质量的重要步骤！** 🔍

