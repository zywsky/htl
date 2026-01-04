# AEM 内容服务和 API：第一部分 - 内容服务基础

## 📖 什么是 AEM 内容服务?

AEM 内容服务提供了以内容为中心的 API，允许以多种格式（JSON、XML 等）交付内容，支持单页应用（SPA）和移动应用等前端技术。

## 🏗️ 内容服务核心概念

### 1. Sling Model Exporter
将 Sling Model 导出为 JSON/XML

### 2. Content Fragment
可重用的内容块，独立于页面

### 3. REST API
通过 HTTP 访问内容的 API

### 4. GraphQL API
使用 GraphQL 查询内容的 API

## 💻 Sling Model Exporter

### 示例 1：导出为 JSON 的 Sling Model

```java
package com.example.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;

/**
 * 导出为 JSON 的 Sling Model
 * 
 * @Exporter: 配置导出器
 * name: 导出器名称
 * extensions: 支持的扩展名（json, xml）
 */
@Model(adaptables = SlingHttpServletRequest.class,
       resourceType = "myproject/components/article",
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
         extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ArticleModel implements ComponentExporter {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String content;

    @ValueMapValue
    private String author;

    /**
     * 导出为 JSON 的属性
     * 只有 getter 方法会被导出
     */
    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    /**
     * ComponentExporter 接口要求的方法
     * 返回组件类型
     */
    @Override
    public String getExportedType() {
        return request.getResource().getResourceType();
    }
}
```

### 示例 2：完整的导出模型

```java
package com.example.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章组件的导出模型
 */
@Model(adaptables = SlingHttpServletRequest.class,
       resourceType = "myproject/components/article",
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
         extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ArticleExportModel implements ComponentExporter {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String content;

    @ValueMapValue
    private String author;

    @ValueMapValue
    private String[] tags;

    @ChildResource
    private Resource image;

    /**
     * 嵌套的图片模型
     */
    @Model(adaptables = Resource.class)
    public interface ImageModel {
        @ValueMapValue
        String getSrc();

        @ValueMapValue
        String getAlt();
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public String[] getTags() {
        return tags;
    }

    /**
     * 获取图片对象（嵌套导出）
     */
    public ImageModel getImage() {
        if (image != null) {
            return image.adaptTo(ImageModel.class);
        }
        return null;
    }

    /**
     * 计算属性（也会被导出）
     */
    public boolean hasImage() {
        return image != null;
    }

    @Override
    public String getExportedType() {
        return request.getResource().getResourceType();
    }
}
```

### 示例 3：使用 REST API 访问导出内容

访问导出的内容：

```
GET /content/myproject/en/home/jcr:content/par/article.model.json
```

响应示例：

```json
{
  ":type": "myproject/components/article",
  "title": "文章标题",
  "content": "文章内容",
  "author": "作者名称",
  "tags": ["标签1", "标签2"],
  "image": {
    "src": "/path/to/image.jpg",
    "alt": "图片描述"
  },
  "hasImage": true
}
```

## 📋 Sling Servlet 提供 REST API

### 示例 4：自定义 REST API Servlet

```java
package com.example.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 自定义 REST API Servlet
 * 
 * 提供 JSON 格式的 API 响应
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "myproject/api/articles",
    methods = "GET",
    extensions = "json"
)
public class ArticlesApiServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, 
                        SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        // 设置响应类型
        response.setContentType("application/json;charset=UTF-8");
        
        try {
            // 构建响应数据
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("status", "success");
            jsonResponse.addProperty("timestamp", 
                System.currentTimeMillis());
            
            // 获取查询参数
            String limit = request.getParameter("limit");
            String offset = request.getParameter("offset");
            
            // 获取文章列表（示例）
            // List<Article> articles = getArticles(limit, offset);
            // jsonResponse.add("articles", convertToJsonArray(articles));
            
            // 返回 JSON
            Gson gson = new Gson();
            response.getWriter().write(gson.toJson(jsonResponse));
            
        } catch (Exception e) {
            response.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");
            error.addProperty("message", e.getMessage());
            response.getWriter().write(new Gson().toJson(error));
        }
    }
}
```

## 🔑 关键要点

1. **@Exporter 注解**：配置 Model 导出为 JSON/XML
2. **ComponentExporter 接口**：实现此接口以支持导出
3. **getter 方法**：只有 getter 方法会被导出
4. **嵌套导出**：可以导出嵌套的 Model 对象
5. **REST API**：通过 URL 访问导出内容（.model.json）

## ➡️ 下一步

继续学习 AEM 的其他高级主题和实践。

