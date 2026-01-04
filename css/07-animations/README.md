# 第七章：CSS动画、过渡和变换

## 什么是CSS动画？

CSS动画可以让元素从一种样式平滑过渡到另一种样式，使网页更加生动和吸引人。

## 过渡（Transition）

过渡是CSS动画的基础，用于在属性值改变时创建平滑的动画效果。

### 基本语法

```css
.element {
    transition: property duration timing-function delay;
}
```

**参数说明：**
- `property`：要过渡的属性（如 `color`、`width` 等，或 `all` 表示所有属性）
- `duration`：过渡持续时间（如 `0.3s`、`500ms`）
- `timing-function`：时间函数（缓动函数）
- `delay`：延迟时间（可选）

### 示例

```css
.button {
    background-color: blue;
    transition: background-color 0.3s ease;
}

.button:hover {
    background-color: red; /* 鼠标悬停时，背景色会平滑过渡到红色 */
}
```

### 多个属性过渡

```css
.element {
    transition: width 0.3s ease, height 0.3s ease, background-color 0.5s ease;
}

/* 或使用 all */
.element {
    transition: all 0.3s ease;
}
```

### 时间函数（Timing Function）

控制动画的速度曲线：

```css
transition-timing-function: ease;        /* 默认：慢-快-慢 */
transition-timing-function: linear;      /* 匀速 */
transition-timing-function: ease-in;     /* 慢-快 */
transition-timing-function: ease-out;    /* 快-慢 */
transition-timing-function: ease-in-out; /* 慢-快-慢 */
transition-timing-function: cubic-bezier(0.25, 0.1, 0.25, 1); /* 自定义贝塞尔曲线 */
```

## 变换（Transform）

变换用于对元素进行旋转、缩放、移动、倾斜等操作。

### 基本语法

```css
.element {
    transform: function(value);
}
```

### 常用变换函数

#### 1. translate（移动）

```css
transform: translateX(50px);  /* 水平移动50px */
transform: translateY(50px);  /* 垂直移动50px */
transform: translate(50px, 50px); /* 同时水平和垂直移动 */
```

#### 2. scale（缩放）

```css
transform: scaleX(1.5);  /* 水平缩放1.5倍 */
transform: scaleY(1.5);  /* 垂直缩放1.5倍 */
transform: scale(1.5);   /* 整体缩放1.5倍 */
transform: scale(1.5, 2); /* 水平1.5倍，垂直2倍 */
```

#### 3. rotate（旋转）

```css
transform: rotate(45deg);  /* 顺时针旋转45度 */
transform: rotate(-45deg);  /* 逆时针旋转45度 */
```

#### 4. skew（倾斜）

```css
transform: skewX(20deg);  /* 水平倾斜20度 */
transform: skewY(20deg);  /* 垂直倾斜20度 */
transform: skew(20deg, 10deg); /* 同时水平和垂直倾斜 */
```

### 组合变换

可以同时应用多个变换：

```css
.element {
    transform: translate(50px, 50px) rotate(45deg) scale(1.2);
}
```

**注意：** 变换的顺序很重要，不同的顺序会产生不同的效果。

### transform-origin（变换原点）

设置变换的原点：

```css
transform-origin: center;        /* 默认：中心 */
transform-origin: top left;     /* 左上角 */
transform-origin: 50% 50%;      /* 中心（百分比） */
transform-origin: 20px 30px;    /* 具体位置 */
```

## 动画（Animation）

动画比过渡更强大，可以创建复杂的多阶段动画。

### 定义动画（@keyframes）

```css
@keyframes animation-name {
    0% {
        /* 起始状态 */
        transform: translateX(0);
    }
    50% {
        /* 中间状态 */
        transform: translateX(100px);
    }
    100% {
        /* 结束状态 */
        transform: translateX(200px);
    }
}
```

**简写：**
```css
@keyframes slide {
    from {
        transform: translateX(0);
    }
    to {
        transform: translateX(200px);
    }
}
```

### 应用动画

```css
.element {
    animation: name duration timing-function delay iteration-count direction fill-mode;
}
```

