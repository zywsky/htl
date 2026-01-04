# AEM 基础：第三部分 - Apache Sling 框架

## 📖 什么是 Apache Sling?

Apache Sling 是一个基于 OSGi 的 Web 框架，它将 HTTP 请求映射到内容资源（JCR 节点）。Sling 使用约定优于配置的原则，通过 URL 路径直接定位到 JCR 节点，然后选择合适的脚本或 Servlet 来渲染内容。

## 🏗️ Sling 核心概念

### 1. 资源解析 (Resource Resolution)

Sling 将 URL 路径解析为资源（Resource）：

```
URL: /content/myproject/en/home.html
     ↓
资源: /content/myproject/en/home
     ↓
JCR 节点: /content/myproject/en/home
     ↓
脚本: /apps/myproject/components/page/home/home.html
```

### 2. Servlet 解析 (Servlet Resolution)

Sling 根据以下因素选择合适的 Servlet：

1. **资源类型** (`sling:resourceType`)
2. **HTTP 方法** (GET, POST, PUT, DELETE)
3. **选择器** (Selectors)
4. **扩展名** (Extension)

```
URL: /content/myproject/en/home.selector1.selector2.json
     │                                           │
     │                                           └── 扩展名：决定响应格式
     │
     └── 选择器：用于选择不同的视图或操作
```

### 3. Sling 资源 (Resource)

Resource 是 Sling 中对 JCR 节点的抽象：

```java
Resource
├── getPath()          // 获取资源路径
├── getResourceType()  // 获取资源类型
├── getResourceMetadata()  // 获取元数据
├── adaptTo(Node.class)    // 适配为 JCR 节点
└── getChild()        // 获取子资源
```

## 💻 Sling API 基础

### 示例 1：Sling Servlet 基础

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

/**
 * 基础 Sling Servlet 示例
 * 
 * SlingSafeMethodsServlet: 用于处理安全的 HTTP 方法（GET, HEAD）
 * 如果要处理 POST, PUT, DELETE，可以使用 SlingAllMethodsServlet
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "myproject/components/page/home",  // 资源类型
    methods = "GET",                                    // HTTP 方法
    extensions = "html"                                 // 扩展名
)
public class BasicSlingServlet extends SlingSafeMethodsServlet {

    /**
     * 处理 GET 请求
     * 
     * @param request Sling HTTP 请求对象
     * @param response Sling HTTP 响应对象
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, 
                        SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        // 获取当前资源
        org.apache.sling.api.resource.Resource resource = request.getResource();
        
        // 获取资源路径
        String path = resource.getPath();
        
        // 获取资源类型
        String resourceType = resource.getResourceType();
        
        // 设置响应类型
        response.setContentType("text/html;charset=UTF-8");
        
        // 写入响应
        response.getWriter().write("<html><body>");
        response.getWriter().write("<h1>Hello from Sling Servlet!</h1>");
        response.getWriter().write("<p>资源路径: " + path + "</p>");
        response.getWriter().write("<p>资源类型: " + resourceType + "</p>");
        response.getWriter().write("</body></html>");
    }
}
```

### 示例 2：使用选择器的 Servlet

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

/**
 * 使用选择器的 Servlet 示例
 * 
 * 选择器用于根据不同的需求返回不同的内容
 * 例如：.mobile.html, .print.html, .json
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "myproject/components/page/home",
    methods = "GET",
    selectors = "json",      // 选择器：当 URL 包含 .json 时触发
    extensions = "html"
)
public class JsonSelectorServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, 
                        SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        // 获取选择器数组
        String[] selectors = request.getRequestPathInfo().getSelectors();
        
        // 设置 JSON 响应类型
        response.setContentType("application/json;charset=UTF-8");
        
        // 获取资源
        org.apache.sling.api.resource.Resource resource = request.getResource();
        
        // 构建 JSON 响应
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"path\": \"").append(resource.getPath()).append("\",");
        json.append("\"resourceType\": \"").append(resource.getResourceType()).append("\",");
        json.append("\"selectors\": [");
        
        for (int i = 0; i < selectors.length; i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(selectors[i]).append("\"");
        }
        json.append("]");
        json.append("}");
        
        response.getWriter().write(json.toString());
    }
}
```

### 示例 3：处理 POST 请求

```java
package com.example.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * 处理 POST 请求的 Servlet 示例
 * 
 * SlingAllMethodsServlet: 可以处理所有 HTTP 方法
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "myproject/components/form",
    methods = {"GET", "POST"},  // 同时支持 GET 和 POST
    extensions = "html"
)
public class FormHandlerServlet extends SlingAllMethodsServlet {

    /**
     * 处理 GET 请求（显示表单）
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, 
                        SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        
        // 返回表单 HTML
        response.getWriter().write("<html><body>");
        response.getWriter().write("<form method='POST'>");
        response.getWriter().write("<input type='text' name='name' placeholder='姓名'><br>");
        response.getWriter().write("<input type='email' name='email' placeholder='邮箱'><br>");
        response.getWriter().write("<button type='submit'>提交</button>");
        response.getWriter().write("</form>");
        response.getWriter().write("</body></html>");
    }

    /**
     * 处理 POST 请求（处理表单提交）
     */
    @Override
    protected void doPost(SlingHttpServletRequest request, 
                         SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        // 获取表单参数
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        
        // 获取资源适配器（用于访问 JCR）
        org.apache.sling.api.resource.ResourceResolver resolver = request.getResourceResolver();
        
        try {
            // 这里可以保存数据到 JCR
            // 例如：创建或更新节点
            
            // 设置成功响应
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("<html><body>");
            response.getWriter().write("<h1>提交成功！</h1>");
            response.getWriter().write("<p>姓名: " + name + "</p>");
            response.getWriter().write("<p>邮箱: " + email + "</p>");
            response.getWriter().write("</body></html>");
            
        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().write("错误: " + e.getMessage());
        }
    }
}
```

### 示例 4：ResourceResolver 操作

```java
package com.example.core.services;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

