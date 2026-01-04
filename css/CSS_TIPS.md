# CSS技巧和窍门

## 💡 实用技巧

### 1. 使用CSS变量创建主题

```css
:root {
    --primary-color: #2196F3;
    --secondary-color: #4CAF50;
    --bg-color: #f5f5f5;
}

[data-theme="dark"] {
    --primary-color: #64B5F6;
    --bg-color: #1a1a1a;
}

body {
    background-color: var(--bg-color);
    color: var(--primary-color);
    transition: background-color 0.3s, color 0.3s;
}
```

---

### 2. 使用clamp()实现响应式字体

```css
h1 {
    font-size: clamp(24px, 5vw, 48px);
    /* 最小24px，理想5vw，最大48px */
}
```

---

### 3. 使用aspect-ratio保持宽高比

```css
.image {
    width: 100%;
    aspect-ratio: 16 / 9;  /* 保持16:9比例 */
    object-fit: cover;
}
```

---

### 4. 使用:has()选择器（现代浏览器）

```css
/* 选择包含特定子元素的父元素 */
.card:has(.badge) {
    border: 2px solid gold;
}

/* 选择紧跟在特定元素后的元素 */
h2:has(+ p) {
    margin-bottom: 0;
}
```

---

### 5. 使用:is()简化选择器

```css
/* 简化前 */
h1, h2, h3, h4, h5, h6 {
    margin-top: 0;
}

/* 简化后 */
:is(h1, h2, h3, h4, h5, h6) {
    margin-top: 0;
}
```

---

### 6. 使用:where()降低优先级

```css
/* :where()的优先级为0 */
:where(h1, h2, h3) {
    margin-top: 0;  /* 容易被覆盖 */
}
```

---

### 7. 使用container queries（容器查询）

```css
.card-container {
    container-type: inline-size;
}

@container (min-width: 400px) {
    .card {
        display: flex;
    }
}
```

---

### 8. 使用:focus-visible改善可访问性

```css
/* 只在键盘导航时显示焦点 */
.button:focus-visible {
    outline: 2px solid blue;
}

.button:focus:not(:focus-visible) {
    outline: none;  /* 鼠标点击时不显示 */
}
```

---

### 9. 使用:empty隐藏空元素

```css
/* 隐藏空的错误消息 */
.error-message:empty {
    display: none;
}
```

---

### 10. 使用:not()排除特定元素

```css
/* 除了最后一个，其他都有下边距 */
.item:not(:last-child) {
    margin-bottom: 20px;
}
```

---

## 🎨 视觉效果技巧

### 11. 创建渐变文字

```css
.gradient-text {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}
```

---

### 12. 创建毛玻璃效果

```css
.glass {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
}
```

---

### 13. 创建文字阴影效果

```css
.text-shadow {
    text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.text-shadow-glow {
    text-shadow: 0 0 10px rgba(255, 255, 255, 0.8);
}
```

---

### 14. 创建3D效果

```css
.card-3d {
    transform: perspective(1000px) rotateY(10deg);
    transition: transform 0.3s;
}

.card-3d:hover {
    transform: perspective(1000px) rotateY(0deg) scale(1.05);
}
```

---

### 15. 创建打字机效果

```css
@keyframes typing {
    from { width: 0; }
    to { width: 100%; }
}

.typewriter {
    overflow: hidden;
    white-space: nowrap;
    animation: typing 3s steps(40, end);
}
```

---

## 📐 布局技巧

### 16. 使用Grid自动填充

```css
.grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 20px;
}
```

---

### 17. 使用Grid创建圣杯布局

```css
.layout {
    display: grid;
    grid-template-areas:
        "header header header"
        "sidebar main aside"
        "footer footer footer";
    grid-template-columns: 200px 1fr 200px;
    grid-template-rows: auto 1fr auto;
    min-height: 100vh;
}
```

---

### 18. 使用Flexbox实现粘性页脚

```css
body {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
}

main {
    flex: 1;
}

footer {
    margin-top: auto;
}
```

---

### 19. 使用Grid实现等高列

```css
.columns {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
    align-items: start;  /* 默认stretch，列等高 */
}
```

---

### 20. 使用subgrid（现代浏览器）

