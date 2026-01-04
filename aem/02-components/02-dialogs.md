# AEM 组件开发：第二部分 - 对话框开发详解

## 📖 什么是组件对话框?

组件对话框是作者编辑组件内容时的用户界面。AEM 支持两种 UI：
- **Touch UI** (Granite UI / Coral UI): 现代化界面（推荐）
- **Classic UI**: 传统界面（已弃用）

## 🎨 Granite UI 组件类型

### 基础表单组件

#### 1. 文本字段 (textfield)

```xml
<field jcr:primaryType="nt:unstructured"
       sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
       fieldLabel="标题"
       name="./title"
       required="{Boolean}true"
       emptyText="请输入标题"
       maxLength="100">
    
    <!-- 字段描述 -->
    <granite:data
        jcr:primaryType="nt:unstructured"
        cq-msm-lockable="title"/>
</field>
```

#### 2. 多行文本 (textarea)

```xml
<description jcr:primaryType="nt:unstructured"
             sling:resourceType="granite/ui/components/coral/foundation/form/textarea"
             fieldLabel="描述"
             name="./description"
             rows="5"
             maxLength="500">
</description>
```

#### 3. 数字字段 (numberfield)

```xml
<price jcr:primaryType="nt:unstructured"
       sling:resourceType="granite/ui/components/coral/foundation/form/numberfield"
       fieldLabel="价格"
       name="./price"
       min="0"
       step="0.01">
</price>
```

#### 4. 选择框 (select)

```xml
<!-- 单选下拉框 -->
<category jcr:primaryType="nt:unstructured"
          sling:resourceType="granite/ui/components/coral/foundation/form/select"
          fieldLabel="分类"
          name="./category">
    
    <items jcr:primaryType="nt:unstructured">
        <option1 jcr:primaryType="nt:unstructured"
                 text="技术"
                 value="tech"/>
        <option2 jcr:primaryType="nt:unstructured"
                 text="设计"
                 value="design"/>
        <option3 jcr:primaryType="nt:unstructured"
                 text="营销"
                 value="marketing"/>
    </items>
</category>
```

#### 5. 复选框 (checkbox)

```xml
<!-- 单个复选框 -->
<featured jcr:primaryType="nt:unstructured"
          sling:resourceType="granite/ui/components/coral/foundation/form/checkbox"
          fieldLabel="推荐"
          name="./featured"
          text="设为推荐内容"
          checked="{Boolean}false"/>
```

```xml
<!-- 复选框组（多选） -->
<tags jcr:primaryType="nt:unstructured"
      sling:resourceType="granite/ui/components/coral/foundation/form/checkboxlist"
      fieldLabel="标签"
      name="./tags">
    
    <items jcr:primaryType="nt:unstructured">
        <tag1 jcr:primaryType="nt:unstructured"
              text="标签1"
              value="tag1"/>
        <tag2 jcr:primaryType="nt:unstructured"
              text="标签2"
              value="tag2"/>
        <tag3 jcr:primaryType="nt:unstructured"
              text="标签3"
              value="tag3"/>
    </items>
</tags>
```

#### 6. 单选按钮 (radio)

```xml
<status jcr:primaryType="nt:unstructured"
        sling:resourceType="granite/ui/components/coral/foundation/form/radio"
        fieldLabel="状态"
        name="./status">
    
    <items jcr:primaryType="nt:unstructured">
        <active jcr:primaryType="nt:unstructured"
                text="激活"
                value="active"/>
        <inactive jcr:primaryType="nt:unstructured"
                  text="未激活"
                  value="inactive"/>
    </items>
</status>
```

### 高级组件

#### 7. 路径字段 (pathfield) - 页面/资源选择器

```xml
<link jcr:primaryType="nt:unstructured"
      sling:resourceType="granite/ui/components/coral/foundation/form/pathfield"
      fieldLabel="链接页面"
      name="./link"
      rootPath="/content"
      filter="[hitsNot=0]">
</link>
```

