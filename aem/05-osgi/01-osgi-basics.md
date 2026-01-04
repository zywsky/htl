# OSGi 基础：第一部分 - OSGi 简介和基础服务

## 📖 什么是 OSGi?

OSGi (Open Service Gateway Initiative) 是一个模块化 Java 应用的框架规范。在 AEM 中，所有 Java 代码都打包成 OSGi Bundles（模块），这些 Bundles 可以动态安装、启动、停止和卸载。

## 🏗️ OSGi 核心概念

### 1. Bundle（模块）
- OSGi 应用的部署单元
- 包含代码、资源、元数据
- 可以动态加载和卸载

### 2. Service（服务）
- 可被其他 Bundle 使用的 Java 对象
- 通过接口定义
- 可以有多个实现

### 3. Service Registry（服务注册表）
- 管理所有服务的注册和发现
- 支持服务依赖注入

## 💻 创建 OSGi 服务

### 示例 1：基础服务接口和实现

```java
package com.example.core.services;

/**
 * 服务接口
 * 
 * 在 OSGi 中，服务通常通过接口定义
 * 这样可以有多个实现，便于测试和替换
 */
public interface HelloService {
    
    /**
     * 问候方法
     * 
     * @param name 名称
     * @return 问候语
     */
    String greet(String name);
}
```

```java
package com.example.core.services.impl;

import com.example.core.services.HelloService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * 服务实现
 * 
 * @Component: 标识这是一个 OSGi 组件
 * service: 注册的服务接口
 * immediate: 是否立即激活（默认 false）
 */
@Component(service = HelloService.class, immediate = true)
@Designate(ocd = HelloServiceImpl.Config.class)  // 使用配置
public class HelloServiceImpl implements HelloService {

    /**
     * 配置接口
     * 用于 OSGi 配置
     */
    @ObjectClassDefinition(
        name = "Hello Service Configuration",
        description = "Hello Service 的配置"
    )
    @interface Config {
        
        @AttributeDefinition(
            name = "Greeting Prefix",
            description = "问候语前缀"
        )
        String greetingPrefix() default "Hello";
    }

    private Config config;

    /**
     * 激活组件
     * 
     * @param config 配置对象
     */
    @org.osgi.service.component.annotations.Activate
    protected void activate(Config config) {
        this.config = config;
        System.out.println("HelloService 已激活");
    }

    /**
     * 停用组件
     */
    @org.osgi.service.component.annotations.Deactivate
    protected void deactivate() {
        System.out.println("HelloService 已停用");
    }

    @Override
    public String greet(String name) {
        String prefix = config != null ? config.greetingPrefix() : "Hello";
        return prefix + ", " + name + "!";
    }
}
```

### 示例 2：使用服务

```java
package com.example.core.models;

import com.example.core.services.HelloService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

/**
 * 在 Sling Model 中使用 OSGi 服务
 */
@Model(adaptables = Resource.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ServiceUsageModel {

    /**
     * @OSGiService: 注入 OSGi 服务
     */
    @OSGiService
    private HelloService helloService;

    /**
     * 使用服务
     */
    public String getGreeting() {
        if (helloService != null) {
            return helloService.greet("World");
        }
        return "Service not available";
    }
}
```

### 示例 3：带有依赖的服务

```java
package com.example.core.services;

import java.util.List;

/**
 * 数据服务接口
 */
public interface DataService {
    List<String> getData();
    void saveData(String data);
}
```

```java
package com.example.core.services.impl;

import com.example.core.services.DataService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * 数据服务实现
 */
@Component(service = DataService.class, immediate = true)
public class DataServiceImpl implements DataService {

    /**
     * @Reference: 引用其他服务
     * cardinality: 基数（必需/可选，单/多）
     * policy: 策略（动态/静态）
     */
    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.DYNAMIC
    )
    private volatile StorageService storageService;  // 可选依赖

    @Override
    public List<String> getData() {
        if (storageService != null) {
            return storageService.read();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public void saveData(String data) {
        if (storageService != null) {
            storageService.write(data);
        }
    }
}
```

### 示例 4：服务事件监听

