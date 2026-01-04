# 第五章：Grid布局

## 什么是Grid？

CSS Grid是一个**二维布局系统**，可以同时控制**行和列**。它比Flexbox更强大，适合复杂的页面布局。

## Grid vs Flexbox

- **Flexbox**：一维布局（行或列）
- **Grid**：二维布局（行和列同时控制）

**选择建议：**
- 简单的一维布局 → 使用 Flexbox
- 复杂的二维布局 → 使用 Grid
- 两者可以结合使用

## Grid的基本概念

### 容器（Container）和项目（Items）

```html
<div class="grid-container">
    <div class="grid-item">1</div>
    <div class="grid-item">2</div>
    <div class="grid-item">3</div>
</div>
```

```css
.grid-container {
    display: grid;
}
```

## 容器的属性

### 1. grid-template-columns（定义列）

定义网格的列数和每列的宽度：

```css
.grid-container {
    grid-template-columns: 200px 200px 200px; /* 3列，每列200px */
    grid-template-columns: 1fr 2fr 1fr;       /* 3列，比例1:2:1 */
    grid-template-columns: repeat(3, 1fr);    /* 3列，每列等宽 */
    grid-template-columns: 200px 1fr auto;      /* 混合单位 */
}
```

**单位说明：**
- `fr`：分数单位（fraction），按比例分配剩余空间
- `auto`：根据内容自动调整
- `px`、`%`、`em`、`rem`：固定单位

### 2. grid-template-rows（定义行）

定义网格的行数和每行的高度：

```css
.grid-container {
    grid-template-rows: 100px 200px 100px; /* 3行 */
    grid-template-rows: repeat(3, 1fr);    /* 3行，每行等高 */
}
```

### 3. grid-template-areas（命名区域）

通过命名区域来定义布局（更直观）：

```css
.grid-container {
    grid-template-areas:
        "header header header"
        "sidebar main main"
        "footer footer footer";
}

.header { grid-area: header; }
.sidebar { grid-area: sidebar; }
.main { grid-area: main; }
.footer { grid-area: footer; }
```

### 4. gap（间距）

设置网格线之间的间距（行列间距）：

```css
.grid-container {
    gap: 20px;           /* 行列间距都是20px */
    gap: 20px 10px;      /* 行间距20px，列间距10px */
    row-gap: 20px;       /* 只设置行间距 */
    column-gap: 10px;    /* 只设置列间距 */
}
```

**注意：** `gap` 是 `row-gap` 和 `column-gap` 的简写。

### 5. justify-items（水平对齐）

控制项目在网格单元格内的水平对齐：

```css
.grid-container {
    justify-items: start;   /* 左对齐 */
    justify-items: end;     /* 右对齐 */
    justify-items: center;  /* 居中 */
    justify-items: stretch; /* 拉伸（默认） */
}
```

### 6. align-items（垂直对齐）

控制项目在网格单元格内的垂直对齐：

```css
.grid-container {
    align-items: start;   /* 顶部对齐 */
    align-items: end;     /* 底部对齐 */
    align-items: center;  /* 居中 */
    align-items: stretch; /* 拉伸（默认） */
}
```

### 7. justify-content（容器水平对齐）

当网格总宽度小于容器宽度时，控制整个网格的水平对齐：

```css
.grid-container {
    justify-content: start;
    justify-content: end;
    justify-content: center;
    justify-content: space-between;
    justify-content: space-around;
}
```

### 8. align-content（容器垂直对齐）

当网格总高度小于容器高度时，控制整个网格的垂直对齐。

### 9. grid-auto-rows / grid-auto-columns

定义自动创建的行/列的尺寸：

```css
.grid-container {
    grid-auto-rows: 100px; /* 自动创建的行高度为100px */
    grid-auto-columns: 150px;
}
```

## 项目的属性

### 1. grid-column（列位置）

定义项目占据的列：

```css
.item {
    grid-column: 1 / 3;        /* 从第1列到第3列（不包含3） */
    grid-column: 1 / span 2;   /* 从第1列开始，跨越2列 */
    grid-column-start: 1;      /* 开始列 */
    grid-column-end: 3;        /* 结束列 */
}
```

### 2. grid-row（行位置）

定义项目占据的行：

```css
.item {
    grid-row: 1 / 3;
    grid-row: 1 / span 2;
    grid-row-start: 1;
    grid-row-end: 3;
}
```

### 3. grid-area（简写）

`grid-row-start`、`grid-column-start`、`grid-row-end`、`grid-column-end` 的简写：

```css
.item {
    grid-area: 1 / 1 / 3 / 3; /* row-start / col-start / row-end / col-end */
}
```

或者用于命名区域：

```css
.item {
    grid-area: header;
}
```

### 4. justify-self（单个项目水平对齐）

控制单个项目在单元格内的水平对齐：

```css
.item {
    justify-self: start;
    justify-self: end;
    justify-self: center;
    justify-self: stretch;
}
```

### 5. align-self（单个项目垂直对齐）

控制单个项目在单元格内的垂直对齐。

## 实际应用示例

### 1. 基础网格布局

```css
.grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
}
```

### 2. 响应式网格

```css
.grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
}
```

**说明：**
- `auto-fit`：自动适应容器宽度
- `minmax(250px, 1fr)`：最小250px，最大1fr

### 3. 经典布局（Header, Sidebar, Main, Footer）

```css
.layout {
    display: grid;
    grid-template-areas:
        "header header header"
        "sidebar main main"
        "footer footer footer";
    grid-template-rows: 80px 1fr 60px;
    grid-template-columns: 200px 1fr;
    gap: 20px;
    min-height: 100vh;
}

.header { grid-area: header; }
.sidebar { grid-area: sidebar; }
.main { grid-area: main; }
.footer { grid-area: footer; }
```

### 4. 卡片网格

```css
.card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
}
```

### 5. 不对称布局

```css
.asymmetric {
    display: grid;
    grid-template-columns: 1fr 2fr 1fr;
    grid-template-rows: 100px 200px 100px;
}

.item-large {
    grid-column: 1 / 3;
    grid-row: 1 / 3;
}
```

## Grid 和 Flexbox 结合使用

Grid用于整体布局，Flexbox用于组件内部：

```css
.page {
    display: grid;
    grid-template-columns: 200px 1fr;
}

.card {
    display: flex;
    flex-direction: column;
}
```

## 常见问题

### 1. 如何让网格响应式？

使用 `repeat(auto-fit, minmax(...))` 或媒体查询。

### 2. fr 和 % 的区别？

- `fr`：按比例分配**剩余空间**
- `%`：相对于**容器宽度**的百分比

### 3. 如何让项目跨越多列/多行？

使用 `grid-column: span 2` 或 `grid-row: span 2`。

## 下一步

Grid布局是CSS布局的终极工具。接下来学习响应式设计，让你的布局适配各种设备！