#### 8. 图片上传 (fileupload)

```xml
<image jcr:primaryType="nt:unstructured"
       sling:resourceType="cq/gui/components/authoring/dialog/fileupload"
       fieldLabel="选择图片"
       name="./file"
       fileNameParameter="./fileFileName"
       fileReferenceParameter="./fileReference"
       allowUpload="{Boolean}true"
       autoStart="{Boolean}false"
       multiple="{Boolean}false"
       mimeTypes="[image/gif,image/jpeg,image/png,image/webp]"
       sizeLimit="{Long}5242880">
</image>
```

#### 9. 多字段 (multifield) - 动态列表

```xml
<items jcr:primaryType="nt:unstructured"
       sling:resourceType="granite/ui/components/coral/foundation/form/multifield"
       fieldLabel="列表项"
       composite="{Boolean}true">
    
    <!-- 复合多字段（多个字段组合） -->
    <field jcr:primaryType="nt:unstructured"
           sling:resourceType="granite/ui/components/coral/foundation/container">
        
        <items jcr:primaryType="nt:unstructured">
            <label jcr:primaryType="nt:unstructured"
                   sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                   name="./label"
                   fieldLabel="标签"/>
            
            <value jcr:primaryType="nt:unstructured"
                   sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                   name="./value"
                   fieldLabel="值"/>
        </items>
    </field>
</items>
```

#### 10. 日期选择器 (datepicker)

```xml
<publishDate jcr:primaryType="nt:unstructured"
             sling:resourceType="granite/ui/components/coral/foundation/form/datepicker"
             fieldLabel="发布日期"
             name="./publishDate"
             type="datetime"
             displayedFormat="YYYY-MM-DD HH:mm">
</publishDate>
```

#### 11. 颜色选择器 (colorfield)

```xml
<color jcr:primaryType="nt:unstructured"
       sling:resourceType="granite/ui/components/coral/foundation/form/colorfield"
       fieldLabel="颜色"
       name="./color"
       format="hex">
</color>
```

### 容器和组织组件

#### 12. 标签页 (tabs)

```xml
<tabs jcr:primaryType="nt:unstructured"
      sling:resourceType="granite/ui/components/coral/foundation/tabs"
      maximized="{Boolean}true">
    
    <items jcr:primaryType="nt:unstructured">
        
        <!-- 基本信息标签页 -->
        <basic jcr:primaryType="nt:unstructured"
               jcr:title="基本信息"
               sling:resourceType="granite/ui/components/coral/foundation/container">
            
            <items jcr:primaryType="nt:unstructured">
                <title jcr:primaryType="nt:unstructured"
                       sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                       fieldLabel="标题"
                       name="./title"/>
                
                <description jcr:primaryType="nt:unstructured"
                             sling:resourceType="granite/ui/components/coral/foundation/form/textarea"
                             fieldLabel="描述"
                             name="./description"/>
            </items>
        </basic>
        
        <!-- 高级选项标签页 -->
        <advanced jcr:primaryType="nt:unstructured"
                  jcr:title="高级选项"
                  sling:resourceType="granite/ui/components/coral/foundation/container">
            
            <items jcr:primaryType="nt:unstructured">
                <featured jcr:primaryType="nt:unstructured"
                          sling:resourceType="granite/ui/components/coral/foundation/form/checkbox"
                          fieldLabel="推荐"
                          name="./featured"/>
            </items>
        </advanced>
        
    </items>
</tabs>
```

#### 13. 字段集 (fieldset) - 分组

```xml
<fieldset jcr:primaryType="nt:unstructured"
          sling:resourceType="granite/ui/components/coral/foundation/form/fieldset"
          jcr:title="内容设置">
    
    <items jcr:primaryType="nt:unstructured">
        <title jcr:primaryType="nt:unstructured"
               sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
               fieldLabel="标题"
               name="./title"/>
    </items>
</fieldset>
```

## 💻 完整对话框示例

