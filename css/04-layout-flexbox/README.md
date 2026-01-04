# 第四章：Flexbox布局

## 什么是Flexbox？

Flexbox（Flexible Box Layout）是一种一维布局方法，用于在容器内排列元素。它非常适合处理**一行或一列**的布局。

## 为什么使用Flexbox？

- **简单易用**：几行代码就能实现复杂的布局
- **响应式**：自动适应不同屏幕尺寸
- **对齐简单**：轻松实现垂直和水平居中
- **灵活**：元素可以自动伸缩

## Flexbox的基本概念

### 容器（Container）和项目（Items）

```html
<div class="container">  <!-- 容器（Flex Container） -->
    <div class="item">1</div>  <!-- 项目（Flex Item） -->
    <div class="item">2</div>
    <div class="item">3</div>
</div>
```

```css
.container {
    display: flex; /* 将容器设置为flex布局 */
}
```

## 容器的属性

### 1. flex-direction（主轴方向）

决定项目的排列方向：

```css
.container {
    flex-direction: row;        /* 默认：从左到右 */
    flex-direction: row-reverse; /* 从右到左 */
    flex-direction: column;     /* 从上到下 */
    flex-direction: column-reverse; /* 从下到上 */
}
```

### 2. flex-wrap（换行）

决定项目是否换行：

```css
.container {
    flex-wrap: nowrap;  /* 默认：不换行，项目会压缩 */
    flex-wrap: wrap;    /* 换行，第一行在上方 */
    flex-wrap: wrap-reverse; /* 换行，第一行在下方 */
}
```

### 3. flex-flow（简写）

`flex-direction` 和 `flex-wrap` 的简写：

```css
.container {
    flex-flow: row wrap; /* 等同于 flex-direction: row; flex-wrap: wrap; */
}
```

### 4. justify-content（主轴对齐）

控制项目在**主轴**上的对齐方式：

```css
.container {
    justify-content: flex-start;    /* 默认：左对齐 */
    justify-content: flex-end;      /* 右对齐 */
    justify-content: center;        /* 居中 */
    justify-content: space-between; /* 两端对齐，项目之间间距相等 */
    justify-content: space-around;  /* 每个项目两侧间距相等 */
    justify-content: space-evenly;  /* 项目之间和两端间距都相等 */
}
```

### 5. align-items（交叉轴对齐）

控制项目在**交叉轴**上的对齐方式：

```css
.container {
    align-items: stretch;    /* 默认：拉伸填满容器 */
    align-items: flex-start;  /* 顶部对齐 */
    align-items: flex-end;    /* 底部对齐 */
    align-items: center;      /* 居中对齐 */
    align-items: baseline;    /* 基线对齐（文本基线） */
}
```

### 6. align-content（多行对齐）

当有多行时，控制**行**在交叉轴上的对齐：

```css
.container {
    align-content: stretch;      /* 默认：拉伸 */
    align-content: flex-start;   /* 顶部对齐 */
    align-content: flex-end;     /* 底部对齐 */
    align-content: center;       /* 居中 */
    align-content: space-between; /* 两端对齐 */
    align-content: space-around;  /* 每行两侧间距相等 */
}
```

## 项目的属性

### 1. flex-grow（放大比例）

定义项目的放大比例，默认为0（不放大）：

```css
.item {
    flex-grow: 0;  /* 默认：不放大 */
    flex-grow: 1;  /* 如果有剩余空间，会放大 */
}
```

**示例：**
```css
.item1 { flex-grow: 1; }  /* 占1份 */
.item2 { flex-grow: 2; }  /* 占2份（是item1的2倍） */
```

### 2. flex-shrink（缩小比例）

定义项目的缩小比例，默认为1（会缩小）：

```css
.item {
    flex-shrink: 1;  /* 默认：会缩小 */
    flex-shrink: 0;  /* 不缩小 */
}
```

### 3. flex-basis（初始大小）

定义项目在分配多余空间之前的初始大小：

```css
.item {
    flex-basis: auto;  /* 默认：项目本身的大小 */
    flex-basis: 200px; /* 初始宽度为200px */
}
```

### 4. flex（简写）

`flex-grow`、`flex-shrink`、`flex-basis` 的简写：

```css
.item {
    flex: 1;           /* 等同于 flex: 1 1 0%; */
    flex: 0 1 auto;    /* 等同于 flex-grow: 0; flex-shrink: 1; flex-basis: auto; */
    flex: 1 1 200px;   /* grow: 1, shrink: 1, basis: 200px */
}
```

**常用值：**
- `flex: 1`：自动填充剩余空间
- `flex: 0 0 auto`：不放大不缩小，保持原始大小
- `flex: none`：等同于 `0 0 auto`

### 5. align-self（单个项目对齐）

允许单个项目有与其他项目不一样的对齐方式：

```css
.item {
    align-self: auto;      /* 默认：继承父容器的align-items */
    align-self: flex-start; /* 顶部对齐 */
    align-self: flex-end;   /* 底部对齐 */
    align-self: center;     /* 居中 */
    align-self: stretch;    /* 拉伸 */
}
```

### 6. order（排序）

定义项目的排列顺序，数值越小越靠前：

```css
.item {
    order: 0;  /* 默认：0 */
    order: 1;  /* 排在order为0的项目后面 */
    order: -1; /* 排在order为0的项目前面 */
}
```

## 实际应用示例

### 1. 水平居中

```css
.container {
    display: flex;
    justify-content: center;
}
```

### 2. 垂直居中

```css
.container {
    display: flex;
    align-items: center;
}
```

### 3. 完全居中

```css
.container {
    display: flex;
    justify-content: center;
    align-items: center;
}
```

### 4. 导航栏

```css
.nav {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.nav-logo {
    flex: 0 0 auto;
}

.nav-links {
    display: flex;
    gap: 20px;
}
```

### 5. 卡片布局

```css
.card-container {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
}

.card {
    flex: 1 1 300px; /* 最小宽度300px，自动填充 */
}
```

### 6. 圣杯布局（Holy Grail Layout）

```css
.container {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
}

.header, .footer {
    flex: 0 0 auto;
}

.main {
    display: flex;
    flex: 1;
}

.sidebar {
    flex: 0 0 200px;
}

.content {
    flex: 1;
}
```

## 主轴和交叉轴

理解主轴和交叉轴很重要：

- **主轴（Main Axis）**：由 `flex-direction` 决定
  - `row`：水平方向（左→右）
  - `column`：垂直方向（上→下）

- **交叉轴（Cross Axis）**：与主轴垂直
  - `row` 时：垂直方向
  - `column` 时：水平方向

## 常见问题

### 1. 为什么项目没有换行？

检查是否设置了 `flex-wrap: wrap`，或者项目的总宽度是否超过了容器。

### 2. 如何让最后一个项目靠右？

```css
.container {
    display: flex;
}

.last-item {
    margin-left: auto; /* 自动填充左边距，推到最后 */
}
```

### 3. 如何让项目等宽？

```css
.item {
    flex: 1; /* 所有项目都设置为 flex: 1 */
}
```

## 下一步

Flexbox非常适合一维布局。如果需要二维布局（同时控制行和列），接下来学习Grid布局！

