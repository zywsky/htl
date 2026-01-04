# 第三章：盒模型（Box Model）

## 什么是盒模型？

盒模型是CSS的基础概念，它描述了每个HTML元素如何占据空间。理解盒模型对于掌握CSS布局至关重要。

## 盒模型的组成部分

每个元素都可以看作一个矩形盒子，由四个部分组成（从内到外）：

```
┌─────────────────────────────────────┐
│         Margin（外边距）              │
│  ┌───────────────────────────────┐  │
│  │      Border（边框）             │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │   Padding（内边距）       │  │  │
│  │  │  ┌───────────────────┐  │  │  │
│  │  │  │   Content（内容）  │  │  │  │
│  │  │  └───────────────────┘  │  │  │
│  │  └─────────────────────────┘  │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### 1. Content（内容区）
元素的实际内容，如文本、图片等。

### 2. Padding（内边距）
内容区与边框之间的空间。背景色会延伸到padding区域。

### 3. Border（边框）
围绕padding的线条。

### 4. Margin（外边距）
元素与其他元素之间的空间。**透明**，不显示背景色。

## 标准盒模型 vs IE盒模型

### 标准盒模型（content-box）⭐ 默认
元素的 `width` 和 `height` 只包括 **content** 区域。

```css
.box {
    width: 200px;
    padding: 20px;
    border: 5px solid black;
    /* 实际占用宽度 = 200 + 20*2 + 5*2 = 250px */
}
```

### IE盒模型（border-box）⭐ 推荐
元素的 `width` 和 `height` 包括 **content + padding + border**。

```css
.box {
    box-sizing: border-box;
    width: 200px;
    padding: 20px;
    border: 5px solid black;
    /* 实际占用宽度 = 200px（内容区会自动缩小） */
}
```

**推荐做法：** 在全局样式中设置 `box-sizing: border-box`，这样更直观。

```css
* {
    box-sizing: border-box;
}
```

## 外边距（Margin）

### 设置方式

```css
/* 四个方向分别设置 */
margin-top: 10px;
margin-right: 20px;
margin-bottom: 10px;
margin-left: 20px;

/* 简写形式 */
margin: 10px;                    /* 四个方向都是 10px */
margin: 10px 20px;               /* 上下 10px，左右 20px */
margin: 10px 20px 15px;          /* 上 10px，左右 20px，下 15px */
margin: 10px 20px 15px 25px;     /* 上 右 下 左（顺时针） */
```

### 外边距合并（Margin Collapse）

当两个垂直相邻的元素都有margin时，它们会**合并**（取较大值），而不是相加。

```css
.box1 {
    margin-bottom: 20px;
}
.box2 {
    margin-top: 30px;
}
/* 两个盒子之间的间距是 30px（不是 50px） */
```

**注意：** 只有**垂直方向**的margin会合并，水平方向不会。

### 负外边距

margin可以是负值，用于元素重叠或调整位置：

```css
.box {
    margin-top: -10px; /* 向上移动 10px */
}
```

## 内边距（Padding）

### 设置方式

与margin类似：

```css
padding: 10px;
padding: 10px 20px;
padding: 10px 20px 15px;
padding: 10px 20px 15px 25px;
```

### 与margin的区别

- **Margin**：元素**外部**的空间，透明，用于元素之间的间距
- **Padding**：元素**内部**的空间，显示背景色，用于内容与边框的间距

## 边框（Border）

### 基本语法

```css
border: width style color;
```

### 边框样式（style）

- `solid`: 实线
- `dashed`: 虚线
- `dotted`: 点线
- `double`: 双线
- `none`: 无边框
- `hidden`: 隐藏边框（与none类似，但在表格中有区别）

### 分别设置四个方向

```css
border-top: 2px solid red;
border-right: 3px dashed blue;
border-bottom: 1px dotted green;
border-left: 4px double orange;
```

### 圆角（border-radius）

```css
border-radius: 10px;           /* 四个角都是 10px */
border-radius: 10px 20px;       /* 左上右下 10px，右上左下 20px */
border-radius: 10px 20px 30px 40px; /* 左上 右上 右下 左下 */

/* 椭圆圆角 */
border-radius: 50%;             /* 圆形（宽高相等时） */
border-radius: 10px / 20px;     /* 水平半径 / 垂直半径 */
```

## 盒模型的实际应用

### 1. 居中元素

```css
/* 水平居中块级元素 */
.container {
    width: 600px;
    margin: 0 auto; /* 上下 0，左右自动（平分剩余空间） */
}
```

### 2. 创建间距

```css
.card {
    padding: 20px;      /* 内容与边框的间距 */
    margin: 10px;       /* 卡片之间的间距 */
}
```

### 3. 创建按钮

```css
.button {
    padding: 10px 20px;     /* 上下 10px，左右 20px */
    border: 2px solid #333;
    border-radius: 5px;
    margin: 5px;
}
```

## 常见问题

### 1. 为什么设置了 width 但元素还是超出了？

检查是否使用了 `box-sizing: border-box`，或者padding/border是否计算在内。

### 2. 为什么两个元素之间的间距不对？

可能是margin合并导致的，或者检查是否有负margin。

### 3. 如何让元素完全居中？

```css
.center {
    width: 200px;
    height: 200px;
    margin: 0 auto; /* 水平居中 */
    /* 垂直居中需要其他方法，后续会讲 */
}
```

## 下一步

理解了盒模型后，我们就可以学习布局了。接下来将学习Flexbox，这是现代CSS布局的强大工具！

