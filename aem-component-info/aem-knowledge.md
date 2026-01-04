## Adobe AEM 知识体系文档

### 0. 本文目标

- **目标**：让你对 Adobe AEM 有系统、可落地的理解，适合作为入门 & 复习文档。  
- **结构**：先列出 AEM 的主要知识方面，再对每个方面做详细讲解。  

---

## 1. AEM 知识有哪些方面

1. **基础认识与整体架构**  
2. **内容存储：JCR / Oak 存储库**  
3. **OSGi 模块化与 AEM 后端开发**  
4. **Sling 请求分发与资源解析机制**  
5. **模板（Template）、组件（Component）与页面模型（Sites 开发核心）**  
6. **HTL（Sightly）前端模板语言与前端集成**  
7. **内容管理与创作体验（Authoring）**  
8. **多站点、多语言与内容复用（MSM / i18n）**  
9. **工作流（Workflow）与审批发布流程**  
10. **权限、安全与用户/组管理**  
11. **Assets（数字资产管理 DAM）**  
12. **AEM Dispatcher 与缓存/安全**  
13. **部署架构：Author/Publish 拓扑与 AEM Cloud Service**  
14. **日志、监控与性能优化**  
15. **与外部系统集成（API、Headless、搜索、第三方系统）**  

---

## 2. 基础认识与整体架构

### 2.1 AEM 是什么？

- **Adobe Experience Manager（AEM）** 是 Adobe 的企业级数字体验平台，核心是一个强大的 **内容管理系统（CMS）**。  
- 典型用途：
  - **搭建企业官网、门户、营销落地页（Landing Page）**
  - 管理 **多品牌、多国家、多语言** 网站
  - 管理图片、视频、文档等 **数字资产（DAM）**
  - 提供 **内容创建、审核、发布** 的完整工作流
  - 支持 **Headless（只输出 JSON 等数据）**，供 App、小程序、SPA 前端使用

### 2.2 关键角色与实例

- **Author 实例**：  
  - 供内容作者、编辑、管理员登录后台进行内容创作和配置。  
- **Publish 实例**：  
  - 面向公众访问的前台站点，只有已“发布”的内容会出现在这里。  
- **Dispatcher（在 Web 服务器上）**：  
  - 缓存 + 安全网关，屏蔽 Publish 实例细节。

---

## 3. 内容存储：JCR / Oak 存储库

### 3.1 JCR 概念

- **JCR（Java Content Repository）** 是一个内容存储标准。AEM 把所有内容（页面、组件配置、资产、用户配置等）都放在 JCR 中。  
- 数据结构类似 **树状文件系统**：
  - **节点（Node）**：相当于一个“文件”或“目录”
  - **属性（Property）**：节点的字段，比如 `jcr:title`, `sling:resourceType`, `cq:template`

### 3.2 Oak 存储实现

- **Apache Jackrabbit Oak** 是 AEM 默认的 JCR 实现。  
- 支持多种底层存储：
  - **Segment Tar**（本地文件）  
  - **Document Store（如 MongoDB）** 等  

### 3.3 你要理解的关键路径概念

- 内容和代码在 JCR 中通过路径组织，比如：
  - 页面：`/content/my-site/en/home`  
  - 组件：`/apps/myproject/components/hero`  
  - 模板与配置：`/conf/myproject/settings/wcm/templates/page`  
- **路径 + 节点类型 + 属性** 是你在 AEM 中查找和配置内容的基础。

---

## 4. OSGi 模块化与 AEM 后端开发

### 4.1 OSGi 是什么？

- **OSGi** 是一种 Java 模块化框架，支持：
  - 把功能拆分成多个 **bundle（jar 包）**
  - 动态安装、更新、停止 bundle  
  - 模块之间通过 **服务（Service）** 解耦通信  
- AEM 运行在 OSGi 容器（Apache Felix）之上。

### 4.2 在 AEM 中怎么用 OSGi

