# 第二章：CSS基础

## 什么是CSS？

CSS (Cascading Style Sheets) 层叠样式表，用于控制HTML元素的外观和布局。

## CSS的三种引入方式

### 1. 内联样式（Inline Styles）
直接在HTML元素上使用 `style` 属性：

```html
<p style="color: red;">红色文本</p>
```

**优点**：优先级最高，直接作用于元素  
**缺点**：难以维护，不能复用

### 2. 内部样式表（Internal Stylesheet）
在HTML的 `<head>` 中使用 `<style>` 标签：

```html
<head>
  <style>
    p {
      color: red;
    }
  </style>
</head>
```

**优点**：集中管理，优先级适中  
**缺点**：只能用于当前页面

### 3. 外部样式表（External Stylesheet）⭐ 推荐
在独立的 `.css` 文件中编写，通过 `<link>` 引入：

```html
<head>
  <link rel="stylesheet" href="styles.css">
</head>
```

**优点**：可复用、易维护、可缓存  
**缺点**：需要额外的HTTP请求（但可忽略）

## CSS语法结构

```css
选择器 {
  属性名: 属性值;
  属性名: 属性值;
}
```

**示例：**
```css
p {
  color: blue;
  font-size: 16px;
  margin: 10px;
}
```

## CSS选择器

选择器用于"选择"要样式化的HTML元素。

### 1. 元素选择器（Element Selector）
选择所有指定标签的元素：

```css
p {
  color: red;
}
/* 所有 <p> 标签都会变成红色 */
```

### 2. 类选择器（Class Selector）
选择具有特定 `class` 属性的元素：

```css
.highlight {
  background-color: yellow;
}
```

```html
<p class="highlight">这段文字会有黄色背景</p>
```

**注意**：类名可以多个元素共享，一个元素可以有多个类名。

### 3. ID选择器（ID Selector）
选择具有特定 `id` 属性的元素：

```css
#header {
  background-color: blue;
}
```

```html
<div id="header">这是页头</div>
```

**注意**：ID应该是唯一的，一个页面中同一个ID只能出现一次。

### 4. 后代选择器（Descendant Selector）
选择某个元素内部的所有指定元素：

```css
div p {
  color: green;
}
/* 选择所有在 <div> 内部的 <p> 标签 */
```

### 5. 子元素选择器（Child Selector）
选择直接子元素（只选择一级）：

```css
div > p {
  color: blue;
}
/* 只选择 <div> 的直接子元素 <p> */
```

### 6. 相邻兄弟选择器（Adjacent Sibling Selector）
选择紧跟在指定元素后面的兄弟元素：

```css
h2 + p {
  margin-top: 0;
}
/* 选择紧跟在 <h2> 后面的 <p> */
```

### 7. 通用兄弟选择器（General Sibling Selector）
选择所有在指定元素后面的兄弟元素：

```css
h2 ~ p {
  color: gray;
}
/* 选择所有在 <h2> 后面的 <p> */
```

### 8. 属性选择器（Attribute Selector）
根据属性选择元素：

```css
/* 选择有 href 属性的 <a> 标签 */
a[href] {
  color: blue;
}

/* 选择 href 属性值等于 "https://example.com" 的 <a> 标签 */
a[href="https://example.com"] {
  color: red;
}

/* 选择 href 属性值包含 "example" 的 <a> 标签 */
a[href*="example"] {
  text-decoration: underline;
}
```

### 9. 伪类选择器（Pseudo-class Selector）
选择元素的特定状态：

```css
/* 链接的未访问状态 */
a:link {
  color: blue;
}

/* 链接的访问过状态 */
a:visited {
  color: purple;
}

/* 鼠标悬停状态 */
a:hover {
  color: red;
}

/* 激活状态（点击时） */
a:active {
  color: orange;
}

/* 第一个子元素 */
li:first-child {
  font-weight: bold;
}

/* 最后一个子元素 */
li:last-child {
  border-bottom: none;
}

/* 第n个子元素 */
li:nth-child(2) {
  color: green;
}

/* 奇数子元素 */
li:nth-child(odd) {
  background-color: #f0f0f0;
}

/* 偶数子元素 */
li:nth-child(even) {
  background-color: #fff;
}
```

