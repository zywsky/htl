# HTL 实际项目案例

## 案例研究概述

本指南提供真实世界中的 HTL 组件开发案例，展示如何将所学知识应用到实际项目中。

## 案例 1: 企业新闻组件

### 需求

创建一个新闻组件，支持：
- 标题、摘要、正文
- 特色图片
- 作者信息
- 发布日期
- 分类标签
- 相关文章链接

### 实现

```html
<!-- news-article.html -->
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.news'}"></sly>

<sly data-sly-use.article="${'com.myapp.components.models.NewsArticleModel'}"></sly>

<article class="news-article ${component.cssClassNames}"
         itemscope 
         itemtype="http://schema.org/NewsArticle"
         data-sly-attribute.id="${component.id}">
    
    <header class="article-header">
        <h1 itemprop="headline">${article.title}</h1>
        <div class="article-meta">
            <time itemprop="datePublished" 
                  datetime="${article.publishDateISO}">
                ${article.formattedPublishDate}
            </time>
            <span itemprop="author" itemscope itemtype="http://schema.org/Person">
                <span itemprop="name">${article.author}</span>
            </span>
            <span data-sly-test="${article.category}">
                分类: <a href="${article.categoryUrl}">${article.category}</a>
            </span>
        </div>
    </header>
    
    <div data-sly-test="${article.hasImage}" class="article-image">
        <img src="${article.imageUrl}"
             srcset="${article.imageSrcset}"
             sizes="(max-width: 768px) 100vw, 80vw"
             alt="${article.imageAlt}"
             itemprop="image">
    </div>
    
    <div class="article-content" itemprop="articleBody">
        <p class="article-excerpt">${article.excerpt}</p>
        <div data-sly-unescape="${article.body}"></div>
    </div>
    
    <div data-sly-test="${article.tags && article.tags.size > 0}" 
         class="article-tags">
        <ul>
            <li data-sly-list="${article.tags}">
                <a href="${item.url}">${item.label}</a>
            </li>
        </ul>
    </div>
    
    <div data-sly-test="${article.relatedArticles && article.relatedArticles.size > 0}"
         class="related-articles">
        <h3>相关文章</h3>
        <ul>
            <li data-sly-list="${article.relatedArticles}">
                <a href="${item.url}">${item.title}</a>
            </li>
        </ul>
    </div>
</article>

<sly data-sly-call="${clientlib.js @ categories='myapp.components.news', async=true}"></sly>
```

## 案例 2: 产品展示组件

### 需求

创建一个产品展示组件，支持：
- 产品图片轮播
- 产品信息（名称、价格、描述）
- 规格参数
- 购买按钮
- 用户评价

### 实现

```html
<!-- product-showcase.html -->
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.product'}"></sly>

<sly data-sly-use.product="${'com.myapp.components.models.ProductModel' @ 
    productId=properties.productId}"></sly>

<div class="product-showcase ${component.cssClassNames}"
     itemscope 
     itemtype="http://schema.org/Product"
     data-sly-attribute.id="${component.id}">
    
    <div class="product-images">
        <div class="main-image">
            <img src="${product.mainImageUrl}"
                 alt="${product.name}"
                 itemprop="image">
        </div>
        <div data-sly-test="${product.images && product.images.size > 1}" 
             class="thumbnail-images">
            <img data-sly-list="${product.images}"
                 src="${item.thumbnailUrl}"
                 alt="${product.name}"
                 data-sly-attribute.data-full-image="${item.fullUrl}">
        </div>
    </div>
    
    <div class="product-info">
        <h1 itemprop="name">${product.name}</h1>
        <div class="product-rating" 
             itemprop="aggregateRating" 
             itemscope 
             itemtype="http://schema.org/AggregateRating">
            <span itemprop="ratingValue">${product.rating}</span>
            <span itemprop="reviewCount">(${product.reviewCount} 评价)</span>
        </div>
        
        <div class="product-price" 
             itemprop="offers" 
             itemscope 
             itemtype="http://schema.org/Offer">
            <span class="current-price" itemprop="price">$${product.formattedPrice}</span>
            <span data-sly-test="${product.originalPrice}" 
                  class="original-price">$${product.originalPrice}</span>
            <span data-sly-test="${product.isOnSale}" class="sale-badge">特价</span>
            <meta itemprop="priceCurrency" content="USD">
            <link itemprop="availability" href="http://schema.org/InStock">
        </div>
        
        <div class="product-description" itemprop="description">
            ${product.description}
        </div>
        
        <div data-sly-test="${product.specifications && product.specifications.size > 0}"
             class="product-specifications">
            <h3>规格参数</h3>
            <dl>
                <dt data-sly-list="${product.specifications}">
                    ${item.name}:
                    <dd>${item.value}</dd>
                </dt>
            </dl>
        </div>
        
        <div class="product-actions">
            <button class="btn-add-to-cart" 
                    data-product-id="${product.id}">
                加入购物车
            </button>
            <button class="btn-buy-now" 
                    data-product-id="${product.id}">
                立即购买
            </button>
        </div>
    </div>
</div>

<sly data-sly-call="${clientlib.js @ categories='myapp.components.product', async=true}"></sly>
```