- 自己写的后端逻辑通常是 **OSGi 组件/服务**：
  - 使用注解（如 `@Component`, `@Service`, `@Reference` 等）声明  
  - 实现一些接口或暴露业务方法，被其他代码调用  
- 配置方式：
  - 在 `/system/console/configMgr` 在线修改  
  - 或在代码中提供 `.config` / `.cfg.json` 文件，作为 OSGi 配置。

### 4.3 实战理解

- “AEM 后端开发”本质上就是：  
  - 在 **OSGi 中写服务**（比如：内容查询服务、调用第三方 API 服务）  
  - 再通过组件（Sling Models/Servlet 等）把数据提供给页面或前端使用。

---

## 5. Sling 请求分发与资源解析机制

### 5.1 Sling 的核心思想

- AEM 基于 **Apache Sling**，Sling 的核心理念是：  
  - **URL → JCR 中的一个资源（Resource） → 根据资源类型找到脚本 → 输出结果**  
- 也就是说：  
  - 你访问的每个 URL，都会被解析为 JCR 的某个节点，然后根据该节点的 `sling:resourceType` 找对应的组件脚本来渲染。

### 5.2 请求示例

访问：`https://example.com/content/my-site/en/home.html` 时：

1. Sling 在 JCR 中找到资源：`/content/my-site/en/home`  
2. 查看该节点的属性 `sling:resourceType`，假设是 `myproject/components/page/home`  
3. 在 `/apps/myproject/components/page/home` 下寻找相应的脚本文件（通常是 HTL `.html`）  
4. 执行脚本，输出 HTML 给浏览器。

### 5.3 关键概念

- **Resource（资源）**：JCR 节点在 Sling 中的抽象。  
- **Resource Type（资源类型）**：告诉 Sling 用哪个组件脚本来渲染。  
- **Selectors / Extension**：
  - 如：`home.print.a4.html` 中的 `print` / `a4` / `html` 可以影响渲染逻辑（不同视图、不同格式等）。

---

## 6. 模板、组件与页面模型（Sites 开发核心）

### 6.1 页面由什么组成？

- AEM 中的页面不是手写的单个 HTML 文件，而是由：
  - **模板（Template）**：决定页面整体框架和哪些区域可以放组件  
  - **组件（Component）**：可复用的功能块（如 Banner、文本、轮播等）  
  - **内容（Content）**：作者在组件对话框中填写的文本、图片等数据  
- 最终渲染时：
  - 模板 + 组件脚本 + 组件内容 → 生成最终页面。

### 6.2 模板（Templates）

- 分为：
  - **静态模板（Classic）**
  - **可编辑模板（Editable Templates，推荐）**
- 模板可以定义：
  - 页面结构：头部、底部、主内容区、侧边栏等  
  - **Policies（政策）**：  
    - 允许哪些组件可以被作者拖入  
    - 组件的默认样式/配置（如最大宽度、允许的样式类）

### 6.3 组件（Components）

- 组件的职责：
  - 提供某种内容展示或交互功能（如：图文模块、视频播放器、表单）  
- 一个典型组件包含：
  - 在 `/apps/myproject/components/my-component` 下的组件节点  
  - `my-component.html`（HTL 模板）  
  - 对应的 Java 类（常用 **Sling Models**）用于处理数据逻辑  
  - Dialog（组件对话框），定义作者能填写的字段（标题、图片、链接等）

### 6.4 Sling Models

- 使用注解如 `@Model(adaptables = Resource.class)` 创建的 Java 类：  
  - 从当前资源/请求中读取属性  
  - 调用 OSGi 服务或做简单业务逻辑  
  - 提供字段给 HTL 使用（例如 `getTitle()`, `getItems()` 等）

---

## 7. HTL（Sightly）与前端集成

### 7.1 HTL 是什么？

- **HTL（HTML Template Language）**，以前叫 Sightly，是 AEM 推荐的服务器端模板语言，用来替代 JSP。  
- 设计目标：
  - **让前端开发更友好**，降低 Java 语法侵入
  - 保持逻辑简单，避免复杂业务逻辑写在页面中