### 10. 伪元素选择器（Pseudo-element Selector）
选择元素的特定部分：

```css
/* 元素的第一行 */
p::first-line {
  font-weight: bold;
}

/* 元素的首字母 */
p::first-letter {
  font-size: 2em;
}

/* 元素之前插入内容 */
p::before {
  content: ">> ";
}

/* 元素之后插入内容 */
p::after {
  content: " <<";
}
```

### 11. 组合选择器
可以组合多个选择器：

```css
/* 多个选择器，用逗号分隔（或） */
h1, h2, h3 {
  color: blue;
}

/* 同时满足多个条件（与） */
p.highlight {
  color: red;
}
/* 选择既是 <p> 又有 class="highlight" 的元素 */
```

## 选择器优先级（Specificity）

当多个规则作用于同一个元素时，优先级决定哪个规则生效：

1. **内联样式** (1000分)
2. **ID选择器** (100分)
3. **类选择器、属性选择器、伪类** (10分)
4. **元素选择器、伪元素** (1分)

**示例：**
```css
p { color: black; }           /* 优先级：1 */
.highlight { color: yellow; } /* 优先级：10 */
#header { color: blue; }      /* 优先级：100 */
```

如果优先级相同，**后定义的规则**会覆盖前面的。

## 常用CSS属性

### 文本属性
- `color`: 文本颜色
- `font-size`: 字体大小
- `font-family`: 字体族
- `font-weight`: 字体粗细（normal, bold, 100-900）
- `text-align`: 文本对齐（left, center, right, justify）
- `text-decoration`: 文本装饰（none, underline, line-through）
- `line-height`: 行高

### 颜色属性
- `color`: 文本颜色
- `background-color`: 背景颜色

颜色可以用多种方式表示：
- 颜色名：`red`, `blue`, `green`
- 十六进制：`#FF0000`, `#f00`（简写）
- RGB：`rgb(255, 0, 0)`
- RGBA：`rgba(255, 0, 0, 0.5)`（带透明度）

### 尺寸属性
- `width`: 宽度
- `height`: 高度
- `max-width`: 最大宽度
- `min-width`: 最小宽度
- `max-height`: 最大高度
- `min-height`: 最小高度

单位：
- `px`: 像素（绝对单位）
- `%`: 百分比（相对单位）
- `em`: 相对于父元素字体大小
- `rem`: 相对于根元素字体大小
- `vw`: 视口宽度的1%
- `vh`: 视口高度的1%

### 间距属性
- `margin`: 外边距（元素与其他元素的距离）
- `padding`: 内边距（元素内容与边框的距离）

可以分别设置四个方向：
```css
margin-top: 10px;
margin-right: 20px;
margin-bottom: 10px;
margin-left: 20px;

/* 简写形式 */
margin: 10px 20px;        /* 上下 左右 */
margin: 10px 20px 15px;   /* 上 左右 下 */
margin: 10px 20px 15px 25px; /* 上 右 下 左（顺时针） */
```

### 边框属性
- `border`: 边框（宽度 样式 颜色）
- `border-width`: 边框宽度
- `border-style`: 边框样式（solid, dashed, dotted, none）
- `border-color`: 边框颜色
- `border-radius`: 圆角

### 显示属性
- `display`: 显示方式
  - `block`: 块级元素（独占一行）
  - `inline`: 行内元素（不换行）
  - `inline-block`: 行内块（不换行但可设置宽高）
  - `none`: 隐藏元素
- `visibility`: 可见性
  - `visible`: 可见
  - `hidden`: 隐藏（仍占据空间）

## CSS注释

```css
/* 这是单行注释 */

/* 
  这是
  多行
  注释
*/
```

## 下一步

现在你已经掌握了CSS的基础语法和选择器，接下来我们将学习盒模型，这是理解CSS布局的关键！

