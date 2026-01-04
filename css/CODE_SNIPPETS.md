# CSS代码片段库

## 🎨 常用代码片段

### 居中

#### 完全居中（Flexbox）
```css
.center {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
}
```

#### 完全居中（Grid）
```css
.center {
    display: grid;
    place-items: center;
    height: 100vh;
}
```

#### 水平居中（块级元素）
```css
.center {
    width: 600px;
    margin: 0 auto;
}
```

#### 文本居中
```css
.text-center {
    text-align: center;
}
```

---

### 清除浮动

```css
.clearfix::after {
    content: "";
    display: table;
    clear: both;
}
```

---

### 文本省略

#### 单行省略
```css
.text-ellipsis {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
```

#### 多行省略（2行）
```css
.text-ellipsis-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
```

---

### 隐藏元素

```css
/* 完全隐藏，不占据空间 */
.hidden {
    display: none;
}

/* 隐藏但占据空间 */
.invisible {
    visibility: hidden;
}

/* 屏幕阅读器可见 */
.sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border-width: 0;
}
```

---

### 响应式图片

```css
.responsive-img {
    max-width: 100%;
    height: auto;
    display: block;
}
```

---

### 卡片样式

```css
.card {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
    padding: 20px;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.card:hover {
    transform: translateY(-5px);
    box-shadow: 0 5px 20px rgba(0, 0, 0, 0.2);
}
```

---

### 按钮样式

```css
.btn {
    display: inline-block;
    padding: 12px 24px;
    border: none;
    border-radius: 5px;
    font-size: 16px;
    font-weight: bold;
    text-decoration: none;
    cursor: pointer;
    transition: all 0.3s ease;
}

.btn-primary {
    background-color: #2196F3;
    color: white;
}

.btn-primary:hover {
    background-color: #1976D2;
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.btn-primary:active {
    transform: translateY(0);
}
```

---

### 输入框样式

```css
.input {
    width: 100%;
    padding: 12px;
    border: 2px solid #ddd;
    border-radius: 5px;
    font-size: 16px;
    transition: border-color 0.3s ease, box-shadow 0.3s ease;
}

.input:focus {
    outline: none;
    border-color: #2196F3;
    box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}
```

---

### 加载动画

#### 旋转加载器
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
    width: 40px;
    height: 40px;
    border: 4px solid #f3f3f3;
    border-top: 4px solid #2196F3;
    border-radius: 50%;
    animation: spin 1s linear infinite;
}
```

#### 点加载器
```css
@keyframes dot-bounce {
    0%, 80%, 100% {
        transform: scale(0);
    }
    40% {
        transform: scale(1);
    }
}

.loader-dots {
    display: flex;
    gap: 8px;
}

.loader-dots span {
    width: 12px;
    height: 12px;
    background-color: #2196F3;
    border-radius: 50%;
    animation: dot-bounce 1.4s ease-in-out infinite;
}

.loader-dots span:nth-child(1) { animation-delay: 0s; }
.loader-dots span:nth-child(2) { animation-delay: 0.2s; }
.loader-dots span:nth-child(3) { animation-delay: 0.4s; }
```

---

### 渐变背景

```css
/* 线性渐变 */
.gradient-linear {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* 径向渐变 */
.gradient-radial {
    background: radial-gradient(circle, #4facfe 0%, #00f2fe 100%);
}

/* 多色渐变 */
.gradient-multi {
    background: linear-gradient(
        90deg,
        #f093fb 0%,
        #f5576c 50%,
        #4facfe 100%
    );
}
```

---

### 阴影效果

```css
.shadow-sm {
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.shadow-md {
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.shadow-lg {
    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
}

.shadow-xl {
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}
```

---

### 响应式容器

```css
.container {
    width: 100%;
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
}

@media (max-width: 768px) {
    .container {
        padding: 0 15px;
    }
}
```

---

### 粘性导航栏

```css
.navbar {
    position: sticky;
    top: 0;
    background: white;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
    z-index: 1000;
}
```

---

### 响应式网格

```css
.grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
}
```

---

### 响应式Flexbox

```css
.flex-container {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
}

.flex-item {
    flex: 1 1 300px; /* 最小宽度300px */
}
```

---

### 模态框背景

```css
.modal-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 1000;
}
```

---

### 工具提示（Tooltip）

```css
.tooltip {
    position: relative;
}

.tooltip::after {
    content: attr(data-tooltip);
    position: absolute;
    bottom: 100%;
    left: 50%;
    transform: translateX(-50%);
    padding: 8px 12px;
    background: #333;
    color: white;
    border-radius: 4px;
    font-size: 14px;
    white-space: nowrap;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.3s;
}

.tooltip:hover::after {
    opacity: 1;
}
```

---

### 分割线

```css
.divider {
    height: 1px;
    background: linear-gradient(
        to right,
        transparent,
        #ddd,
        transparent
    );
    margin: 20px 0;
}
```

---

### 滚动条样式

```css
/* Webkit浏览器（Chrome, Safari） */
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

### 平滑滚动

```css
html {
    scroll-behavior: smooth;
}
```

---

### 禁用选择

```css
.no-select {
    user-select: none;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
}
```

---

### 全屏背景

```css
.fullscreen-bg {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: url('image.jpg') center/cover;
    z-index: -1;
}
```

---

### 响应式字体

```css
.responsive-text {
    font-size: clamp(16px, 4vw, 24px);
}
```

---

### 玻璃态效果（Glassmorphism）

```css
.glass {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 10px;
}
```

---

### 骨架屏（Skeleton）

```css
@keyframes skeleton-loading {
    0% {
        background-position: -200px 0;
    }
    100% {
        background-position: calc(200px + 100%) 0;
    }
}

.skeleton {
    background: linear-gradient(
        90deg,
        #f0f0f0 0px,
        #e0e0e0 40px,
        #f0f0f0 80px
    );
    background-size: 200px 100%;
    animation: skeleton-loading 1.5s infinite;
}
```

---

### 响应式表格

```css
@media (max-width: 768px) {
    table {
        display: block;
        overflow-x: auto;
        white-space: nowrap;
    }
}
```

---

### 固定页脚

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

## 💡 使用建议

1. **复制需要的代码片段**
2. **根据项目需求修改**
3. **保持代码风格一致**
4. **添加必要的注释**
5. **测试兼容性**

---

**提示：将这些代码片段保存到你的代码库中，方便随时使用！** 📚