```css
.grid-item {
    display: grid;
    grid-template-columns: subgrid;
    grid-column: span 2;
}
```

---

## 🎬 动画技巧

### 21. 使用steps()创建逐帧动画

```css
@keyframes walk {
    from { background-position: 0 0; }
    to { background-position: -800px 0; }
}

.sprite {
    animation: walk 1s steps(8, end) infinite;
}
```

---

### 22. 暂停动画

```css
.paused {
    animation-play-state: paused;
}
```

---

### 23. 反向播放动画

```css
.reverse {
    animation-direction: reverse;
}
```

---

### 24. 创建弹跳效果

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
```

---

### 25. 创建脉冲效果

```css
@keyframes pulse {
    0%, 100% {
        transform: scale(1);
        opacity: 1;
    }
    50% {
        transform: scale(1.1);
        opacity: 0.8;
    }
}
```

---

## 🔧 实用技巧

### 26. 使用CSS计数器

```css
ol {
    counter-reset: item;
    list-style: none;
}

li {
    counter-increment: item;
}

li::before {
    content: counter(item) ". ";
    font-weight: bold;
}
```

---

### 27. 创建自定义复选框

```css
.checkbox-custom {
    appearance: none;
    width: 20px;
    height: 20px;
    border: 2px solid #ddd;
    border-radius: 4px;
    position: relative;
}

.checkbox-custom:checked {
    background-color: #2196F3;
    border-color: #2196F3;
}

.checkbox-custom:checked::after {
    content: "✓";
    position: absolute;
    color: white;
    font-size: 14px;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
}
```

---

### 28. 创建自定义滚动条

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

---

### 29. 使用:target创建标签页

```css
.tab-content {
    display: none;
}

.tab-content:target {
    display: block;
}
```

---

### 30. 创建工具提示

```css
.tooltip {
    position: relative;
}

.tooltip::before {
    content: attr(data-tooltip);
    position: absolute;
    bottom: 100%;
    left: 50%;
    transform: translateX(-50%);
    padding: 8px 12px;
    background: #333;
    color: white;
    border-radius: 4px;
    white-space: nowrap;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.3s;
}

.tooltip:hover::before {
    opacity: 1;
}
```

---

## 🎯 性能技巧

### 31. 使用contain优化性能

```css
.card {
    contain: layout style paint;
    /* 限制重排和重绘的范围 */
}
```

---

### 32. 使用content-visibility

```css
.long-list {
    content-visibility: auto;
    /* 只渲染可见部分 */
}
```

---

### 33. 使用will-change提示浏览器

```css
.animated {
    will-change: transform;
    /* 提示浏览器优化 */
}
```

---

## 📱 响应式技巧

### 34. 使用容器查询

```css
.card-container {
    container-type: inline-size;
}

@container (min-width: 400px) {
    .card {
        display: flex;
    }
}
```

---

### 35. 使用min()和max()

```css
.element {
    width: min(100%, 800px);
    padding: max(10px, 2vw);
}
```

---

### 36. 使用clamp()三值限制

```css
.element {
    font-size: clamp(16px, 4vw, 24px);
    width: clamp(300px, 50%, 800px);
}
```

---

## 🎨 创意技巧

### 37. 创建渐变边框

```css
.gradient-border {
    border: 2px solid;
    border-image: linear-gradient(45deg, red, blue) 1;
}
```

---

### 38. 创建文字描边

```css
.text-stroke {
    -webkit-text-stroke: 2px black;
    color: transparent;
}
```

---

### 39. 创建图片遮罩

```css
.image-mask {
    mask-image: linear-gradient(to bottom, black, transparent);
    -webkit-mask-image: linear-gradient(to bottom, black, transparent);
}
```

---

### 40. 创建分页效果

```css
.page-break {
    break-after: page;
    /* 打印时换页 */
}
```

---

## 💡 最佳实践

1. **使用现代CSS特性**
   - 但提供降级方案
   - 使用@supports检测

2. **优化性能**
   - 使用transform代替位置属性
   - 使用contain限制范围

3. **保持代码简洁**
   - 使用CSS变量
   - 使用简写属性

4. **测试兼容性**
   - 在不同浏览器测试
   - 使用Can I Use检查

---

**记住：技巧是工具，理解原理才是关键！** 🚀

