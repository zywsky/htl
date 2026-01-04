# AEM 基础：第一部分 - AEM 简介

## 📖 什么是 Adobe Experience Manager (AEM)?

Adobe Experience Manager (AEM) 是一个企业级内容管理系统 (CMS)，用于构建网站、移动应用和表单。它是 Adobe Marketing Cloud 的一部分，提供了强大的内容管理、数字资产管理、社区和表单功能。

## 🏗️ AEM 的核心特性

### 1. 内容管理系统 (CMS)
- 强大的页面编辑器 (Touch UI / Classic UI)
- 组件化内容创作
- 版本控制和内容历史

### 2. 数字资产管理 (DAM)
- 中央媒体库
- 智能标记和搜索
- 多格式支持

### 3. 多站点管理 (MSM)
- 管理多个网站
- 内容重用和同步
- 多语言支持

### 4. 社区功能
- 用户生成内容
- 论坛和博客
- 社交集成

## 🔧 AEM 技术栈

AEM 建立在以下核心技术之上：

```
┌─────────────────────────────────────┐
│      Adobe Experience Manager       │
├─────────────────────────────────────┤
│         Apache Sling Framework      │  ← Web 框架，处理 HTTP 请求
├─────────────────────────────────────┤
│    Java Content Repository (JCR)    │  ← 内容存储，基于节点树结构
├─────────────────────────────────────┤
│          Apache Jackrabbit          │  ← JCR 实现
├─────────────────────────────────────┤
│              OSGi Framework          │  ← 模块化架构，动态服务管理
├─────────────────────────────────────┤
│            Java Virtual Machine     │  ← 运行环境
└─────────────────────────────────────┘
```

### 关键组件说明

1. **JCR (Java Content Repository)**
   - 内容以节点 (Nodes) 和属性 (Properties) 的形式存储
   - 树形结构，类似文件系统
   - 支持版本控制、权限管理

2. **Apache Sling**
   - RESTful Web 框架
   - 基于资源 (Resource) 的路由
   - 将 HTTP 请求映射到内容节点

3. **OSGi**
   - 模块化 Java 应用框架
   - 动态加载和卸载模块
   - 服务依赖管理

## 📁 AEM 内容结构

AEM 中的内容以层次化的节点树结构存储：

```
/content
  └── myproject
      ├── en                    ← 语言根节点
      │   ├── home              ← 页面节点
      │   │   ├── jcr:content   ← 页面内容节点
      │   │   │   ├── title     ← 属性：页面标题
      │   │   │   └── par       ← 段落系统（包含组件）
      │   │   │       └── component1  ← 组件实例
      │   │   └── about
      │   └── products
      └── zh
          └── home
```

### 节点类型说明

- **页面节点** (`cq:Page`): 代表一个可渲染的页面
- **内容节点** (`jcr:content`): 页面的实际内容
- **组件节点**: 页面中的可重用内容块

## 🎨 AEM 架构层次

```
┌─────────────────────────────────────────────┐
│           Presentation Layer                │  ← HTL/Sightly 模板，JSP
│         (呈现层：模板和视图)                  │
├─────────────────────────────────────────────┤
│           Business Logic Layer              │  ← Sling Models，Java 类
│         (业务逻辑层：数据处理)                │
├─────────────────────────────────────────────┤
│              Data Access Layer              │  ← JCR API，Sling API
│         (数据访问层：内容操作)                │
├─────────────────────────────────────────────┤
│           Content Repository                │  ← JCR，文件系统，数据库
│         (内容存储：数据持久化)                │
└─────────────────────────────────────────────┘
```

## 🚀 AEM 开发环境设置

### 系统要求

- **Java**: JDK 11 或更高版本
- **Maven**: 3.3.9 或更高版本
- **IDE**: IntelliJ IDEA 或 Eclipse
- **AEM SDK**: 从 Adobe 官网下载

### Maven 项目结构

一个标准的 AEM 项目通常使用 Maven 多模块架构：

```
my-aem-project/
├── pom.xml                    ← 父 POM
├── core/                      ← 核心 Java 代码
│   └── src/main/java/
├── ui.apps/                   ← UI 资源 (HTML, CSS, JS)
│   └── src/main/content/
├── ui.content/                ← 初始内容结构
│   └── src/main/content/
└── all/                       ← 聚合模块，用于打包
    └── pom.xml
```

## 📝 第一个 AEM 项目示例

让我们从最简单的 Maven POM 配置开始：

### 父 POM 示例 (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-aem-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>My AEM Project</name>
    <description>AEM Project</description>

    <properties>
        <!-- AEM 版本 -->
        <aem.sdk.api>2023.11.15051.20231117T035515Z-231117</aem.sdk.api>
        
        <!-- 项目编码 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Maven 插件版本 -->
        <maven.compiler.plugin.version>3.8.1</maven.compiler.plugin.version>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
    </properties>

    <modules>
        <module>core</module>
        <module>ui.apps</module>
        <module>ui.content</module>
        <module>all</module>
    </modules>

    <build>
        <plugins>
            <!-- Maven 编译器插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven.compiler.plugin.version}</version>
                <configuration>
                    <source>${maven.compiler.source}</source>
                    <target>${maven.compiler.target}</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 核心模块 POM (core/pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-aem-project</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>my-aem-project.core</artifactId>
    <packaging>bundle</packaging>

    <name>My AEM Project - Core</name>
    <description>Core bundle</description>

    <dependencies>
        <!-- OSGi 核心注解 -->
        <dependency>
            <groupId>org.osgi</groupId>
            <artifactId>org.osgi.annotation.versioning</artifactId>
            <version>1.1.0</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- Sling API -->
        <dependency>
            <groupId>org.apache.sling</groupId>
            <artifactId>org.apache.sling.api</artifactId>
            <version>2.27.2</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- JCR API -->
        <dependency>
            <groupId>javax.jcr</groupId>
            <artifactId>jcr</artifactId>
            <version>2.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven Bundle Plugin - 用于创建 OSGi Bundle -->
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <version>5.1.8</version>
                <extensions>true</extensions>
                <configuration>
                    <instructions>
                        <Bundle-SymbolicName>${project.groupId}.${project.artifactId}</Bundle-SymbolicName>
                        <Bundle-Name>${project.name}</Bundle-Name>
                        <Export-Package>
                            com.example.core.*;version="${project.version}"
                        </Export-Package>
                        <Import-Package>
                            !javax.annotation,
                            *
                        </Import-Package>
                        <Embed-Dependency>*;scope=compile;inline=false</Embed-Dependency>
                        <Embed-Transitive>false</Embed-Transitive>
                    </instructions>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 🔑 核心概念总结

1. **AEM** 是一个企业级 CMS，基于 JCR、Sling 和 OSGi
2. **内容以节点树结构存储**，类似文件系统
3. **组件化架构**，内容由可重用的组件组成
4. **Maven 多模块项目**是标准的 AEM 项目结构
5. **OSGi Bundles** 是 AEM 应用的部署单元

## ➡️ 下一步

在下一节中，我们将深入学习 **JCR (Java Content Repository)**，理解 AEM 如何存储和访问内容。