### 示例：文章组件对话框

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
          jcr:primaryType="nt:unstructured"
          jcr:title="文章组件"
          sling:resourceType="cq/gui/components/authoring/dialog">
    
    <content jcr:primaryType="nt:unstructured"
             sling:resourceType="granite/ui/components/coral/foundation/container">
        
        <items jcr:primaryType="nt:unstructured">
            
            <!-- 使用标签页组织内容 -->
            <tabs jcr:primaryType="nt:unstructured"
                  sling:resourceType="granite/ui/components/coral/foundation/tabs"
                  maximized="{Boolean}true">
                
                <items jcr:primaryType="nt:unstructured">
                    
                    <!-- 基本信息标签页 -->
                    <basic jcr:primaryType="nt:unstructured"
                           jcr:title="基本信息"
                           sling:resourceType="granite/ui/components/coral/foundation/container">
                        
                        <items jcr:primaryType="nt:unstructured">
                            
                            <!-- 标题 -->
                            <title jcr:primaryType="nt:unstructured"
                                   sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                                   fieldLabel="标题"
                                   name="./title"
                                   required="{Boolean}true"
                                   emptyText="请输入文章标题">
                            </title>
                            
                            <!-- 描述 -->
                            <description jcr:primaryType="nt:unstructured"
                                         sling:resourceType="granite/ui/components/coral/foundation/form/textarea"
                                         fieldLabel="描述"
                                         name="./description"
                                         rows="5"
                                         maxLength="500">
                            </description>
                            
                            <!-- 作者 -->
                            <author jcr:primaryType="nt:unstructured"
                                    sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                                    fieldLabel="作者"
                                    name="./author">
                            </author>
                            
                            <!-- 发布日期 -->
                            <publishDate jcr:primaryType="nt:unstructured"
                                         sling:resourceType="granite/ui/components/coral/foundation/form/datepicker"
                                         fieldLabel="发布日期"
                                         name="./publishDate"
                                         type="datetime">
                            </publishDate>
                            
                        </items>
                    </basic>
                    
                    <!-- 内容标签页 -->
                    <content jcr:primaryType="nt:unstructured"
                             jcr:title="内容"
                             sling:resourceType="granite/ui/components/coral/foundation/container">
                        
                        <items jcr:primaryType="nt:unstructured">
                            
                            <!-- 富文本编辑器 (使用 RTE) -->
                            <text jcr:primaryType="nt:unstructured"
                                  sling:resourceType="cq/gui/components/authoring/dialog/richtext"
                                  fieldLabel="正文内容"
                                  name="./text"
                                  useFixedInlineToolbar="{Boolean}true">
                                
                                <rtePlugins jcr:primaryType="nt:unstructured">
                                    <format jcr:primaryType="nt:unstructured"
                                            features="bold,italic,underline"/>
                                    <links jcr:primaryType="nt:unstructured"
                                           features="modifylink,unlink"/>
                                    <lists jcr:primaryType="nt:unstructured"
                                           features="*"/>
                                </rtePlugins>
                            </text>
                            
                            <!-- 图片上传 -->
                            <image jcr:primaryType="nt:unstructured"
                                   sling:resourceType="cq/gui/components/authoring/dialog/fileupload"
                                   fieldLabel="封面图片"
                                   name="./file"
                                   fileReferenceParameter="./fileReference"
                                   mimeTypes="[image/gif,image/jpeg,image/png]">
                            </image>
                            
                        </items>
                    </content>
                    
                    <!-- 分类和标签标签页 -->
                    <categorization jcr:primaryType="nt:unstructured"
                                    jcr:title="分类和标签"
                                    sling:resourceType="granite/ui/components/coral/foundation/container">
                        
                        <items jcr:primaryType="nt:unstructured">
                            
                            <!-- 分类选择 -->
                            <category jcr:primaryType="nt:unstructured"
                                      sling:resourceType="granite/ui/components/coral/foundation/form/select"
                                      fieldLabel="分类"
                                      name="./category">
                                
                                <items jcr:primaryType="nt:unstructured">
                                    <tech jcr:primaryType="nt:unstructured"
                                          text="技术"
                                          value="tech"/>
                                    <design jcr:primaryType="nt:unstructured"
                                            text="设计"
                                            value="design"/>
                                    <marketing jcr:primaryType="nt:unstructured"
                                               text="营销"
                                               value="marketing"/>
                                </items>
                            </category>
                            
                            <!-- 标签（多选） -->
                            <tags jcr:primaryType="nt:unstructured"
                                  sling:resourceType="granite/ui/components/coral/foundation/form/checkboxlist"
                                  fieldLabel="标签"
                                  name="./tags">
                                
                                <items jcr:primaryType="nt:unstructured">
                                    <tag1 jcr:primaryType="nt:unstructured"
                                          text="热门"
                                          value="hot"/>
                                    <tag2 jcr:primaryType="nt:unstructured"
                                          text="推荐"
                                          value="featured"/>
                                    <tag3 jcr:primaryType="nt:unstructured"
                                          text="最新"
                                          value="latest"/>
                                </items>
                            </tags>
                            
                        </items>
                    </categorization>
                    
                    <!-- 设置标签页 -->
                    <settings jcr:primaryType="nt:unstructured"
                              jcr:title="设置"
                              sling:resourceType="granite/ui/components/coral/foundation/container">
                        
                        <items jcr:primaryType="nt:unstructured">
                            
                            <!-- 推荐复选框 -->
                            <featured jcr:primaryType="nt:unstructured"
                                      sling:resourceType="granite/ui/components/coral/foundation/form/checkbox"
                                      fieldLabel="推荐"
                                      name="./featured"
                                      text="设为推荐内容">
                            </featured>
                            
                            <!-- 链接页面 -->
                            <link jcr:primaryType="nt:unstructured"
                                  sling:resourceType="granite/ui/components/coral/foundation/form/pathfield"
                                  fieldLabel="链接页面"
                                  name="./link"
                                  rootPath="/content">
                            </link>
                            
                        </items>
                    </settings>
                    
                </items>
            </tabs>
            
        </items>
    </content>
