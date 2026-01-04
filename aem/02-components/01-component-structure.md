# AEM 组件开发：第一部分 - 组件结构

## 📖 什么是 AEM 组件?

AEM 组件是可重用的内容块，用于构建页面。每个组件都包含：
- **对话框** (Dialog): 用于编辑组件内容的 UI
- **模板/脚本**: 用于渲染组件的视图（HTL/JSP）
- **节点定义**: 组件的结构定义

## 🏗️ 组件结构

一个标准的 AEM 组件包含以下文件：

```
/apps/myproject/components/mycomponent/
├── .content.xml              # 组件节点定义
├── _cq_dialog/
│   └── .content.xml          # 组件对话框定义
├── mycomponent.html          # HTL 模板（主视图）
├── mycomponent.css           # 组件样式
└── mycomponent.js            # 组件 JavaScript
```

## 💻 创建第一个组件

### 示例 1：基础文本组件

#### 1. 组件节点定义 (.content.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:Component"
          jcr:title="My Text Component"
          jcr:description="一个简单的文本组件示例"
          sling:resourceSuperType="core/wcm/components/text/v2/text"
          componentGroup="MyProject - Content">
    
    <!-- 
    jcr:primaryType: 节点类型，必须是 cq:Component
    jcr:title: 组件在组件浏览器中显示的名称
    jcr:description: 组件的描述
    sling:resourceSuperType: 组件继承的父组件（可选）
    componentGroup: 组件在组件浏览器中的分组
    -->
</jcr:root>
```

#### 2. 组件对话框 (_cq_dialog/.content.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
          jcr:primaryType="nt:unstructured"
          jcr:title="Text Component"
          sling:resourceType="cq/gui/components/authoring/dialog">
    
    <content jcr:primaryType="nt:unstructured"
             sling:resourceType="granite/ui/components/coral/foundation/container">
        
        <items jcr:primaryType="nt:unstructured">
            
            <!-- 文本字段 -->
            <text jcr:primaryType="nt:unstructured"
                  sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                  fieldLabel="文本内容"
                  name="./text"
                  required="{Boolean}true">
                
                <!-- 字段描述 -->
                <granite:data
                    jcr:primaryType="nt:unstructured"
                    cq-msm-lockable="text"/>
            </text>
            
            <!-- 标题字段 -->
            <title jcr:primaryType="nt:unstructured"
                   sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                   fieldLabel="标题"
                   name="./title">
            </title>
            
        </items>
    </content>
</jcr:root>
```

#### 3. HTL 模板 (mycomponent.html)

```html
<!--
    HTL (HTML Template Language) 模板
    这是 AEM 推荐的模板语言，用于渲染组件
-->
<sly data-sly-use.component="com.example.core.models.TextComponentModel"
     data-sly-use.page="com.adobe.cq.wcm.core.components.models.Page">
    
    <!-- 
    data-sly-use: 引入 Java 对象（Sling Model）
    component: 组件的业务逻辑对象
    page: 页面对象（AEM 核心组件提供）
    -->
    
    <div class="text-component" 
         data-component-path="${resource.path}">
        
        <!-- 如果标题存在，显示标题 -->
        <h2 data-sly-test="${component.title}">${component.title}</h2>
        
        <!-- 
        data-sly-test: 条件判断，如果条件为真，渲染元素
        ${component.title}: 输出表达式，显示属性的值
        -->
        
        <!-- 显示文本内容，使用上下文感知 XSS 保护 -->
        <div class="text-component__content">
            ${component.text @ context='html'}
            <!-- 
            @ context='html': 上下文感知，自动进行 XSS 防护
            其他上下文：'text', 'html', 'attribute', 'uri', 'script', 'style', 'json'
            -->
        </div>
        
    </div>
</sly>

<!-- 
样式和脚本可以通过客户端库包含
或者直接在组件目录中定义
-->
```

#### 4. Sling Model (TextComponentModel.java)

