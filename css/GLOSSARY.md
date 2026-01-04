# CSS术语表（词汇表）

## A

**Animation（动画）**
- 定义：使用@keyframes创建的复杂动画效果
- 示例：`animation: slide 1s ease-in-out;`

**Attribute Selector（属性选择器）**
- 定义：根据HTML属性选择元素的选择器
- 示例：`a[href]`、`input[type="text"]`

## B

**Block Element（块级元素）**
- 定义：独占一行的元素，可以设置宽高
- 示例：`<div>`、`<p>`、`<h1>`

**Border（边框）**
- 定义：围绕元素内容和内边距的线条
- 示例：`border: 2px solid black;`

**Box Model（盒模型）**
- 定义：描述元素如何占据空间的模型，包括content、padding、border、margin

## C

**Cascade（层叠）**
- 定义：CSS规则如何相互覆盖的机制
- 说明：多个样式规则作用于同一元素时，优先级高的规则生效

**Class Selector（类选择器）**
- 定义：选择具有特定class属性的元素
- 示例：`.button`、`.card`

**CSS Variable（CSS变量）**
- 定义：可重用的CSS值
- 示例：`--primary-color: #2196F3;`

## D

**Display（显示方式）**
- 定义：控制元素的显示方式
- 值：`block`、`inline`、`inline-block`、`flex`、`grid`、`none`

## E

**Element Selector（元素选择器）**
- 定义：选择所有指定标签的元素
- 示例：`p`、`div`、`h1`

**External Stylesheet（外部样式表）**
- 定义：独立的CSS文件，通过`<link>`标签引入
- 优点：可复用、易维护、可缓存

## F

**Flexbox（弹性盒子）**
- 定义：一维布局方法，用于在容器内排列元素
- 用途：适合行或列的布局

**Float（浮动）**
- 定义：使元素脱离文档流，向左或向右浮动
- 注意：现代布局推荐使用Flexbox或Grid代替

## G

**Grid（网格）**
- 定义：二维布局系统，可以同时控制行和列
- 用途：适合复杂的页面布局

## H

**Hover（悬停）**
- 定义：鼠标指针悬停在元素上时的状态
- 示例：`.button:hover { color: red; }`

## I

**ID Selector（ID选择器）**
- 定义：选择具有特定id属性的元素（应该是唯一的）
- 示例：`#header`、`#footer`

**Inline Element（行内元素）**
- 定义：不换行的元素，不能设置宽高
- 示例：`<span>`、`<a>`、`<strong>`

**Inline Styles（内联样式）**
- 定义：直接在HTML元素上使用style属性
- 示例：`<p style="color: red;">`

**Internal Stylesheet（内部样式表）**
- 定义：在HTML的`<head>`中使用`<style>`标签

## J

**Justify-content（主轴对齐）**
- 定义：Flexbox中控制项目在主轴上的对齐方式
- 值：`flex-start`、`center`、`flex-end`、`space-between`等

## K

**Keyframes（关键帧）**
- 定义：定义动画的各个阶段
- 示例：`@keyframes slide { from { ... } to { ... } }`

## L

**Layout（布局）**
- 定义：元素在页面上的排列方式
- 方法：Flexbox、Grid、Float等

## M

**Margin（外边距）**
- 定义：元素与其他元素之间的空间（透明）
- 示例：`margin: 20px;`

**Media Query（媒体查询）**
- 定义：根据设备特性应用不同的样式
- 示例：`@media (max-width: 768px) { ... }`

## P

**Padding（内边距）**
- 定义：元素内容与边框之间的空间（显示背景色）
- 示例：`padding: 20px;`

**Pseudo-class（伪类）**
- 定义：选择元素的特定状态
- 示例：`:hover`、`:focus`、`:first-child`

**Pseudo-element（伪元素）**
- 定义：创建不在HTML中的元素
- 示例：`::before`、`::after`、`::first-line`

## R

**Responsive Design（响应式设计）**
- 定义：网页能够根据不同的设备自动调整布局和样式

**Rule（规则）**
- 定义：CSS的基本单位，由选择器和声明块组成
- 示例：`p { color: red; }`

## S

**Selector（选择器）**
- 定义：用于选择要样式化的HTML元素
- 类型：元素、类、ID、属性、伪类等

**Specificity（优先级）**
- 定义：当多个规则作用于同一元素时，决定哪个规则生效的机制

## T

**Transform（变换）**
- 定义：对元素进行旋转、缩放、移动、倾斜等操作
- 示例：`transform: translateX(100px);`

**Transition（过渡）**
- 定义：属性值改变时创建平滑的动画效果
- 示例：`transition: color 0.3s ease;`

## V

**Viewport（视口）**
- 定义：浏览器显示网页的区域
- 单位：`vw`（视口宽度）、`vh`（视口高度）

## Z

**Z-index（层叠顺序）**
- 定义：控制元素的层叠顺序（仅对定位元素有效）
- 说明：数值越大，越在上层

---

**提示：** 遇到不熟悉的术语时，可以查阅这个词汇表！

