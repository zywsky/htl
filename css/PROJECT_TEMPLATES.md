# 项目模板和起始代码

## 🚀 快速开始模板

### 模板1：基础HTML5模板

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="网站描述">
    <title>页面标题</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <header>
        <!-- 页头内容 -->
    </header>
    
    <main>
        <!-- 主要内容 -->
    </main>
    
    <footer>
        <!-- 页脚内容 -->
    </footer>
</body>
</html>
```

---

### 模板2：CSS重置模板

```css
/* ============================================
   CSS重置和基础样式
   ============================================ */

/* 重置样式 */
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

/* 根元素样式 */
:root {
    /* 颜色变量 */
    --primary-color: #2196F3;
    --secondary-color: #4CAF50;
    --text-color: #333;
    --bg-color: #f5f5f5;
    
    /* 间距变量 */
    --spacing-xs: 5px;
    --spacing-sm: 10px;
    --spacing-md: 20px;
    --spacing-lg: 30px;
    
    /* 字体变量 */
    --font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    --font-size-base: 16px;
    
    /* 其他变量 */
    --border-radius: 8px;
    --shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

/* 基础样式 */
html {
    scroll-behavior: smooth;
}

body {
    font-family: var(--font-family);
    font-size: var(--font-size-base);
    line-height: 1.6;
    color: var(--text-color);
    background-color: var(--bg-color);
}

/* 链接样式 */
a {
    color: var(--primary-color);
    text-decoration: none;
    transition: color 0.3s ease;
}

a:hover {
    color: var(--secondary-color);
}

/* 图片响应式 */
img {
    max-width: 100%;
    height: auto;
    display: block;
}

/* 列表样式 */
ul, ol {
    list-style: none;
}

/* 按钮基础样式 */
button {
    border: none;
    background: none;
    cursor: pointer;
    font-family: inherit;
}

/* 输入框基础样式 */
input, textarea, select {
    font-family: inherit;
    font-size: inherit;
}
```

---

### 模板3：响应式容器模板

```css
/* ============================================
   响应式容器
   ============================================ */

.container {
    width: 100%;
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 var(--spacing-md);
}

.container-sm {
    max-width: 800px;
}

.container-lg {
    max-width: 1400px;
}

/* 响应式调整 */
@media (max-width: 768px) {
    .container {
        padding: 0 var(--spacing-sm);
    }
}
```

---

### 模板4：导航栏模板

```html
<nav class="navbar">
    <div class="container">
        <div class="logo">Logo</div>
        <button class="menu-toggle" id="menuToggle">☰</button>
        <ul class="nav-links" id="navLinks">
            <li><a href="#home">首页</a></li>
            <li><a href="#about">关于</a></li>
            <li><a href="#services">服务</a></li>
            <li><a href="#contact">联系</a></li>
        </ul>
    </div>
</nav>
```

```css
.navbar {
    background-color: white;
    box-shadow: var(--shadow);
    position: sticky;
    top: 0;
    z-index: 1000;
}

.navbar .container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--spacing-md) var(--spacing-md);
}

.nav-links {
    display: flex;
    gap: var(--spacing-lg);
    list-style: none;
}

.menu-toggle {
    display: none;
}

@media (max-width: 768px) {
    .menu-toggle {
        display: block;
    }
    
    .nav-links {
        position: absolute;
        top: 100%;
        left: 0;
        right: 0;
        flex-direction: column;
        background: white;
        padding: var(--spacing-md);
        display: none;
    }
    
    .nav-links.active {
        display: flex;
    }
}
```

---

### 模板5：卡片组件模板

```html
<div class="card">
    <div class="card-image">
        <img src="image.jpg" alt="描述">
    </div>
    <div class="card-content">
        <h3 class="card-title">卡片标题</h3>
        <p class="card-text">卡片描述内容</p>
        <a href="#" class="card-link">了解更多 →</a>
    </div>
</div>
```

```css
.card {
    background: white;
    border-radius: var(--border-radius);
    overflow: hidden;
    box-shadow: var(--shadow);
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.card:hover {
    transform: translateY(-5px);
    box-shadow: 0 5px 20px rgba(0, 0, 0, 0.2);
}

.card-image {
    width: 100%;
    height: 200px;
    overflow: hidden;
}

.card-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s ease;
}

.card:hover .card-image img {
    transform: scale(1.1);
}

.card-content {
    padding: var(--spacing-md);
}

.card-title {
    margin-bottom: var(--spacing-sm);
    color: var(--text-color);
}

.card-text {
    color: #666;
    margin-bottom: var(--spacing-md);
    line-height: 1.6;
}

.card-link {
    color: var(--primary-color);
    font-weight: 500;
}
```

---

### 模板6：按钮组件模板

```html
<button class="btn btn-primary">主要按钮</button>
<button class="btn btn-secondary">次要按钮</button>
<button class="btn btn-outline">轮廓按钮</button>
```

```css
.btn {
    display: inline-block;
    padding: 12px 24px;
    border-radius: var(--border-radius);
    font-size: var(--font-size-base);
    font-weight: 500;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s ease;
    border: 2px solid transparent;
}

.btn-primary {
    background-color: var(--primary-color);
    color: white;
}