```java
package com.example.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * 文本组件的 Sling Model
 * 
 * @Model: 标识这是一个 Sling Model
 * adaptables: 可以适配的资源类型（Resource 或 SlingHttpServletRequest）
 * defaultInjectionStrategy: 默认注入策略（可选、必需、默认）
 */
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class},
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TextComponentModel {

    /**
     * @SlingObject: 注入 Sling 对象
     * 注入当前资源
     */
    @SlingObject
    private Resource resource;

    /**
     * @ValueMapValue: 从资源的 ValueMap 中注入值
     * 如果属性不存在，返回 null（因为使用 OPTIONAL 策略）
     */
    @ValueMapValue
    private String text;

    @ValueMapValue
    private String title;

    /**
     * 获取文本内容
     * 
     * @return 文本内容，如果不存在返回默认值
     */
    public String getText() {
        return text != null ? text : "";
    }

    /**
     * 获取标题
     * 
     * @return 标题，如果不存在返回 null
     */
    public String getTitle() {
        return title;
    }

    /**
     * 检查组件是否有内容
     * 
     * @return 如果文本不为空返回 true
     */
    public boolean isEmpty() {
        return getText().isEmpty();
    }
}
```

### 示例 2：图片组件

#### 1. 组件对话框（包含图片上传）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
          jcr:primaryType="nt:unstructured"
          jcr:title="Image Component"
          sling:resourceType="cq/gui/components/authoring/dialog">
    
    <content jcr:primaryType="nt:unstructured"
             sling:resourceType="granite/ui/components/coral/foundation/container">
        
        <items jcr:primaryType="nt:unstructured">
            
            <!-- 图片上传字段 -->
            <fileupload jcr:primaryType="nt:unstructured"
                        sling:resourceType="cq/gui/components/authoring/dialog/fileupload"
                        fieldLabel="选择图片"
                        name="./file"
                        fileNameParameter="./fileFileName"
                        fileReferenceParameter="./fileReference"
                        allowUpload="{Boolean}true"
                        autoStart="{Boolean}false"
                        multiple="{Boolean}false"
                        mimeTypes="[image/gif,image/jpeg,image/png,image/webp]"
                        sizeLimit="{Long}2097152">
            </fileupload>
            
            <!-- 图片描述（替代文本） -->
            <alt jcr:primaryType="nt:unstructured"
                 sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                 fieldLabel="图片描述（替代文本）"
                 name="./alt"
                 required="{Boolean}true">
            </alt>
            
            <!-- 链接地址 -->
            <link jcr:primaryType="nt:unstructured"
                  sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                  fieldLabel="链接地址"
                  name="./link">
            </link>
            
        </items>
    </content>
</jcr:root>
```

#### 2. HTL 模板 (image.html)

```html
<!--
    图片组件 HTL 模板
-->
<sly data-sly-use.component="com.example.core.models.ImageComponentModel">
    
    <div class="image-component" data-component-path="${resource.path}">
        
        <!-- 检查是否有图片 -->
        <sly data-sly-test="${component.imagePath}">
            
            <!-- 如果有链接，包装在 <a> 标签中 -->
            <sly data-sly-test="${component.link}">
                <a href="${component.link}" 
                   class="image-component__link"
                   data-sly-attribute.aria-label="${component.alt}">
                    
                    <img src="${component.imagePath}" 
                         alt="${component.alt @ context='attribute'}"
                         class="image-component__image"
                         loading="lazy"/>
                    
                    <!-- 
                    data-sly-attribute.aria-label: 
                    条件性地添加 aria-label 属性
                    -->
                </a>
            </sly>
            
            <!-- 如果没有链接，直接显示图片 -->
            <sly data-sly-test="${!component.link}">
                <img src="${component.imagePath}" 
                     alt="${component.alt @ context='attribute'}"
                     class="image-component__image"
                     loading="lazy"/>
            </sly>
            
        </sly>
        
        <!-- 如果没有图片，显示占位符 -->
        <sly data-sly-test="${!component.imagePath}">
            <div class="image-component__placeholder">
                <p>请选择图片</p>
            </div>
        </sly>
        
    </div>
