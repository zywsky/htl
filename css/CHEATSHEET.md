# CSS速查表（Cheat Sheet）

## 📋 常用属性速查

### 文本属性

```css
color: #333;                    /* 文本颜色 */
font-size: 16px;                /* 字体大小 */
font-family: Arial, sans-serif;  /* 字体族 */
font-weight: bold;               /* 字体粗细 */
text-align: center;             /* 文本对齐 */
text-decoration: underline;     /* 文本装饰 */
line-height: 1.6;               /* 行高 */
```

### 颜色属性

```css
color: red;                     /* 颜色名 */
color: #FF0000;                 /* 十六进制 */
color: rgb(255, 0, 0);          /* RGB */
color: rgba(255, 0, 0, 0.5);    /* RGBA（带透明度） */
background-color: blue;         /* 背景颜色 */
```

### 尺寸属性

```css
width: 100px;                   /* 宽度 */
height: 100px;                   /* 高度 */
max-width: 800px;                /* 最大宽度 */
min-width: 300px;                /* 最小宽度 */
```

### 间距属性

```css
margin: 10px;                    /* 四个方向 */
margin: 10px 20px;               /* 上下 左右 */
margin: 10px 20px 15px;         /* 上 左右 下 */
margin: 10px 20px 15px 25px;    /* 上 右 下 左 */

padding: 10px;                   /* 内边距（同margin） */
```

### 边框属性

```css
border: 2px solid black;         /* 宽度 样式 颜色 */
border-width: 2px;
border-style: solid;             /* solid, dashed, dotted */
border-color: black;
border-radius: 5px;              /* 圆角 */
```

### 显示属性

```css
display: block;                  /* 块级 */
display: inline;                 /* 行内 */
display: inline-block;           /* 行内块 */
display: flex;                   /* Flexbox */
display: grid;                   /* Grid */
display: none;                   /* 隐藏 */
```

### 定位属性

```css
position: static;                /* 默认 */
position: relative;               /* 相对定位 */
position: absolute;               /* 绝对定位 */
position: fixed;                 /* 固定定位 */
position: sticky;                 /* 粘性定位 */

top: 10px;
right: 20px;
bottom: 30px;
left: 40px;
z-index: 100;
```

## 🎯 选择器速查

### 基础选择器

```css
p { }                            /* 元素选择器 */
.class { }                       /* 类选择器 */
#id { }                          /* ID选择器 */
* { }                            /* 通配符 */
```

### 组合选择器

```css
div p { }                        /* 后代选择器 */
div > p { }                      /* 子元素选择器 */
h2 + p { }                       /* 相邻兄弟选择器 */
h2 ~ p { }                       /* 通用兄弟选择器 */
```

### 属性选择器

```css
a[href] { }                      /* 有href属性 */
a[href="url"] { }                /* href等于url */
a[href*="example"] { }           /* href包含example */
a[href^="https"] { }             /* href以https开头 */
a[href$=".pdf"] { }              /* href以.pdf结尾 */
```

### 伪类选择器

```css
a:link { }                        /* 未访问链接 */
a:visited { }                    /* 已访问链接 */
a:hover { }                       /* 悬停 */
a:active { }                     /* 激活 */
input:focus { }                  /* 聚焦 */
li:first-child { }               /* 第一个子元素 */
li:last-child { }                /* 最后一个子元素 */
li:nth-child(3) { }              /* 第3个子元素 */
li:nth-child(odd) { }            /* 奇数子元素 */
li:nth-child(even) { }           /* 偶数子元素 */
```

### 伪元素选择器

```css
p::before { content: ""; }       /* 之前 */
p::after { content: ""; }        /* 之后 */
p::first-line { }                /* 第一行 */
p::first-letter { }              /* 首字母 */
::selection { }                   /* 选中文本 */
```

## 📐 Flexbox速查

### 容器属性