### 7.2 HTL 基本语法

- 输出变量：
  - `${model.title}`  
- 指令示例：
  - `data-sly-use`：引入 Sling Model 或 Use 类
  - `data-sly-list`：循环
  - `data-sly-test`：条件判断
- 逻辑规则：
  - 复杂逻辑放在 Java（Sling Model、Service）中  
  - HTL 只做简单的条件、循环和展示。

### 7.3 JS / CSS 与 ClientLibs

- 前端静态资源（JS、CSS、图片等）通过 **ClientLibs** 管理：
  - 位于 `/apps/<project>/clientlibs/...`（新项目推荐）  
  - 使用 `cq:ClientLibraryFolder` 节点声明 categories、依赖  
- 在页面或组件 HTL 中，通过引用某个 category 来引入对应 JS/CSS。  
- 可以结合 SPA Editor，实现 React/Vue 等前端框架与 AEM 的集成。

---

## 8. 内容管理与创作体验（Authoring）

### 8.1 Page Editor：作者如何编辑页面？

- AEM 提供所见即所得的页面编辑器：
  - 中间：页面预览
  - 左侧：组件库（可以拖拽到页面上）
  - 右侧：选中组件或页面的属性编辑区  
- 作者可以：
  - 拖拽添加组件  
  - 编辑组件对话框（修改标题、文字、图片等）
  - 复制、移动、删除组件  
  - 预览不同设备视口（PC/Pad/Mobile）

### 8.2 版本控制与时间线

- 每个页面可以：
  - 创建版本（Snapshot）
  - 对比版本差异
  - 回滚到某个历史版本  
- 时间线（Timeline）视图展示：
  - 创建/修改/发布记录
  - 相关工作流事件

### 8.3 WCM 关键操作

- 创建新页面（选择模板）  
- 编辑、保存、预览  
- 提交审核、发布到 Publish  
- 调整站点树结构（移动、复制、删除页面）

---

## 9. 多站点、多语言与内容复用（MSM / i18n）

### 9.1 Multi Site Manager（MSM）

- 当有 **多个国家/地区/品牌** 的站点时，很多内容结构是类似的：  
  - 可以通过 **MSM（Multi Site Manager）** 来管理：
    - 创建一个主站或蓝本（Blueprint）
    - 基于蓝本创建多个 **Live Copy**（子站点）
    - 蓝本更新后，可以把变更“推送”（Rollout）到子站点  
- 这样可以：
  - 统一结构
  - 在需要的地方允许局部内容“断开继承”并本地化。

### 9.2 多语言（i18n）

- 路径级多语言：
  - 例如：`/content/site/en`、`/content/site/zh`  
- 内容翻译：
  - 使用 **Translation Project** 把一批页面打包，交给人工或翻译供应商处理  
- 固定文案（如按钮“提交/Submit”）：
  - 使用 AEM 的 i18n 字典和 API，在组件中根据语言自动展示对应文案。

### 9.3 关键区分

- **MSM**：解决的是 **多个站点结构/内容同步** 的问题。  
- **i18n**：解决的是 **不同语言文本展示** 的问题。  

---

## 10. 工作流（Workflow）与审批发布流程

### 10.1 工作流的用途

- 在企业环境中，内容通常需要审核和控制上线流程：  
  - 作者创建内容 → 审核人确认 → 发布到线上  
- AEM 的 Workflow 引擎支持：
  - 图形化设计流程（步骤节点、分支、条件）  
  - 人工任务（需要人操作的步骤）
  - 自动任务（执行脚本/服务的步骤）

### 10.2 常见工作流场景

- 页面/资产的 **提交审核 + 发布**  
- 图片上传后的自动处理（生成缩略图、不同格式等）  
- 定时发布/下线内容  
- 内容归档、通知邮件发送等。

