# HTL 设计模式

## 设计模式概述

设计模式是可重用的解决方案，用于解决常见的设计问题。本指南介绍 HTL 组件开发中的常用设计模式。

## 1. 容器/展示模式 (Container/Presentational Pattern)

### 概念

将组件分为容器组件（处理逻辑）和展示组件（处理显示）。

### 实现

**容器组件 (Container):**
```html
<!-- container.html -->
<sly data-sly-use.model="${'com.example.ContainerModel' @ data=properties.data}"></sly>
<div data-sly-resource="${'presentation' @ resourceType='myapp/components/presentation', data=model.processedData}"></div>
```

**展示组件 (Presentation):**
```html
<!-- presentation.html -->
<div class="presentation-component">
    <h2>${properties.data.title}</h2>
    <p>${properties.data.content}</p>
</div>
```

## 2. 组合模式 (Composition Pattern)

### 概念

通过组合小组件创建复杂组件。

### 实现

```html
<!-- complex-component.html -->
<div class="complex-component">
    <div data-sly-resource="${'header' @ resourceType='myapp/components/header'}"></div>
    <div data-sly-resource="${'body' @ resourceType='foundation/components/parsys'}"></div>
    <div data-sly-resource="${'footer' @ resourceType='myapp/components/footer'}"></div>
</div>
```

## 3. 策略模式 (Strategy Pattern)

### 概念

根据配置选择不同的渲染策略。

### 实现

```html
<sly data-sly-use.renderer="${'com.example.RendererStrategy' @ strategy=properties.renderStrategy}"></sly>
<div data-sly-call="${renderer.getTemplate() @ data=properties.data}"></div>
```

## 4. 工厂模式 (Factory Pattern)

### 概念

根据类型创建不同的组件实例。

### 实现

```html
<sly data-sly-use.factory="${'com.example.ComponentFactory' @ type=properties.componentType}"></sly>
<div data-sly-resource="${'component' @ resourceType=factory.getResourceType(), config=factory.getConfig()}"></div>
```

## 5. 观察者模式 (Observer Pattern)

### 概念

组件响应数据变化自动更新。

### 实现

```html
<div class="component" 
     data-sly-attribute.data-component-id="${component.id}"
     data-sly-attribute.data-auto-update="${properties.autoUpdate}">
    <div class="component-content">${properties.content}</div>
</div>

<!-- JavaScript 监听数据变化并更新 -->
```

## 6. 单例模式 (Singleton Pattern)

### 概念

确保 Use API 对象只创建一次。

### 实现

```java
@Model(adaptables = Resource.class)
public class SingletonModel {
    private static SingletonModel instance;
    
    public static SingletonModel getInstance(Resource resource) {
        if (instance == null) {
            instance = resource.adaptTo(SingletonModel.class);
        }
        return instance;
    }
}
```

## 7. 适配器模式 (Adapter Pattern)

### 概念

将不同数据格式适配为统一格式。

### 实现

```html
<sly data-sly-use.adapter="${'com.example.DataAdapter' @ sourceData=properties.data}"></sly>
<div class="component">
    <h2>${adapter.adaptedData.title}</h2>
    <p>${adapter.adaptedData.content}</p>
</div>
```

## 8. 装饰器模式 (Decorator Pattern)

### 概念

动态添加功能到组件。

### 实现

```html
<div class="component ${properties.decoratorClass}">
    <div class="decorator-wrapper">
        <div class="component-content">${properties.content}</div>
        <div data-sly-test="${properties.showBadge}" class="badge">${properties.badge}</div>
    </div>
</div>
```

## 9. 模板方法模式 (Template Method Pattern)

### 实现

```html
<template data-sly-template.baseLayout="${@ header, body, footer}">
    <div class="layout">
        <header>${header}</header>
        <main>${body}</main>
        <footer>${footer}</footer>
    </div>
</template>

<!-- 使用模板 -->
<div data-sly-call="${baseLayout @ 
    header=headerTemplate,
    body=bodyTemplate,
    footer=footerTemplate}">
</div>
```

## 10. 代理模式 (Proxy Pattern)

### 概念

通过代理对象控制对原始对象的访问。

### 实现

```html
<sly data-sly-use.proxy="${'com.example.DataProxy' @ dataSource=properties.dataSource}"></sly>
<div class="component">
    <!-- 通过代理访问数据 -->
    <div>${proxy.getData()}</div>
</div>
```

## 11. 建造者模式 (Builder Pattern)

### 概念

逐步构建复杂对象。

### 实现

```html
<sly data-sly-use.builder="${'com.example.ComponentBuilder' @ 
    title=properties.title,
    content=properties.content,
    image=properties.image}"></sly>
<div class="component">
    ${builder.build()}
</div>
```

## 12. 门面模式 (Facade Pattern)

### 概念

提供简化的接口访问复杂子系统。

### 实现

```html
<sly data-sly-use.facade="${'com.example.ComponentFacade' @ config=properties.config}"></sly>
<div class="component">
    <!-- 通过门面访问多个服务 -->
    <div>${facade.getFormattedData()}</div>
</div>
```

## 实际应用示例

### 示例 1: 卡片列表组件（组合模式）

```html
<div class="card-list">
    <div data-sly-list="${properties.cards}">
        <div data-sly-resource="${'card-' + itemList.index @ 
            resourceType='myapp/components/card',
            title=item.title,
            content=item.content,
            image=item.image}">
        </div>
    </div>
</div>
```

### 示例 2: 条件渲染策略（策略模式）

```html
<sly data-sly-use.strategy="${'com.example.RenderStrategy' @ 
    type=properties.renderType}"></sly>
<div data-sly-call="${strategy.getTemplate() @ data=properties.data}"></div>
```

### 示例 3: 数据适配器（适配器模式）

```html
<sly data-sly-use.adapter="${'com.example.ExternalDataAdapter' @ 
    externalData=properties.externalData}"></sly>
<div class="component">
    <h2>${adapter.normalizedData.title}</h2>
    <p>${adapter.normalizedData.description}</p>
</div>
```

## 模式选择指南

1. **容器/展示模式**: 当需要分离逻辑和显示时
2. **组合模式**: 当需要组合多个组件时
3. **策略模式**: 当需要根据条件选择不同实现时
4. **工厂模式**: 当需要根据类型创建对象时
5. **适配器模式**: 当需要转换数据格式时
6. **装饰器模式**: 当需要动态添加功能时

## 最佳实践

1. **选择合适模式**: 根据实际需求选择模式
2. **保持简单**: 不要过度设计
3. **文档化**: 记录使用的模式和原因
4. **重构**: 定期重构以应用更好的模式
5. **测试**: 确保模式实现正确

## 总结

设计模式帮助：
- 提高代码可维护性
- 增强代码可重用性
- 改善代码结构
- 便于团队协作
- 降低复杂度

选择合适的模式可以显著提高代码质量和开发效率。

