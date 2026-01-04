# 第六章：响应式设计

## 什么是响应式设计？

响应式设计（Responsive Design）是指网页能够根据不同的设备（手机、平板、电脑）自动调整布局和样式，提供最佳的用户体验。

## 为什么需要响应式设计？

- **设备多样性**：手机、平板、电脑屏幕尺寸差异巨大
- **用户体验**：在不同设备上都能良好显示
- **SEO优化**：搜索引擎偏好响应式网站
- **维护成本**：一个网站适配所有设备

## 视口（Viewport）

### 设置视口

在HTML的 `<head>` 中添加：

```html
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```

**说明：**
- `width=device-width`：视口宽度等于设备宽度
- `initial-scale=1.0`：初始缩放比例为1

### 视口单位

- `vw`：视口宽度的1%（Viewport Width）
- `vh`：视口高度的1%（Viewport Height）
- `vmin`：视口宽度和高度中较小的1%
- `vmax`：视口宽度和高度中较大的1%

```css
.full-width {
    width: 100vw; /* 占满整个视口宽度 */
}

.full-height {
    height: 100vh; /* 占满整个视口高度 */
}
```

## 媒体查询（Media Queries）

媒体查询是响应式设计的核心，根据设备特性应用不同的样式。

### 基本语法

```css
@media (条件) {
    /* 样式 */
}
```

### 常用媒体查询

#### 1. 屏幕宽度

```css
/* 手机 */
@media (max-width: 768px) {
    .container {
        width: 100%;
        padding: 10px;
    }
}

/* 平板 */
@media (min-width: 769px) and (max-width: 1024px) {
    .container {
        width: 750px;
    }
}

/* 电脑 */
@media (min-width: 1025px) {
    .container {
        width: 1200px;
    }
}
```

#### 2. 设备方向

```css
/* 横屏 */
@media (orientation: landscape) {
    .sidebar {
        width: 300px;
    }
}

/* 竖屏 */
@media (orientation: portrait) {
    .sidebar {
        width: 100%;
    }
}
```

#### 3. 屏幕分辨率

```css
/* 高分辨率屏幕 */
@media (min-resolution: 2dppx) {
    .logo {
        background-image: url('logo@2x.png');
    }
}
```

### 断点（Breakpoints）

常见的断点设置：

```css
/* 超小屏幕（手机） */
@media (max-width: 575px) { }

/* 小屏幕（大手机） */
@media (min-width: 576px) and (max-width: 767px) { }

/* 中等屏幕（平板） */
@media (min-width: 768px) and (max-width: 991px) { }

/* 大屏幕（小电脑） */
@media (min-width: 992px) and (max-width: 1199px) { }

/* 超大屏幕（大电脑） */
@media (min-width: 1200px) { }
```

### 移动优先（Mobile First）

先为移动设备设计，然后逐步增强：

```css
/* 移动设备样式（默认） */
.container {
    width: 100%;
    padding: 10px;
}

/* 平板及以上 */
@media (min-width: 768px) {
    .container {
        width: 750px;
        padding: 20px;
    }
}

/* 电脑及以上 */
@media (min-width: 1024px) {
    .container {
        width: 1200px;
        padding: 30px;
    }
}
```

## 响应式布局技巧

### 1. 弹性图片

```css
img {
    max-width: 100%;
    height: auto; /* 保持宽高比 */
}
```

### 2. 响应式字体

```css
/* 使用相对单位 */
body {
    font-size: 16px;
}

h1 {
    font-size: 2rem; /* 相对于根元素 */
}

/* 使用媒体查询 */
@media (max-width: 768px) {
    body {
        font-size: 14px;
    }
    
    h1 {
        font-size: 1.5rem;
    }
}
```

### 3. 响应式Grid

```css
.grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
}
```

### 4. 响应式Flexbox

```css
.container {
    display: flex;
    flex-wrap: wrap;
}

.item {
    flex: 1 1 300px; /* 最小宽度300px */
}

@media (max-width: 768px) {
    .item {
        flex: 1 1 100%; /* 移动设备上占满一行 */
    }
}
```

### 5. 隐藏/显示元素

```css
/* 移动设备隐藏 */
.mobile-hidden {
    display: none;
}

@media (min-width: 768px) {
    .mobile-hidden {
        display: block;
    }
    
    .desktop-hidden {
        display: none;
    }
}
```

## 响应式设计模式

### 1. 列折叠（Column Drop）

多列布局在小屏幕上垂直堆叠：

```css
.container {
    display: flex;
    flex-wrap: wrap;
}

.sidebar {
    flex: 1 1 200px;
}

.main {
    flex: 1 1 400px;
}

@media (max-width: 768px) {
    .sidebar,
    .main {
        flex: 1 1 100%;
    }
}
```

### 2. 布局切换（Layout Shifter）

在不同屏幕尺寸下使用完全不同的布局：

```css
.mobile-layout {
    display: block;
}

.desktop-layout {
    display: none;
}

@media (min-width: 1024px) {
    .mobile-layout {
        display: none;
    }
    
    .desktop-layout {
        display: grid;
        grid-template-columns: 200px 1fr;
    }
}
```

### 3. 微调（Tiny Tweaks）

只调整字体大小、间距等细节：

```css
.container {
    padding: 20px;
    font-size: 16px;
}

@media (max-width: 768px) {
    .container {
        padding: 10px;
        font-size: 14px;
    }
}
```

## 实际应用示例

### 响应式导航栏

```css
.nav {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.nav-links {
    display: flex;
    gap: 20px;
}

.menu-toggle {
    display: none;
}

@media (max-width: 768px) {
    .nav-links {
        display: none;
        flex-direction: column;
        position: absolute;
        top: 100%;
        left: 0;
        width: 100%;
        background: white;
    }
    
    .menu-toggle {
        display: block;
    }
}
```

### 响应式卡片

```css
.card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
}

@media (max-width: 768px) {
    .card-grid {
        grid-template-columns: 1fr;
    }
}
```

## 测试响应式设计

### 1. 浏览器开发者工具

- 打开开发者工具（F12）
- 点击设备工具栏图标
- 选择不同设备或自定义尺寸

### 2. 真实设备测试

在不同设备上实际访问网站测试。

### 3. 在线工具

- Responsive Design Checker
- BrowserStack

## 最佳实践

1. **移动优先**：先设计移动端，再逐步增强
2. **使用相对单位**：rem、em、%、vw、vh
3. **弹性图片**：`max-width: 100%`
4. **合理断点**：根据内容需要设置，不要过多
5. **测试**：在不同设备上测试
6. **性能**：避免在小屏幕上加载大图片

## 下一步

掌握了响应式设计后，接下来学习CSS动画和过渡，让页面更加生动！

