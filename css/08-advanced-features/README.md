# 第八章：CSS高级特性

## CSS变量（Custom Properties）

CSS变量是CSS的强大功能，允许你定义可重用的值。

### 定义变量

```css
:root {
    --primary-color: #2196F3;
    --secondary-color: #4CAF50;
    --font-size-large: 24px;
    --spacing-unit: 20px;
}
```

**说明：**
- 变量名以 `--` 开头
- 通常在 `:root` 中定义（全局作用域）
- 也可以在特定选择器中定义（局部作用域）

### 使用变量

```css
.button {
    background-color: var(--primary-color);
    font-size: var(--font-size-large);
    margin: var(--spacing-unit);
}
```

### 变量作用域

```css
:root {
    --color: blue; /* 全局变量 */
}

.container {
    --color: red; /* 局部变量，只在这个容器内有效 */
}

.element {
    color: var(--color); /* 使用最近的变量 */
}
```

### 默认值

```css
.element {
    color: var(--color, blue); /* 如果 --color 不存在，使用 blue */
}
```

### 动态改变主题

```css
:root {
    --bg-color: white;
    --text-color: black;
}

[data-theme="dark"] {
    --bg-color: black;
    --text-color: white;
}

body {
    background-color: var(--bg-color);
    color: var(--text-color);
}
```

## CSS函数

### calc() - 计算

```css
.element {
    width: calc(100% - 50px);
    height: calc(100vh - 100px);
    margin: calc(20px + 10px);
}
```

### min() / max() - 最小值/最大值

```css
.element {
    width: min(100%, 800px); /* 取较小值 */
    width: max(300px, 50%);  /* 取较大值 */
}
```

### clamp() - 限制范围

```css
.element {
    font-size: clamp(16px, 4vw, 24px); /* 最小16px，理想4vw，最大24px */
}
```

### var() - 变量

```css
.element {
    color: var(--primary-color);
}
```

### url() - 资源路径

```css
.element {
    background-image: url('image.jpg');
}
```

### linear-gradient() - 线性渐变

```css
.element {
    background: linear-gradient(to right, red, blue);
    background: linear-gradient(45deg, red, blue);
    background: linear-gradient(to right, red 0%, blue 50%, green 100%);
}
```

### radial-gradient() - 径向渐变

```css
.element {
    background: radial-gradient(circle, red, blue);
    background: radial-gradient(ellipse at center, red, blue);
}
```

## 伪元素（Pseudo-elements）

伪元素用于创建不在HTML中的元素。

### ::before 和 ::after

```css
.element::before {
    content: "前置内容";
    /* 其他样式 */
}

.element::after {
    content: "后置内容";
    /* 其他样式 */
}
```

**重要：** `content` 属性是必需的，即使是空字符串 `content: "";`

### 实际应用

#### 1. 装饰性元素

```css
.title::before {
    content: "";
    display: block;
    width: 50px;
    height: 3px;
    background-color: blue;
    margin-bottom: 10px;
}
```

#### 2. 清除浮动

```css
.clearfix::after {
    content: "";
    display: table;
    clear: both;
}
```

#### 3. 图标

```css
.icon::before {
    content: "★";
    margin-right: 5px;
}
```

### ::first-line 和 ::first-letter

```css
p::first-line {
    font-weight: bold;
    color: blue;
}

p::first-letter {
    font-size: 2em;
    float: left;
    margin-right: 5px;
}
```

### ::selection

```css
::selection {
    background-color: yellow;
    color: black;
}
```

## 伪类进阶

### :focus 和 :focus-within

```css
.input:focus {
    outline: 2px solid blue;
}

.form:focus-within {
    background-color: #f0f0f0;
}
```

### :not() - 否定选择器

```css
p:not(.special) {
    color: gray;
}

div:not(:first-child) {
    margin-top: 20px;
}
```

### :nth-child() 进阶

```css
li:nth-child(3n) { }        /* 每3个 */
li:nth-child(3n+1) { }     /* 1, 4, 7, 10... */
li:nth-child(odd) { }      /* 奇数 */
li:nth-child(even) { }     /* 偶数 */
li:nth-child(-n+3) { }     /* 前3个 */
```

## 滤镜（Filter）

### blur() - 模糊

```css
.element {
    filter: blur(5px);
}
```

### brightness() - 亮度

```css
.element {
    filter: brightness(1.5); /* 1.5倍亮度 */
}
```

### contrast() - 对比度

```css
.element {
    filter: contrast(1.2);
}
```

### grayscale() - 灰度

```css
.element {
    filter: grayscale(100%);
}
```

### 组合使用

```css
.element {
    filter: blur(2px) brightness(1.2) contrast(1.1);
}
```

## 混合模式（Blend Modes）

```css
.element {
    background-image: url('image.jpg');
    background-color: blue;
    background-blend-mode: multiply;
}

.overlay {
    mix-blend-mode: overlay;
}
```

**常用模式：**
- `multiply`：正片叠底
- `screen`：滤色
- `overlay`：叠加
- `darken`：变暗
- `lighten`：变亮

## 遮罩（Mask）

```css
.element {
    mask-image: url('mask.png');
    mask-size: cover;
    mask-position: center;
}
```

## 裁剪（Clip-path）

```css
.element {
    clip-path: circle(50%);
    clip-path: polygon(0 0, 100% 0, 50% 100%);
    clip-path: inset(10% 20%);
}
```

## 多列布局（Multi-column）

```css
.text {
    column-count: 3;
    column-gap: 20px;
    column-rule: 1px solid #ddd;
}
```

## 滚动行为（Scroll Behavior）

```css
html {
    scroll-behavior: smooth;
}
```

## 实际应用示例

### 1. 主题切换

```css
:root {
    --bg-color: white;
    --text-color: black;
}

[data-theme="dark"] {
    --bg-color: #1a1a1a;
    --text-color: white;
}

body {
    background-color: var(--bg-color);
    color: var(--text-color);
    transition: background-color 0.3s, color 0.3s;
}
```

### 2. 响应式字体

```css
h1 {
    font-size: clamp(24px, 5vw, 48px);
}
```

### 3. 卡片悬停效果

```css
.card {
    position: relative;
    overflow: hidden;
}

.card::before {
    content: "";
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
    transition: left 0.5s;
}

.card:hover::before {
    left: 100%;
}
```

### 4. 自定义滚动条

```css
::-webkit-scrollbar {
    width: 10px;
}

::-webkit-scrollbar-track {
    background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
    background: #888;
    border-radius: 5px;
}

::-webkit-scrollbar-thumb:hover {
    background: #555;
}
```

## 性能优化

### 1. 使用CSS变量

减少重复代码，提高可维护性。

### 2. 使用transform代替位置属性

```css
/* 好的做法 */
.element {
    transform: translateX(100px);
}

/* 避免 */
.element {
    left: 100px; /* 会触发重排 */
}
```

### 3. 使用will-change

```css
.element {
    will-change: transform;
}
```

## 浏览器兼容性

- 使用 [Can I Use](https://caniuse.com/) 检查兼容性
- 使用 Autoprefixer 自动添加前缀
- 提供降级方案

## 下一步

恭喜！你已经完成了CSS的全面学习。现在你已经掌握了：

✅ HTML基础  
✅ CSS基础语法和选择器  
✅ 盒模型  
✅ Flexbox和Grid布局  
✅ 响应式设计  
✅ 动画和过渡  
✅ 高级特性  

继续实践，创建自己的项目，不断探索CSS的更多可能性！