```css
display: flex;
flex-direction: row;             /* row, column, row-reverse, column-reverse */
flex-wrap: wrap;                 /* nowrap, wrap, wrap-reverse */
justify-content: center;         /* flex-start, center, flex-end, space-between, space-around */
align-items: center;             /* flex-start, center, flex-end, stretch */
align-content: center;           /* 多行对齐 */
gap: 20px;                       /* 间距 */
```

### 项目属性

```css
flex-grow: 1;                    /* 放大比例 */
flex-shrink: 1;                  /* 缩小比例 */
flex-basis: 200px;               /* 初始大小 */
flex: 1;                         /* 简写：grow shrink basis */
align-self: center;              /* 单个项目对齐 */
order: 1;                        /* 排序 */
```

## 🎨 Grid速查

### 容器属性

```css
display: grid;
grid-template-columns: repeat(3, 1fr);  /* 定义列 */
grid-template-rows: 100px 200px;        /* 定义行 */
grid-template-areas: "header header" "sidebar main";  /* 命名区域 */
gap: 20px;                               /* 间距 */
justify-items: center;                   /* 水平对齐 */
align-items: center;                     /* 垂直对齐 */
```

### 项目属性

```css
grid-column: 1 / 3;              /* 列位置 */
grid-row: 1 / 2;                 /* 行位置 */
grid-area: header;                /* 命名区域 */
justify-self: center;             /* 单个项目水平对齐 */
align-self: center;               /* 单个项目垂直对齐 */
```

## 🎬 动画速查

### Transition（过渡）

```css
transition: property duration timing-function delay;
transition: color 0.3s ease;
transition: all 0.3s ease;
```

### Transform（变换）

```css
transform: translateX(100px);    /* 水平移动 */
transform: translateY(50px);     /* 垂直移动 */
transform: translate(100px, 50px);  /* 同时移动 */
transform: scale(1.5);            /* 缩放 */
transform: rotate(45deg);         /* 旋转 */
transform: skew(10deg, 5deg);     /* 倾斜 */
```

### Animation（动画）

```css
@keyframes name {
    from { }
    to { }
}

animation: name duration timing-function delay iteration-count direction;
animation: slide 1s ease-in-out infinite;
```

## 📱 媒体查询速查

```css
/* 手机 */
@media (max-width: 575px) { }

/* 大手机 */
@media (min-width: 576px) and (max-width: 767px) { }

/* 平板 */
@media (min-width: 768px) and (max-width: 991px) { }

/* 电脑 */
@media (min-width: 992px) { }

/* 横屏 */
@media (orientation: landscape) { }

/* 竖屏 */
@media (orientation: portrait) { }
```

## 🎨 CSS函数速查

```css
calc(100% - 50px);               /* 计算 */
min(100%, 800px);                /* 最小值 */
max(300px, 50%);                 /* 最大值 */
clamp(16px, 4vw, 24px);          /* 限制范围 */
var(--variable-name);             /* CSS变量 */
url('image.jpg');                 /* 资源路径 */
linear-gradient(to right, red, blue);  /* 线性渐变 */
radial-gradient(circle, red, blue);    /* 径向渐变 */
```

## 🔧 常用工具类

```css
/* 居中 */
.center {
    display: flex;
    justify-content: center;
    align-items: center;
}

/* 清除浮动 */
.clearfix::after {
    content: "";
    display: table;
    clear: both;
}

/* 隐藏 */
.hidden {
    display: none;
}

/* 文本省略 */
.text-ellipsis {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
```

## 📏 单位速查

```css
10px;                            /* 像素（绝对单位） */
50%;                             /* 百分比（相对单位） */
1em;                             /* 相对于父元素字体大小 */
1rem;                            /* 相对于根元素字体大小 */
1vw;                             /* 视口宽度的1% */
1vh;                             /* 视口高度的1% */
```

## 🎯 优先级速查

1. **内联样式** (1000分)
2. **ID选择器** (100分)
3. **类选择器、属性选择器、伪类** (10分)
4. **元素选择器、伪元素** (1分)

---

**提示：** 打印或保存这个速查表，方便随时查阅！

