# 第一章：HTML基础

## 什么是HTML？

HTML (HyperText Markup Language) 是构建网页的标记语言。它定义了网页的结构和内容。

## HTML文档结构

每个HTML文档都有基本的结构：

```html
<!DOCTYPE html>           <!-- 文档类型声明 -->
<html lang="zh-CN">       <!-- 根元素，lang属性指定语言 -->
  <head>                  <!-- 头部：包含元数据和样式 -->
    <meta charset="UTF-8">
    <title>页面标题</title>
  </head>
  <body>                  <!-- 主体：可见内容 -->
    <!-- 内容在这里 -->
  </body>
</html>
```

## 常用HTML标签

### 文本标签

- `<h1>` 到 `<h6>`：标题（h1最大，h6最小）
- `<p>`：段落
- `<span>`：行内文本容器
- `<strong>`：强调（粗体）
- `<em>`：强调（斜体）
- `<br>`：换行

### 结构标签

- `<div>`：块级容器（通用）
- `<section>`：文档中的节
- `<article>`：独立的文章内容
- `<header>`：页头
- `<footer>`：页脚
- `<nav>`：导航
- `<main>`：主要内容区域

### 列表标签

- `<ul>`：无序列表
- `<ol>`：有序列表
- `<li>`：列表项

### 链接和图片

- `<a>`：链接（href属性指定URL）
- `<img>`：图片（src属性指定图片路径，alt属性提供替代文本）

### 表单标签

- `<form>`：表单容器
- `<input>`：输入框
- `<button>`：按钮
- `<label>`：标签（与输入框关联）

## HTML属性

属性提供元素的额外信息：

```html
<a href="https://example.com" target="_blank" class="link">
  链接文本
</a>
```

- `href`：链接地址
- `target`：打开方式（_blank表示新窗口）
- `class`：CSS类名（用于样式）
- `id`：唯一标识符

## 语义化HTML

使用语义化标签让代码更易读、更易维护，也利于SEO：

```html
<!-- 不好的做法 -->
<div class="header">...</div>
<div class="nav">...</div>

<!-- 好的做法 -->
<header>...</header>
<nav>...</nav>
```

## 下一步

理解HTML结构后，我们就可以开始学习如何用CSS来美化这些元素了！

