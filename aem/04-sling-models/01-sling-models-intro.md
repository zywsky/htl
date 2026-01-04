# Sling Models：第一部分 - 介绍和基础

## 📖 什么是 Sling Models?

Sling Models 是一个注解驱动的框架，用于将 Sling 对象（Resource、Request 等）映射到 POJO（Plain Old Java Object）。它提供了一种简洁的方式来处理业务逻辑，将数据访问和业务逻辑从模板中分离出来。

## 🎯 Sling Models 的优势

1. **注解驱动**：使用注解简化配置
2. **依赖注入**：自动注入依赖
3. **类型安全**：编译时类型检查
4. **测试友好**：易于单元测试
5. **解耦**：业务逻辑与模板分离

## 💻 基础 Sling Model

### 示例 1：最简单的 Model

```java
package com.example.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;

/**
 * 基础 Sling Model 示例
 * 
 * @Model: 标识这是一个 Sling Model
 * adaptables: 可以适配的资源类型
 * defaultInjectionStrategy: 默认注入策略
 *   - OPTIONAL: 可选注入（如果找不到，为 null）
 *   - REQUIRED: 必需注入（如果找不到，模型无法创建）
 *   - DEFAULT: 默认策略
 */
@Model(adaptables = Resource.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class BasicModel {

    /**
     * 获取资源路径
     * 可以直接在方法中访问资源
     */
    public String getPath() {
        // 注意：这里需要资源，但还没有注入
        // 我们需要注入 Resource 或使用 @Self 注解
        return "";
    }
}
```

### 示例 2：注入资源

```java
package com.example.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * 注入资源的示例
 */
@Model(adaptables = Resource.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ResourceInjectionModel {

    /**
     * @Self: 注入适配的对象本身（Resource 或 Request）
     * 这是最常用的注入方式
     */
    @Self
    private Resource resource;

    /**
     * 获取资源路径
     */
    public String getPath() {
        return resource.getPath();
    }

    /**
     * 获取资源名称
     */
    public String getName() {
        return resource.getName();
    }

    /**
     * 获取资源类型
     */
    public String getResourceType() {
        return resource.getResourceType();
    }
}
```

### 示例 3：注入属性值

```java
package com.example.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * 注入属性值的示例
 */
@Model(adaptables = Resource.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PropertyInjectionModel {

    @Self
    private Resource resource;

    /**
     * @ValueMapValue: 从资源的 ValueMap 中注入属性值
     * name: 属性名称（可选，默认使用字段名）
     * optional: 是否可选（默认 false，如果属性不存在会报错）
     */
    @ValueMapValue
    private String title;

    @ValueMapValue(name = "jcr:title")  // 使用不同的属性名
    private String jcrTitle;

    @ValueMapValue(optional = true)     // 可选属性
    private String description;

    /**
     * 也可以注入不同类型的属性
     */
    @ValueMapValue
    private Long count;

    @ValueMapValue
    private Boolean isActive;

    @ValueMapValue
    private String[] tags;  // 多值属性

    /**
     * Getter 方法
     */
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description != null ? description : "默认描述";
    }

    public Long getCount() {
        return count != null ? count : 0L;
    }

    public Boolean getIsActive() {
        return isActive != null ? isActive : false;
    }

    public String[] getTags() {
        return tags != null ? tags : new String[0];
    }
}
```

### 示例 4：使用 @PostConstruct 初始化

```java
package com.example.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用 @PostConstruct 初始化的示例
 * 
 * @PostConstruct: 在所有依赖注入完成后执行
 * 用于数据初始化、验证、转换等操作
 */
@Model(adaptables = Resource.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PostConstructModel {

    @Self
    private Resource resource;

    @ValueMapValue
    private String[] items;

    // 初始化后的列表
    private List<String> processedItems = new ArrayList<>();

    /**
     * @PostConstruct 方法在所有注入完成后执行
     */
    @PostConstruct
    protected void init() {
        // 处理 items 数组
        if (items != null) {
            for (String item : items) {
                if (item != null && !item.trim().isEmpty()) {
                    processedItems.add(item.trim());
                }
            }
        }
    }

    /**
     * 获取处理后的列表
     */
    public List<String> getProcessedItems() {
        return processedItems;
    }

    /**
     * 检查是否有项目
     */
    public boolean hasItems() {
        return !processedItems.isEmpty();
    }
}
```

### 示例 5：注入子资源