## 案例 3: 动态内容列表组件

### 需求

创建一个动态内容列表组件，支持：
- 从指定路径获取内容
- 过滤和排序
- 分页
- 多种显示模式（列表、网格、卡片）

### 实现

```html
<!-- dynamic-content-list.html -->
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.content-list'}"></sly>

<sly data-sly-use.list="${'com.myapp.components.models.ContentListModel' @ 
    rootPath=properties.rootPath,
    contentType=properties.contentType,
    limit=properties.limit,
    sortBy=properties.sortBy,
    displayMode=properties.displayMode}"></sly>

<section class="content-list-component ${component.cssClassNames} display-${list.displayMode}"
         data-sly-attribute.id="${component.id}">
    
    <header class="list-header">
        <h2 data-sly-test="${list.title}">${list.title}</h2>
        <div class="list-controls">
            <select class="sort-selector">
                <option value="date-desc">最新</option>
                <option value="date-asc">最早</option>
                <option value="title-asc">标题 A-Z</option>
                <option value="title-desc">标题 Z-A</option>
            </select>
            <div class="view-toggle">
                <button class="view-list" aria-label="列表视图">列表</button>
                <button class="view-grid" aria-label="网格视图">网格</button>
                <button class="view-cards" aria-label="卡片视图">卡片</button>
            </div>
        </div>
    </header>
    
    <div class="list-content">
        <div data-sly-test="${list.hasItems}">
            <ul class="content-items">
                <li data-sly-list="${list.items}" class="content-item">
                    <article class="item-${list.displayMode}">
                        <div data-sly-test="${item.image}" class="item-image">
                            <img src="${item.imageUrl}" alt="${item.title}">
                        </div>
                        <div class="item-content">
                            <h3><a href="${item.url}">${item.title}</a></h3>
                            <p data-sly-test="${item.excerpt}">${item.excerpt}</p>
                            <div class="item-meta">
                                <span>${item.author}</span>
                                <time datetime="${item.dateISO}">${item.formattedDate}</time>
                            </div>
                        </div>
                    </article>
                </li>
            </ul>
        </div>
        <div data-sly-test="${!list.hasItems}" class="empty-state">
            <p>暂无内容</p>
        </div>
    </div>
    
    <nav data-sly-test="${list.showPagination && list.hasMorePages}" 
         class="pagination">
        <a data-sly-test="${list.hasPreviousPage}"
           href="${list.previousPageUrl}"
           class="pagination-link prev">上一页</a>
        <span class="pagination-info">
            第 ${list.currentPage} 页，共 ${list.totalPages} 页
        </span>
        <a data-sly-test="${list.hasNextPage}"
           href="${list.nextPageUrl}"
           class="pagination-link next">下一页</a>
    </nav>
</section>

<sly data-sly-call="${clientlib.js @ categories='myapp.components.content-list', async=true}"></sly>
```

## 案例 4: 表单组件

### 需求

创建一个可配置的表单组件，支持：
- 动态字段配置
- 表单验证
- 提交处理
- 成功/错误消息

### 实现