**参数说明：**
- `name`：动画名称
- `duration`：持续时间
- `timing-function`：时间函数
- `delay`：延迟
- `iteration-count`：播放次数（`infinite` 表示无限循环）
- `direction`：播放方向（`normal`、`reverse`、`alternate`）
- `fill-mode`：动画结束后的状态（`forwards`、`backwards`、`both`）

### 示例

```css
@keyframes bounce {
    0%, 100% {
        transform: translateY(0);
    }
    50% {
        transform: translateY(-20px);
    }
}

.ball {
    animation: bounce 1s ease-in-out infinite;
}
```

### 动画属性详解

#### animation-iteration-count（播放次数）

```css
animation-iteration-count: 1;      /* 播放1次 */
animation-iteration-count: 3;      /* 播放3次 */
animation-iteration-count: infinite; /* 无限循环 */
```

#### animation-direction（播放方向）

```css
animation-direction: normal;   /* 正常播放 */
animation-direction: reverse;  /* 反向播放 */
animation-direction: alternate; /* 交替播放（正反交替） */
animation-direction: alternate-reverse; /* 反向交替 */
```

#### animation-fill-mode（填充模式）

```css
animation-fill-mode: none;      /* 默认：不填充 */
animation-fill-mode: forwards;  /* 保持结束状态 */
animation-fill-mode: backwards; /* 保持起始状态 */
animation-fill-mode: both;     /* 同时保持起始和结束状态 */
```

#### animation-play-state（播放状态）

```css
animation-play-state: running;  /* 播放 */
animation-play-state: paused;   /* 暂停 */
```

## 实际应用示例

### 1. 悬停效果

```css
.card {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.card:hover {
    transform: translateY(-10px) scale(1.05);
    box-shadow: 0 10px 20px rgba(0,0,0,0.2);
}
```

### 2. 加载动画

```css
@keyframes spin {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}

.loader {
    animation: spin 1s linear infinite;
}
```

### 3. 淡入淡出

```css
@keyframes fadeIn {
    from {
        opacity: 0;
    }
    to {
        opacity: 1;
    }
}

.element {
    animation: fadeIn 0.5s ease-in;
}
```

### 4. 弹跳动画

```css
@keyframes bounce {
    0%, 20%, 50%, 80%, 100% {
        transform: translateY(0);
    }
    40% {
        transform: translateY(-30px);
    }
    60% {
        transform: translateY(-15px);
    }
}

.bounce {
    animation: bounce 2s infinite;
}
```

### 5. 滑动菜单

```css
.menu {
    transform: translateX(-100%);
    transition: transform 0.3s ease;
}

.menu.open {
    transform: translateX(0);
}
```

### 6. 脉冲效果

```css
@keyframes pulse {
    0% {
        transform: scale(1);
    }
    50% {
        transform: scale(1.1);
    }
    100% {
        transform: scale(1);
    }
}

.pulse {
    animation: pulse 2s ease-in-out infinite;
}
```

## 性能优化

### 1. 使用 transform 和 opacity

这两个属性不会触发重排（reflow），性能最好：

```css
/* 好的做法 */
.element {
    transform: translateX(100px);
    opacity: 0.5;
}

/* 避免的做法 */
.element {
    left: 100px; /* 会触发重排 */
    visibility: hidden; /* 会触发重排 */
}
```

### 2. 使用 will-change

提前告诉浏览器元素将要变化：

```css
.element {
    will-change: transform;
}
```

**注意：** 不要过度使用，只在确实需要时使用。

### 3. 使用 GPU 加速

```css
.element {
    transform: translateZ(0); /* 触发GPU加速 */
    /* 或 */
    transform: translate3d(0, 0, 0);
}
```

## 常见问题

### 1. 动画不流畅？

- 检查是否使用了会触发重排的属性
- 使用 `transform` 和 `opacity` 代替 `left`、`top`、`width`、`height`
- 减少动画的复杂度

### 2. 如何暂停动画？

```css
.element {
    animation-play-state: paused;
}
```

### 3. 如何让动画只播放一次？

```css
.element {
    animation-iteration-count: 1;
    animation-fill-mode: forwards; /* 保持结束状态 */
}
```

## 下一步

掌握了动画后，接下来学习CSS的高级特性，包括变量、函数、伪元素等，让你的CSS代码更加优雅和强大！