```java
package com.example.core.listeners;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.EventConstants;

/**
 * 事件监听器
 * 
 * 监听 OSGi 事件
 */
@Component(
    service = EventHandler.class,
    immediate = true,
    property = {
        EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/ADDED",
        EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/CHANGED"
    }
)
public class ResourceChangeListener implements EventHandler {

    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        String path = (String) event.getProperty("path");
        
        System.out.println("资源事件: " + topic);
        System.out.println("资源路径: " + path);
        
        // 处理事件逻辑
        if (topic.contains("ADDED")) {
            handleResourceAdded(path);
        } else if (topic.contains("CHANGED")) {
            handleResourceChanged(path);
        }
    }

    private void handleResourceAdded(String path) {
        // 处理资源添加逻辑
    }

    private void handleResourceChanged(String path) {
        // 处理资源修改逻辑
    }
}
```

### 示例 5：调度任务（Scheduled Task）

```java
package com.example.core.schedulers;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * 调度任务示例
 * 
 * 使用 Apache Sling Commons Scheduler 进行任务调度
 */
@Component(service = Runnable.class)
@Designate(ocd = ScheduledTask.Config.class)
public class ScheduledTask implements Runnable {

    @ObjectClassDefinition(name = "Scheduled Task Configuration")
    @interface Config {
        @AttributeDefinition(name = "Cron Expression")
        String scheduler_expression() default "0 0 * * * ?";  // 每小时执行一次

        @AttributeDefinition(name = "Task Name")
        String scheduler_concurrent() default "false";
    }

    @Override
    public void run() {
        System.out.println("调度任务执行: " + new java.util.Date());
        
        // 执行任务逻辑
        performTask();
    }

    private void performTask() {
        // 任务逻辑
    }
}
```

### 示例 6：配置服务（Configuration Service）

```java
package com.example.core.services.impl;

import com.example.core.services.ConfigService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * 配置服务示例
 * 
 * 使用 OSGi Configuration Admin 管理配置
 */
@Component(service = ConfigService.class, immediate = true)
@Designate(ocd = ConfigServiceImpl.Config.class)
public class ConfigServiceImpl implements ConfigService {

    @ObjectClassDefinition(
        name = "My Project Configuration",
        description = "项目的全局配置"
    )
    @interface Config {
        
        @AttributeDefinition(
            name = "API Endpoint",
            description = "API 端点地址"
        )
        String apiEndpoint() default "https://api.example.com";

        @AttributeDefinition(
            name = "API Key",
            description = "API 密钥",
            type = AttributeDefinition.Type.PASSWORD
        )
        String apiKey();

        @AttributeDefinition(
            name = "Timeout (seconds)",
            description = "请求超时时间（秒）"
        )
        int timeout() default 30;

        @AttributeDefinition(
            name = "Enabled Features",
            description = "启用的功能列表"
        )
        String[] enabledFeatures() default {};
    }

    private Config config;

    @Activate
    protected void activate(Config config) {
        this.config = config;
        System.out.println("ConfigService 已激活");
        System.out.println("API Endpoint: " + config.apiEndpoint());
    }

    @Override
    public String getApiEndpoint() {
        return config != null ? config.apiEndpoint() : "";
    }

    @Override
    public String getApiKey() {
        return config != null ? config.apiKey() : "";
    }

    @Override
    public int getTimeout() {
        return config != null ? config.timeout() : 30;
    }

    @Override
    public String[] getEnabledFeatures() {
        return config != null ? config.enabledFeatures() : new String[0];
    }
}
```

## 📋 OSGi 注解总结

| 注解 | 用途 | 说明 |
|------|------|------|
| `@Component` | 标识 OSGi 组件 | service, immediate, configurationPolicy |
| `@Reference` | 引用其他服务 | cardinality, policy |
| `@Activate` | 组件激活时调用 | 初始化逻辑 |
| `@Deactivate` | 组件停用时调用 | 清理逻辑 |
| `@Modified` | 配置修改时调用 | 更新逻辑 |
| `@Service` | 注册为服务 | 旧式注解（现在用 @Component） |

## 🔑 关键要点

1. **服务接口**：定义清晰的接口
2. **组件注解**：使用 @Component 注册服务
3. **生命周期**：使用 @Activate/@Deactivate 管理生命周期
4. **依赖注入**：使用 @Reference 引用其他服务
5. **配置管理**：使用 @Designate 和 @ObjectClassDefinition 管理配置

## ➡️ 下一步

在下一节中，我们将学习 **OSGi 服务的高级用法和最佳实践**。