</jcr:root>
```

## 🎯 对话框最佳实践

### 1. 使用标签页组织复杂表单

对于包含多个字段的对话框，使用标签页可以改善用户体验：

```xml
<tabs>
    <items>
        <basic jcr:title="基本信息">...</basic>
        <content jcr:title="内容">...</content>
        <settings jcr:title="设置">...</settings>
    </items>
</tabs>
```

### 2. 必填字段验证

使用 `required="{Boolean}true"` 标记必填字段：

```xml
<field required="{Boolean}true"
       fieldLabel="标题"
       name="./title"/>
```

### 3. 字段描述和帮助文本

```xml
<field fieldLabel="标签"
       name="./tags">
    <granite:data
        jcr:primaryType="nt:unstructured"
        helpText="选择相关标签，可多选"/>
</field>
```

### 4. 默认值设置

```xml
<field fieldLabel="状态"
       name="./status">
    <items>
        <default jcr:primaryType="nt:unstructured"
                 text="草稿"
                 value="draft"
                 selected="{Boolean}true"/>
    </items>
</field>
```

### 5. 字段禁用和只读

```xml
<field disabled="{Boolean}true"
       fieldLabel="创建日期"
       name="./createdDate"/>
```

## 🔑 关键要点

1. **Touch UI 是标准**：使用 Granite UI / Coral UI 组件
2. **组件类型**：选择合适的组件类型（textfield, select, checkbox 等）
3. **组织方式**：使用标签页和字段集组织复杂表单
4. **验证**：设置必填字段和格式验证
5. **用户体验**：提供清晰的标签、描述和帮助文本

## ➡️ 下一步

学习更多 Granite UI 组件和高级对话框功能，如条件显示、自定义验证等。