/**
 * ResourceResolver 使用示例
 * 
 * ResourceResolver 是访问 JCR 资源的主要接口
 * 类似于 JCR Session，但提供了更高级的抽象
 */
@Component(service = ResourceResolverExample.class)
public class ResourceResolverExample {

    // 注入 ResourceResolverFactory
    // 这是获取 ResourceResolver 的标准方式
    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /**
     * 获取系统用户 ResourceResolver
     * 
     * @return ResourceResolver 对象
     */
    public ResourceResolver getSystemResourceResolver() throws Exception {
        // 使用系统用户凭证创建 ResourceResolver
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(ResourceResolverFactory.SUBSERVICE, "system-user");
        
        // 获取 ResourceResolver
        return resourceResolverFactory.getServiceResourceResolver(paramMap);
    }

    /**
     * 获取资源
     * 
     * @param path 资源路径
     * @return Resource 对象
     */
    public Resource getResource(String path) throws Exception {
        ResourceResolver resolver = getSystemResourceResolver();
        try {
            // 解析资源路径
            Resource resource = resolver.getResource(path);
            return resource;
        } finally {
            // 关闭 ResourceResolver（类似于 Session.logout()）
            resolver.close();
        }
    }

    /**
     * 查找资源（支持通配符）
     * 
     * @param basePath 基础路径
     * @param pattern 匹配模式（如 "*.html"）
     * @return 匹配的资源迭代器
     */
    public java.util.Iterator<Resource> findResources(String basePath, String pattern) 
            throws Exception {
        ResourceResolver resolver = getSystemResourceResolver();
        try {
            // 使用查询查找资源
            String query = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE('" + basePath + "')";
            return resolver.findResources(query, javax.jcr.query.Query.JCR_SQL2);
        } finally {
            resolver.close();
        }
    }

    /**
     * 将 Resource 适配为 JCR Node
     * 
     * @param resource Resource 对象
     * @return JCR Node 对象
     */
    public Node adaptToNode(Resource resource) {
        // Sling 适配器模式：将 Resource 适配为 Node
        return resource.adaptTo(Node.class);
    }