.btn-primary:hover {
    background-color: #1976D2;
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.btn-secondary {
    background-color: var(--secondary-color);
    color: white;
}

.btn-outline {
    background-color: transparent;
    border-color: var(--primary-color);
    color: var(--primary-color);
}

.btn-outline:hover {
    background-color: var(--primary-color);
    color: white;
}
```

---

### 模板7：表单模板

```html
<form class="form">
    <div class="form-group">
        <label for="name">姓名</label>
        <input type="text" id="name" name="name" required>
    </div>
    
    <div class="form-group">
        <label for="email">邮箱</label>
        <input type="email" id="email" name="email" required>
    </div>
    
    <div class="form-group">
        <label for="message">消息</label>
        <textarea id="message" name="message" rows="5" required></textarea>
    </div>
    
    <button type="submit" class="btn btn-primary">提交</button>
</form>
```

```css
.form {
    max-width: 600px;
    margin: 0 auto;
}

.form-group {
    margin-bottom: var(--spacing-md);
}

.form-group label {
    display: block;
    margin-bottom: var(--spacing-xs);
    font-weight: 500;
    color: var(--text-color);
}

.form-group input,
.form-group textarea,
.form-group select {
    width: 100%;
    padding: 12px;
    border: 2px solid #ddd;
    border-radius: var(--border-radius);
    font-size: var(--font-size-base);
    transition: border-color 0.3s ease, box-shadow 0.3s ease;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
    outline: none;
    border-color: var(--primary-color);
    box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

.form-group textarea {
    resize: vertical;
    min-height: 120px;
}
```

---

### 模板8：页脚模板

```html
<footer class="footer">
    <div class="container">
        <div class="footer-content">
            <div class="footer-section">
                <h4>关于我们</h4>
                <p>公司简介内容</p>
            </div>
            <div class="footer-section">
                <h4>快速链接</h4>
                <ul>
                    <li><a href="#">首页</a></li>
                    <li><a href="#">关于</a></li>
                    <li><a href="#">服务</a></li>
                </ul>
            </div>
            <div class="footer-section">
                <h4>联系我们</h4>
                <p>邮箱：info@example.com</p>
                <p>电话：123-456-7890</p>
            </div>
        </div>
        <div class="footer-bottom">
            <p>&copy; 2024 公司名称. 所有权利保留.</p>
        </div>
    </div>
</footer>
```

```css
.footer {
    background-color: #2a2a2a;
    color: white;
    padding: var(--spacing-lg) 0;
    margin-top: var(--spacing-lg);
}

.footer-content {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: var(--spacing-lg);
    margin-bottom: var(--spacing-lg);
}

.footer-section h4 {
    margin-bottom: var(--spacing-md);
    color: white;
}

.footer-section ul {
    list-style: none;
}

.footer-section ul li {
    margin-bottom: var(--spacing-xs);
}

.footer-section a {
    color: #ccc;
    transition: color 0.3s ease;
}

.footer-section a:hover {
    color: white;
}

.footer-bottom {
    text-align: center;
    padding-top: var(--spacing-md);
    border-top: 1px solid #444;
    color: #999;
}
```

---

### 模板9：Hero区域模板

```html
<section class="hero">
    <div class="hero-content">
        <h1 class="hero-title">欢迎标题</h1>
        <p class="hero-subtitle">副标题描述</p>
        <a href="#cta" class="btn btn-primary">开始行动</a>
    </div>
</section>
```

```css
.hero {
    min-height: 80vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    text-align: center;
    padding: var(--spacing-lg);
}

.hero-title {
    font-size: clamp(32px, 5vw, 56px);
    margin-bottom: var(--spacing-md);
    font-weight: bold;
}

.hero-subtitle {
    font-size: clamp(18px, 2.5vw, 24px);
    margin-bottom: var(--spacing-lg);
    opacity: 0.9;
}
```

---

### 模板10：响应式网格模板

```css
/* 响应式网格 */
.grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: var(--spacing-md);
}

/* 响应式Flexbox */
.flex-container {
    display: flex;
    flex-wrap: wrap;
    gap: var(--spacing-md);
}

.flex-item {
    flex: 1 1 300px; /* 最小宽度300px */
}

/* 移动端单列 */
@media (max-width: 768px) {
    .grid,
    .flex-container {
        grid-template-columns: 1fr;
    }
    
    .flex-item {
        flex: 1 1 100%;
    }
}
```

---

## 📁 项目结构模板

```
project-name/
├── index.html
├── styles/
│   ├── main.css          # 主样式文件
│   ├── reset.css        # 重置样式
│   ├── variables.css    # CSS变量
│   ├── components.css   # 组件样式
│   └── utilities.css    # 工具类
├── images/              # 图片资源
├── js/                  # JavaScript文件
└── README.md            # 项目说明
```

---

## 🎨 主题模板

### 亮色主题

```css
:root {
    --bg-color: #ffffff;
    --text-color: #333333;
    --primary-color: #2196F3;
    --secondary-color: #4CAF50;
    --border-color: #e0e0e0;
}
```

### 暗色主题

```css
[data-theme="dark"] {
    --bg-color: #1a1a1a;
    --text-color: #e0e0e0;
    --primary-color: #64B5F6;
    --secondary-color: #81C784;
    --border-color: #444444;
}
```

---

## 💡 使用建议

1. **复制需要的模板**
2. **根据项目需求修改**
3. **保持代码风格一致**
4. **添加必要的注释**
5. **测试响应式效果**

---

**提示：将这些模板保存到你的代码库中，快速开始新项目！** 🚀