### 10.3 自定义 Workflow

- 通过编写 Java 类实现 `WorkflowProcess`：
  - 在执行到该步骤时执行自定义逻辑（如：调用外部接口、校验数据）  
- 在 Workflow Model 中配置这个步骤，并在需要的内容路径上触发。

---

## 11. 权限、安全与用户/组管理

### 11.1 访问控制模型

- AEM 使用 **JCR ACL（Access Control List）** 控制权限：  
  - **用户（User）** 与 **组（Group）**  
  - 权限类型：Read、Modify、Create、Delete、Replicate（发布）、ACL 等  
  - 权限可以配置在特定路径上，比如：
    - `/content/site/brand-a` 只允许 Brand A 团队编辑  
    - `/conf` 只有管理员可修改  

### 11.2 管理实践

- 使用组（Group）来管理角色：
  - 作者组、审核组、管理员组等  
- 避免在单个用户上直接配置大量权限，而是通过加入不同组来继承权限。

### 11.3 身份认证集成

- AEM 可以和企业的：
  - LDAP / Active Directory  
  - SAML / OAuth / SSO 方案  
  集成，实现统一登录和单点登录。

---

## 12. Assets（数字资产管理 DAM）

### 12.1 DAM 概念

- **AEM Assets** 是一个 **数字资产管理系统（DAM）**：
  - 集中管理图片、视频、PDF、Office 文档等  
  - 记录版本、元数据、使用范围等

### 12.2 核心功能

- 上传资产时自动处理：
  - 生成预览、缩略图、多种尺寸（renditions）  
- 元数据管理：
  - 标题、描述、标签（Tag）、版权信息、到期时间等  
- 版本管理：
  - 替换图片时保留历史版本，可回退。

### 12.3 与 Sites 的结合

- 页面组件中通常通过选择 DAM 中的资产来展示图片/视频：  
  - 一个资产可以在多个页面中复用  
  - 更新资产文件后可以影响所有使用它的页面（受缓存和发布影响）。

---

## 13. AEM Dispatcher 与缓存/安全

### 13.1 Dispatcher 的角色

- **Dispatcher** 是 AEM 官方提供的 Web 服务器模块，用于：
  - **缓存** Publish 实例响应的页面和资源  
  - **安全防护**：限制不合法的请求到达 AEM  
  - 简单的 **负载均衡** 功能  
- 通常部署在 Apache HTTP Server 或 IIS 上。

### 13.2 缓存策略

- Dispatcher 会把 HTML、JSON、静态资源缓存到磁盘：  
  - 减少对 Publish 实例的压力  
  - 提高站点响应速度  
- 配置内容包括：
  - 哪些路径允许被缓存  
  - 对某些参数是否区分缓存  
  - 通过内容复制（Replication）或 API 进行缓存失效（清缓存）

### 13.3 安全配置

- 过滤危险 URL（如 `/system/console`, `/crx/de` 等）  
- 限制 HTTP 方法（仅允许 GET/HEAD 等公开请求）  
- 对输入参数进行白名单控制。

---

## 14. 部署架构：Author/Publish 拓扑与 AEM Cloud Service

### 14.1 经典 On-Premise / AMS 拓扑

- 典型结构：
  - 1 个或多个 **Author 实例**（可集群）  
  - 多个 **Publish 实例**（前台）  
  - 前面一层或多层 **Dispatcher**（负载均衡 + 缓存 + 安全）  
- 内容流程：
  1. 作者在 Author 实例中修改内容  
  2. 提交流程并通过审核  
  3. 内容通过 **Replication（复制）** 发布到 Publish  
  4. 用户通过 Dispatcher 访问，Dispatcher 从 Publish 获取/缓存内容。

### 14.2 AEM as a Cloud Service

- 新一代的云原生部署模式：**AEM Cloud Service**：
  - 自动扩容、高可用  
  - Adobe 负责底层运维与升级  
  - 通过 **Cloud Manager** 管理 CI/CD、环境、部署流程  
