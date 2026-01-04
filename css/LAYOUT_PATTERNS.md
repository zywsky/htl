# 常见布局模式

## 📐 经典布局模式

### 1. 圣杯布局（Holy Grail Layout）

**特点：**
- Header、Footer全宽
- 中间三列：Sidebar、Main、Aside
- Main内容优先加载

**实现方式：**

#### Flexbox实现
```css
.layout {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
}

.header,
.footer {
    flex: 0 0 auto;
}

.main-content {
    display: flex;
    flex: 1;
}

.sidebar {
    flex: 0 0 200px;
    order: -1;  /* 移到前面 */
}

.main {
    flex: 1;
}

.aside {
    flex: 0 0 200px;
}
```

#### Grid实现
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

.header { grid-area: header; }
.sidebar { grid-area: sidebar; }
.main { grid-area: main; }
.aside { grid-area: aside; }
.footer { grid-area: footer; }
```

---

### 2. 双栏布局（Two Column Layout）

**特点：**
- 侧边栏 + 主内容
- 响应式：移动端堆叠

**实现：**

```css
.two-column {
    display: grid;
    grid-template-columns: 250px 1fr;
    gap: 20px;
}

@media (max-width: 768px) {
    .two-column {
        grid-template-columns: 1fr;
    }
}
```

---

### 3. 卡片网格布局（Card Grid）

**特点：**
- 响应式网格
- 自动换行
- 等宽卡片

**实现：**

```css
.card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
}
```

---

### 4. 瀑布流布局（Masonry Layout）

**特点：**
- 不同高度的卡片
- 像砖墙一样排列

**实现：**

```css
.masonry {
    column-count: 3;
    column-gap: 20px;
}

.masonry-item {
    break-inside: avoid;
    margin-bottom: 20px;
}