```html
<!-- form-component.html -->
<sly data-sly-use.clientlib="${'/libs/granite/sightly/templates/clientlib.html'}"></sly>
<sly data-sly-call="${clientlib.css @ categories='myapp.components.form'}"></sly>

<sly data-sly-use.form="${'com.myapp.components.models.FormModel' @ 
    formId=properties.formId,
    request=request}"></sly>

<div class="form-component ${component.cssClassNames}"
     data-sly-attribute="${{
         'id': component.id,
         'data-form-id': form.formId
     }}">
    
    <h2 data-sly-test="${form.title}">${form.title}</h2>
    <p data-sly-test="${form.description}">${form.description}</p>
    
    <div data-sly-test="${form.showSuccessMessage}" 
         class="form-message form-success"
         role="alert">
        ${form.successMessage}
    </div>
    
    <div data-sly-test="${form.showErrorMessage}" 
         class="form-message form-error"
         role="alert">
        ${form.errorMessage}
    </div>
    
    <form action="${form.actionUrl}" 
          method="${form.method}"
          novalidate
          data-sly-attribute.data-validate="${form.clientSideValidation}">
        
        <div data-sly-list="${form.fields}">
            <div class="form-field">
                <label for="${item.fieldId}">
                    ${item.label}
                    <span data-sly-test="${item.required}" 
                          class="required" 
                          aria-label="必填">*</span>
                </label>
                
                <input data-sly-test="${item.type != 'textarea' && item.type != 'select'}"
                       type="${item.type}"
                       id="${item.fieldId}"
                       name="${item.name}"
                       data-sly-attribute="${{
                           'required': item.required,
                           'aria-required': item.required,
                           'aria-describedby': item.helpId + ' ' + item.errorId,
                           'aria-invalid': item.hasError,
                           'value': item.value,
                           'placeholder': item.placeholder
                       }}">
                
                <textarea data-sly-test="${item.type == 'textarea'}"
                          id="${item.fieldId}"
                          name="${item.name}"
                          data-sly-attribute="${{
                              'required': item.required,
                              'aria-required': item.required,
                              'aria-describedby': item.helpId + ' ' + item.errorId,
                              'aria-invalid': item.hasError,
                              'placeholder': item.placeholder
                          }}">${item.value}</textarea>
                
                <select data-sly-test="${item.type == 'select'}"
                        id="${item.fieldId}"
                        name="${item.name}"
                        data-sly-attribute="${{
                            'required': item.required,
                            'aria-required': item.required,
                            'aria-describedby': item.helpId + ' ' + item.errorId,
                            'aria-invalid': item.hasError
                        }}">
                    <option value="">请选择</option>
                    <option data-sly-list="${item.options}"
                            value="${item.value}"
                            data-sly-attribute.selected="${item.selected}">
                        ${item.label}
                    </option>
                </select>
                
                <span data-sly-test="${item.helpText}"
                      id="${item.helpId}"
                      class="help-text">
                    ${item.helpText}
                </span>
                
                <span data-sly-test="${item.errorMessage}"
                      id="${item.errorId}"
                      class="error-message"
                      role="alert">
                    ${item.errorMessage}
                </span>
            </div>
        </div>
        
        <div class="form-actions">
            <button type="submit">${form.submitLabel || '提交'}</button>
            <button data-sly-test="${form.showResetButton}"
                    type="reset">${form.resetLabel || '重置'}</button>
        </div>
    </form>
</div>

<sly data-sly-call="${clientlib.js @ categories='myapp.components.form', defer=true}"></sly>
```

## 案例总结

### 关键要点

1. **使用 Sling Models**: 将业务逻辑放在 Models 中
2. **语义化 HTML**: 使用正确的 HTML 元素
3. **结构化数据**: 使用 Schema.org 标记
4. **可访问性**: 添加 ARIA 属性
5. **响应式设计**: 确保移动端友好
6. **性能优化**: 使用懒加载、异步加载
7. **错误处理**: 处理空状态和错误情况
8. **客户端库**: 正确管理 CSS 和 JS

### 最佳实践

- 保持组件职责单一
- 使用有意义的变量名
- 提供默认值
- 验证输入
- 文档化组件
- 编写测试

这些案例展示了如何在实际项目中应用 HTL 知识，创建生产级的组件。