- 对开发规范有更多约束（推荐使用最新的最佳实践，减少自定义低层实现）。

---

## 15. 日志、监控与性能优化

### 15.1 常见日志

- 基础日志：
  - `error.log`：系统/应用错误、警告等  
  - `request.log`：请求访问记录  
- 可以通过 `/system/console/slinglog` 配置：
  - 新的日志文件  
  - 日志级别（DEBUG/INFO/WARN/ERROR）

### 15.2 性能关注点

- 组件渲染耗时：
  - 是否每次都查询大量数据、是否可缓存  
- JCR 查询：
  - 是否使用合适的索引  
  - 避免深层遍历和昂贵的查询模式  
- Dispatcher：
  - 缓存是否命中  
  - 是否合理配置了缓存键、过期/失效策略  

### 15.3 监控

- On-Prem / AMS：
  - 常结合 ELK、Splunk、Datadog 等监控/日志平台使用  
- Cloud Service：
  - 使用 Adobe 提供的监控、日志访问能力，并可集成到企业监控体系。

---

## 16. 与外部系统集成（API、Headless、搜索、第三方）

### 16.1 Headless / Content Services / GraphQL

- AEM 可作为 **Headless CMS** 使用：
  - 通过 **AEM Content Services** 或 **GraphQL API** 输出 JSON  
  - 前端应用（App、小程序、SPA、IoT）调用这些 API 获取内容  
- 设计内容模型时，需要：
  - 把内容拆分为清晰的字段和结构
  - 考虑多语言、多渠道复用。

### 16.2 外部系统集成

- 常见集成对象：
  - CRM（Salesforce 等）
  - 营销自动化（Marketo 等）
  - 电商平台、支付系统
  - 内部业务系统（订单、会员等）  
- 实现方式：
  - 通过 **OSGi Service + HTTP 客户端** 调用外部 REST/SOAP API  
  - 使用 Adobe 或第三方提供的连接器。

### 16.3 搜索与索引

- 内部搜索：
  - 使用 AEM 提供的 QueryBuilder 或 JCR-SQL2 查询内容  
- 外部搜索：
  - 将内容同步或推送到 ElasticSearch/Solr 等搜索引擎  
  - 用于全站搜索、推荐等复杂场景。

---

## 17. 学习路径与总结

### 17.1 推荐学习顺序

1. **整体架构 & Author/Publish 概念**  
2. **JCR + Sling 请求模型（URL → Resource → Component）**  
3. **模板 / 组件 / HTL / Sling Models**：  
   - 学会从 0 写一个简单组件、挂到模板中、在页面上使用  
4. **Authoring 体验 & 工作流 & 发布流程**  
5. **Dispatcher 缓存 & 基本运维概念**  
6. 逐步深入：Assets、MSM、多语言、OSGi 复杂服务、Headless、Cloud Service 等。

### 17.2 后续可以做什么？

- 从这个文档出发，你可以：
  - 选择一个方向做实战练习，比如：
    - “**从零实现一个 Banner 组件**，包含 HTL + Sling Model + Dialog + ClientLibs”
    - “**从零搭一个简单站点结构**，理解模板、页面、组件的完整流程”  

### 17.3 实战时要牢记的几个原则

- **组件要小而清晰**：一个组件只解决一个清晰的问题，可配置但不要做成“万能大组件”。  
- **逻辑下沉到 Java**：复杂逻辑尽量放在 Sling Models/Service 中，HTL 只做展示。  
- **路径与类型规范**：`/apps`、`/conf`、`/content` 下保持清晰命名和层级，方便后期维护和迁移。  
- **Author 体验优先**：组件对话框、预览效果要简单直观，让内容作者用得顺手。  

---

## 18. 质量保障与测试（做 AEM 项目必须考虑）

- **单元测试**：  
  - 使用 `JUnit + AEM Mocks`（如 `io.wcm.testing.aem-mock`）对 Sling Models、OSGi Service 做逻辑单测。  
  - 尤其是有复杂数据转换、外部接口调用封装的地方。  