@media (max-width: 768px) {
    .masonry {
        column-count: 1;
    }
}
```

---

### 5. 居中布局（Centered Layout）

**特点：**
- 内容居中
- 最大宽度限制
- 响应式

**实现：**

```css
.centered {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
}
```

---

### 6. 全屏布局（Full Screen Layout）

**特点：**
- 占据整个视口
- 常用于登录页、Landing Page

**实现：**

```css
.fullscreen {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
}
```

---

### 7. 粘性导航布局（Sticky Navigation）

**特点：**
- 导航栏固定在顶部
- 滚动时保持可见

**实现：**

```css
.navbar {
    position: sticky;
    top: 0;
    z-index: 1000;
    background: white;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
```

---

### 8. 侧边栏布局（Sidebar Layout）

**特点：**
- 可折叠侧边栏
- 主内容区域自适应

**实现：**

```css
.sidebar-layout {
    display: grid;
    grid-template-columns: 250px 1fr;
    gap: 20px;
}

.sidebar {
    position: sticky;
    top: 20px;
    height: fit-content;
}

@media (max-width: 768px) {
    .sidebar-layout {
        grid-template-columns: 1fr;
    }
    
    .sidebar {
        position: static;
    }
}
```

---

## 🎨 现代布局模式

### 9. 不对称布局（Asymmetric Layout）

**特点：**
- 打破传统对称
- 更具视觉冲击力

**实现：**

```css
.asymmetric {
    display: grid;
    grid-template-columns: 1fr 2fr 1fr;
    gap: 20px;
}

.featured {
    grid-column: 1 / 3;
    grid-row: 1 / 3;
}
```

---

### 10. 分屏布局（Split Screen）

**特点：**
- 左右或上下分屏
- 常用于对比展示

**实现：**

```css
.split-screen {
    display: grid;
    grid-template-columns: 1fr 1fr;
    min-height: 100vh;
}

@media (max-width: 768px) {
    .split-screen {
        grid-template-columns: 1fr;
    }
}
```

---

### 11. 杂志布局（Magazine Layout）

**特点：**
- 多列文本
- 图片穿插
- 类似杂志排版

**实现：**

```css
.magazine {
    column-count: 3;
    column-gap: 30px;
    column-rule: 2px solid #ddd;
}

.magazine img {
    width: 100%;
    break-inside: avoid;
    margin: 20px 0;
}
```

---

### 12. 仪表板布局（Dashboard Layout）

**特点：**
- 多个小部件
- 可拖拽排列
- 响应式网格

**实现：**

```css
.dashboard {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
    padding: 20px;
}

.widget {
    background: white;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
```

---

## 📱 移动端布局模式

### 13. 底部导航（Bottom Navigation）

**特点：**
- 固定在底部
- 常用移动端导航

**实现：**

```css
.bottom-nav {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    display: flex;
    justify-content: space-around;
    background: white;
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
    padding: 10px 0;
    z-index: 1000;
}
```

---

### 14. 汉堡菜单（Hamburger Menu）

**特点：**
- 移动端常用
- 点击展开/收起

**实现：**

```css
.menu-toggle {
    display: none;
}

.menu {
    display: flex;
}

@media (max-width: 768px) {
    .menu-toggle {
        display: block;
    }
    
    .menu {
        position: fixed;
        top: 0;
        left: -100%;
        width: 80%;
        height: 100vh;
        background: white;
        transition: left 0.3s;
    }
    
    .menu.active {
        left: 0;
    }
}
```

---

### 15. 滑动卡片（Swipeable Cards）

**特点：**
- 可滑动切换
- 常用于移动端

**实现：**

```css
.card-container {
    display: flex;
    overflow-x: auto;
    scroll-snap-type: x mandatory;
    -webkit-overflow-scrolling: touch;
}

.card {
    flex: 0 0 100%;
    scroll-snap-align: start;
}
```

---

## 🎯 特殊布局模式

### 16. 等宽列（Equal Width Columns）

**实现：**

```css
.equal-columns {
    display: flex;
}

.equal-columns > * {
    flex: 1;
}
```

---

### 17. 等高列（Equal Height Columns）

**实现：**

```css
.equal-height {
    display: flex;
    align-items: stretch;  /* 默认值 */
}

.equal-height > * {
    /* 自动等高 */
}
```

---

### 18. 响应式图片网格

**实现：**

```css
.image-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 10px;
}

.image-grid img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}
```

---

### 19. 固定侧边栏 + 滚动内容

**实现：**

```css
.layout {
    display: grid;
    grid-template-columns: 250px 1fr;
    height: 100vh;
}

.sidebar {
    overflow-y: auto;
}

.content {
    overflow-y: auto;
}
```

---

### 20. 全屏背景 + 居中内容

**实现：**

```css
.hero {
    position: relative;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
}

.hero::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url('bg.jpg') center/cover;
    z-index: -1;
}

.hero-content {
    position: relative;
    z-index: 1;
}
```

---

## 💡 布局选择建议

### 简单布局
- **单列**：使用默认流式布局
- **双列**：使用Grid或Flexbox
- **居中**：使用margin: auto或Flexbox

### 复杂布局
- **多列网格**：使用Grid
- **响应式**：使用auto-fit和minmax
- **卡片**：使用Grid或Flexbox

### 特殊需求
- **等高列**：使用Flexbox或Grid
- **瀑布流**：使用column-count
- **粘性元素**：使用position: sticky

---

## ✅ 布局最佳实践

1. **移动优先**
   - 先设计移动端
   - 逐步增强到桌面端

2. **使用Grid和Flexbox**
   - Grid用于二维布局
   - Flexbox用于一维布局

3. **响应式设计**
   - 使用相对单位
   - 使用媒体查询
   - 测试不同设备

4. **性能优化**
   - 避免过度嵌套
   - 使用contain限制范围
   - 优化重排和重绘

---

**记住：选择合适的布局模式，让代码更简洁、更易维护！** 📐