    /**
     * 创建资源结构
     * 
     * @param path 资源路径
     * @param resourceType 资源类型
     */
    public void createResource(String path, String resourceType) throws Exception {
        ResourceResolver resolver = getSystemResourceResolver();
        try {
            // 使用 ModifiableValueMap 创建/更新资源
            Resource parentResource = resolver.getResource(getParentPath(path));
            
            if (parentResource != null) {
                // 创建资源（实际上是在 JCR 中创建节点）
                Map<String, Object> properties = new HashMap<>();
                properties.put("sling:resourceType", resourceType);
                
                resolver.create(parentResource, getResourceName(path), properties);
                resolver.commit(); // 提交更改（类似于 session.save()）
            }
        } catch (Exception e) {
            resolver.revert(); // 如果出错，回滚更改
            throw e;
        } finally {
            resolver.close();
        }
    }

    /**
     * 更新资源属性
     * 
     * @param path 资源路径
     * @param properties 属性 Map
     */
    public void updateResource(String path, Map<String, Object> properties) throws Exception {
        ResourceResolver resolver = getSystemResourceResolver();
        try {
            Resource resource = resolver.getResource(path);
            if (resource != null) {
                // 使用 ModifiableValueMap 更新属性
                org.apache.sling.api.resource.ModifiableValueMap valueMap = 
                    resource.adaptTo(org.apache.sling.api.resource.ModifiableValueMap.class);
                
                if (valueMap != null) {
                    valueMap.putAll(properties);
                    resolver.commit();
                }
            }
        } catch (Exception e) {
            resolver.revert();
            throw e;
        } finally {
            resolver.close();
        }
    }

    // 辅助方法：获取父路径
    private String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : "/";
    }

    // 辅助方法：获取资源名称
    private String getResourceName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
```

### 示例 5：请求参数处理

```java
package com.example.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

/**
 * 请求参数处理示例
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "myproject/components/api",
    methods = "GET",
    extensions = "json"
)
public class ParameterHandlerServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, 
                        SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        // 1. 获取单个参数（字符串）
        String name = request.getParameter("name");
        
        // 2. 获取所有参数（Map）
        RequestParameterMap parameterMap = request.getRequestParameterMap();
        
        // 3. 构建响应
        response.setContentType("application/json;charset=UTF-8");
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"parameters\": {");
        
        boolean first = true;
        for (Map.Entry<String, RequestParameter[]> entry : parameterMap.entrySet()) {
            if (!first) json.append(",");
            first = false;
            
            String paramName = entry.getKey();
            RequestParameter[] params = entry.getValue();
            
            json.append("\"").append(paramName).append("\": ");
            
            if (params.length == 1) {
                // 单值参数
                json.append("\"").append(params[0].getString()).append("\"");
            } else {
                // 多值参数
                json.append("[");
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) json.append(",");
                    json.append("\"").append(params[i].getString()).append("\"");
                }
                json.append("]");
            }
        }
        
        json.append("}");
        json.append("}");
        
        response.getWriter().write(json.toString());
    }
}
```

## 🔄 Sling 资源解析流程

```
HTTP 请求
    ↓
URL: /content/myproject/en/home.html
    ↓
1. 资源解析
   查找 JCR 节点: /content/myproject/en/home
    ↓
2. 获取资源类型
   从节点的 sling:resourceType 属性获取
   例如: myproject/components/page/home
    ↓
3. Servlet 解析
   查找匹配的 Servlet:
   - 资源类型: myproject/components/page/home
   - 方法: GET
   - 扩展名: html
    ↓
4. 执行 Servlet
   调用 doGet() 方法
    ↓
5. 返回响应
   HTML/JSON/XML 等
```

## 🔑 关键要点

1. **资源即内容**：Sling 将 JCR 节点抽象为 Resource
2. **约定优于配置**：通过路径和资源类型自动定位 Servlet
3. **适配器模式**：Resource 可以适配为 Node、ValueMap 等
4. **ResourceResolver**：类似于 JCR Session，用于访问资源
5. **Servlet 选择**：根据资源类型、方法、选择器、扩展名匹配

## ➡️ 下一步

在下一部分中，我们将开始学习 **AEM 组件开发**，这是 AEM 开发的核心内容。