```java
package com.example.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;

import java.util.ArrayList;
import java.util.List;

/**
 * 注入子资源的示例
 */
@Model(adaptables = Resource.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ChildResourceModel {

    @Self
    private Resource resource;

    /**
     * @ChildResource: 注入子资源
     * name: 子资源名称（可选，默认使用字段名）
     * optional: 是否可选
     */
    @ChildResource
    private Resource image;

    @ChildResource(name = "content")
    private Resource contentResource;

    /**
     * 也可以注入为另一个 Model
     */
    @ChildResource
    private ImageModel imageModel;

    /**
     * 获取所有子资源
     */
    public List<Resource> getChildren() {
        List<Resource> children = new ArrayList<>();
        resource.listChildren().forEachRemaining(children::add);
        return children;
    }

    /**
     * 获取图片路径
     */
    public String getImagePath() {
        return image != null ? image.getPath() : null;
    }

    /**
     * 获取内容资源
     */
    public Resource getContentResource() {
        return contentResource;
    }

    /**
     * 图片 Model（嵌套 Model）
     */
    @Model(adaptables = Resource.class)
    public interface ImageModel {
        @ValueMapValue
        String getSource();

        @ValueMapValue
        String getAlt();
    }
}
```

### 示例 6：注入 OSGi 服务

```java
package com.example.core.models;

import com.example.core.services.MyService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

/**
 * 注入 OSGi 服务的示例
 */
@Model(adaptables = Resource.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ServiceInjectionModel {

    /**
     * @OSGiService: 注入 OSGi 服务
     */
    @OSGiService
    private MyService myService;

    /**
     * 使用注入的服务
     */
    public String getProcessedData() {
        if (myService != null) {
            return myService.processData("example");
        }
        return "Service not available";
    }
}
```

### 示例 7：完整的组件 Model

```java
package com.example.core.models;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.Calendar;

/**
 * 完整的组件 Model 示例
 * 
 * 这个示例展示了在实际组件中如何组合使用各种注解
 */
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class},
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ArticleComponentModel {

    @Self
    private Resource resource;

    /**
     * @SlingObject: 注入 Sling 对象（Request, Response, ResourceResolver 等）
     */
    @SlingObject
    private SlingHttpServletRequest request;

    // 属性注入
    @ValueMapValue
    private String title;

    @ValueMapValue
    private String content;

    @ValueMapValue
    private String author;

    @ValueMapValue
    private Calendar publishDate;

    @ValueMapValue
    private String[] tags;

    @ValueMapValue
    private Boolean featured;

    // 计算属性
    private String formattedDate;
    private boolean isEmpty;

    @PostConstruct
    protected void init() {
        // 格式化日期
        if (publishDate != null) {
            formattedDate = formatDate(publishDate);
        }

        // 检查是否为空
        isEmpty = StringUtils.isBlank(title) && StringUtils.isBlank(content);
    }

    // Getter 方法
    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author != null ? author : "匿名";
    }

    public Calendar getPublishDate() {
        return publishDate;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public String[] getTags() {
        return tags != null ? tags : new String[0];
    }

    public Boolean getFeatured() {
        return featured != null && featured;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     * 格式化日期的辅助方法
     */
    private String formatDate(Calendar date) {
        // 简单的日期格式化示例
        return String.format("%d-%02d-%02d",
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH));
    }
}
```

## 📋 常用注入注解总结

| 注解 | 用途 | 示例 |
|------|------|------|
| `@Self` | 注入适配的对象本身 | `@Self Resource resource` |
| `@SlingObject` | 注入 Sling 对象 | `@SlingObject SlingHttpServletRequest request` |
| `@ValueMapValue` | 注入属性值 | `@ValueMapValue String title` |
| `@ChildResource` | 注入子资源 | `@ChildResource Resource child` |
| `@OSGiService` | 注入 OSGi 服务 | `@OSGiService MyService service` |
| `@RequestAttribute` | 注入请求属性 | `@RequestAttribute String attr` |
| `@ScriptVariable` | 注入脚本变量 | `@ScriptVariable Page currentPage` |

## 🔑 关键要点

1. **使用 @Model 注解**：标识这是一个 Sling Model
2. **选择合适的 adaptables**：通常是 Resource 或 SlingHttpServletRequest
3. **依赖注入**：使用注解自动注入依赖
4. **@PostConstruct**：用于初始化逻辑
5. **Getter 方法**：提供访问模型属性的方法

## ➡️ 下一步

在下一节中，我们将深入学习 **Sling Models 的高级注解和特性**。