</sly>
```

#### 3. Sling Model (ImageComponentModel.java)

```java
package com.example.core.models;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * 图片组件的 Sling Model
 */
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class},
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ImageComponentModel {

    @SlingObject
    private Resource resource;

    @SlingObject
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String fileReference;

    @ValueMapValue
    private String alt;

    @ValueMapValue
    private String link;

    /**
     * 获取图片路径
     * 
     * @return 图片的完整 URL 路径
     */
    public String getImagePath() {
        if (StringUtils.isNotBlank(fileReference)) {
            // fileReference 通常是 DAM 资源的路径（如 /content/dam/myproject/image.jpg）
            // 需要转换为可访问的 URL
            return fileReference + "/jcr:content/renditions/original.img.png";
        }
        
        // 如果 fileReference 不存在，检查是否有文件上传到组件节点
        Resource imageResource = resource.getChild("file");
        if (imageResource != null) {
            return imageResource.getPath();
        }
        
        return null;
    }

    /**
     * 获取替代文本
     * 
     * @return 图片的 alt 属性值
     */
    public String getAlt() {
        return StringUtils.isNotBlank(alt) ? alt : "图片";
    }

    /**
     * 获取链接地址
     * 
     * @return 链接 URL
     */
    public String getLink() {
        return link;
    }

    /**
     * 检查是否有图片
     * 
     * @return 如果图片路径存在返回 true
     */
    public boolean hasImage() {
        return StringUtils.isNotBlank(getImagePath());
    }
}
```

### 示例 3：列表组件（使用子节点）

#### 1. 组件对话框

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
          jcr:primaryType="nt:unstructured"
          jcr:title="List Component"
          sling:resourceType="cq/gui/components/authoring/dialog">
    
    <content jcr:primaryType="nt:unstructured"
             sling:resourceType="granite/ui/components/coral/foundation/container">
        
        <items jcr:primaryType="nt:unstructured">
            
            <!-- 标题字段 -->
            <title jcr:primaryType="nt:unstructured"
                   sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                   fieldLabel="列表标题"
                   name="./title">
            </title>
            
            <!-- 使用多字段 (multifield) 来添加列表项 -->
            <items jcr:primaryType="nt:unstructured"
                   sling:resourceType="granite/ui/components/coral/foundation/form/multifield"
                   fieldLabel="列表项"
                   required="{Boolean}false">
                
                <field jcr:primaryType="nt:unstructured"
                       sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                       name="./items"
                       placeholder="输入列表项"/>
            </items>
            
        </items>
    </content>
</jcr:root>
```

#### 2. HTL 模板 (list.html)

```html
<!--
    列表组件 HTL 模板
-->
<sly data-sly-use.component="com.example.core.models.ListComponentModel">
    
    <div class="list-component" data-component-path="${resource.path}">
        
        <!-- 显示标题 -->
        <h3 data-sly-test="${component.title}">${component.title}</h3>
        
        <!-- 显示列表 -->
        <ul class="list-component__items" data-sly-list.item="${component.items}">
            <!-- 
            data-sly-list: 循环遍历集合
            item: 循环变量名
            ${component.items}: 要遍历的集合
            -->
            
            <li class="list-component__item">
                ${item @ context='text'}
            </li>
        </ul>
        
    </div>
</sly>
```

#### 3. Sling Model (ListComponentModel.java)

```java
package com.example.core.models;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 列表组件的 Sling Model
 */
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class},
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ListComponentModel {

    @SlingObject
    private Resource resource;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String[] items;

    private List<String> itemList = new ArrayList<>();

    /**
     * @PostConstruct: 在依赖注入完成后执行
     * 用于初始化或处理数据
     */
    @PostConstruct
    protected void init() {
        // 将数组转换为 List，过滤空值
        if (items != null) {
            for (String item : items) {
                if (StringUtils.isNotBlank(item)) {
                    itemList.add(item);
                }
            }
        }
    }

    /**
     * 获取标题
     * 
     * @return 列表标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取列表项
     * 
     * @return 列表项的 List
     */
    public List<String> getItems() {
        return itemList;
    }

    /**
     * 检查列表是否为空
     * 
     * @return 如果列表为空返回 true
     */
    public boolean isEmpty() {
        return itemList.isEmpty();
    }
}
```

## 📁 组件在 JCR 中的结构

当组件被添加到页面时，在 JCR 中的结构如下：

```
/content/myproject/en/home/jcr:content/par
├── component1                    ← 组件实例节点
│   ├── jcr:primaryType: nt:unstructured
│   ├── sling:resourceType: myproject/components/mycomponent
│   ├── text: "组件内容"
│   └── title: "组件标题"
└── component2
    ├── jcr:primaryType: nt:unstructured
    ├── sling:resourceType: myproject/components/mycomponent
    └── ...
```

## 🔑 关键要点

1. **组件三要素**：节点定义、对话框、模板
2. **HTL 是首选**：AEM 推荐的模板语言
3. **Sling Models**：业务逻辑应该放在 Model 中，而不是模板中
4. **组件继承**：可以使用 `sling:resourceSuperType` 继承其他组件
5. **对话框类型**：Touch UI（Granite UI）是现代化界面

## ➡️ 下一步

在下一节中，我们将深入学习 **组件对话框的开发**，学习更多 Granite UI 组件。