- **集成测试**：  
  - 使用 AEM 的测试框架，对 Servlet、Model Exporter、关键页面进行请求级测试（验证响应结构和状态码）。  
- **前端与作者验收**：  
  - 检查组件在不同浏览器/视口下显示是否正确；  
  - 对话框字段是否清晰、默认值是否合理、错误提示是否友好。  
- **流水线校验**：  
  - 在 CI 或 Cloud Manager 中跑静态检查（代码质量、安全依赖）、单测和简单 UI 回归测试。  

---

## 19. 性能与容量规划（上线前后都要关注）

- **Dispatcher 缓存优先**：  
  - 明确哪些页面/接口可以缓存，缓存多久；  
  - 配好缓存失效策略（发布/撤稿时失效对应路径）。  
- **JCR 查询与索引**：  
  - 优先使用有索引支持的查询模式；  
  - 避免在大树下做深层遍历或全量扫描。  
- **组件渲染性能**：  
  - 避免在请求中做重 IO 或多个外部接口串行调用，可考虑缓存或异步方案；  
  - 长列表组件要分页或懒加载。  
- **资产与前端性能**：  
  - 合理选择 renditions（不要在页面强行缩放超大图）；  
  - ClientLibs 压缩合并、开启浏览器缓存和 gzip/br。  

---

## 20. 安全与合规（线上环境的刚需）

- **Dispatcher 安全规则**：  
  - 禁止直接访问 `/system/console`, `/crx/de`, `/libs` 等敏感路径；  
  - 限制 HTTP 方法和允许的查询参数，防止恶意构造请求打到 AEM。  
- **权限与分权**：  
  - 按角色建组（作者、审核人、管理员），按路径授予最小必要权限；  
  - 定期审计高权限账号、复制（Replicate）权限。  
- **输入校验与防护**：  
  - 配置好 CSRF Filter、CORS、Referrer Filter；  
  - 富文本、上传内容做 XSS 白名单过滤。  
- **敏感配置管理**：  
  - 不在代码里硬编码密码/密钥，用 OSGi 配置 + 环境变量/密钥库来存放敏感信息。  

---

## 21. 迁移与升级时的关键点

- **On-Prem/AMS → Cloud Service**：  
  - 遵守“实例无状态、文件系统不可变”的原则；  
  - 把旧的 `/etc` 配置迁到 `/conf`，用可编辑模板和最新推荐结构。  
- **JSP → HTL / Classic UI → Touch UI**：  
  - 逐步替换旧 JSP 组件为 HTL + Sling Models；  
  - 把 Classic UI 对话框迁到 Coral/Granite Touch UI。  
- **内容迁移**：  
  - 使用 Package、RepoInit 等工具迁移节点和权限；迁移后检查节点类型、引用完整性和 ACL。  
- **升级前后回归**：  
  - 针对发布/工作流、搜索、主要页面、Dispatcher 缓存做重点回归，准备好回滚方案。  

---

## 22. 常用工具与调试技巧

- **本地开发与样板工程**：  
  - 使用 AEM SDK Quickstart 启本地实例；  
  - 用 AEM Project Archetype 生成标准化项目骨架。  
- **控制台与状态页面**：  
  - `/system/console/configMgr` 查看和修改 OSGi 配置；  
  - `/system/console/status` 系列页面查看线程、队列、脚本等状态；  
  - `/system/console/slinglog` 临时调整日志级别。  
- **内容与请求排查**：  
  - 开发环境用 CRX/DE Lite 看节点和属性；  
  - 用 QueryBuilder Debug 测试查询；  
  - 用 Resource Resolver Mapping 工具排查 URL 映射问题。  
- **日志分析**：  
  - 本地 tail/grep 查看错误；  
  - 线上导入到 ELK/Splunk，对慢请求和错误做聚合分析。  



