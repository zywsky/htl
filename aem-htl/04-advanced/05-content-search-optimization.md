# AEM Content 快速查找优化指南

## 概述

在 AEM 的 `/content` 目录下查找包含特定关键词的页面节点是一个常见需求。由于 `/content` 下通常包含大量节点，直接遍历会非常低效。本文档介绍如何高效地查找和搜索内容节点。

---

## 1. 问题场景

**需求**: 在 `/content` 下查找所有属性中包含特定关键词的页面节点。

**挑战**:
- `/content` 下节点数量庞大（可能数万到数十万）
- 直接遍历所有节点性能极差
- 需要支持多种搜索条件（属性名、属性值、节点类型等）

**示例场景**:
- 查找所有标题包含 "产品" 的页面
- 查找所有 `jcr:title` 属性包含 "新闻" 的节点
- 查找所有自定义属性 `productCategory` 包含 "电子" 的页面

---

## 2. JCR 查询方法

### 2.1 JCR SQL2 查询详解

JCR SQL2 是基于 SQL 标准的查询语言，功能强大，是 AEM/Oak 中推荐的查询方式。

#### 2.1.1 SQL2 基础语法

**基本结构**:
```sql
SELECT [列] FROM [节点类型] WHERE [条件] [ORDER BY] [LIMIT]
```

**关键组成部分**:
- **SELECT**: 指定返回的列（属性）
- **FROM**: 指定节点类型（必须是具体的节点类型，不能是抽象类型）
- **WHERE**: 查询条件
- **ORDER BY**: 排序
- **LIMIT/OFFSET**: 限制结果数量

#### 2.1.2 SELECT 子句

**选择所有属性**:
```java
String query = "SELECT * FROM [cq:Page]";
// * 表示返回节点的所有属性
```

**选择特定属性**:
```java
String query = "SELECT jcr:title, jcr:path FROM [cq:Page]";
// 只返回 jcr:title 和 jcr:path 属性
```

**选择路径**:
```java
String query = "SELECT * FROM [cq:Page]";
// 结果中总是包含路径信息，可通过 Node.getPath() 获取
```

#### 2.1.3 FROM 子句 - 节点类型

**常见节点类型**:

```java
// 页面节点
"SELECT * FROM [cq:Page]"

// 页面内容节点（jcr:content）
"SELECT * FROM [cq:PageContent]"

// 组件节点
"SELECT * FROM [cq:Component]"

// 资源节点
"SELECT * FROM [nt:resource]"

// 文件节点
"SELECT * FROM [nt:file]"

// 任意节点（性能差，不推荐）
"SELECT * FROM [nt:base]"
```

**节点类型层次结构**:
```
nt:base (基础类型，抽象)
  ├── nt:hierarchyNode
  │   ├── nt:folder
  │   ├── nt:file
  │   └── cq:Page
  └── nt:unstructured
      ├── cq:PageContent
      └── cq:Component
```

**选择正确的节点类型**:
```java
// ❌ 不好：使用基础类型
"SELECT * FROM [nt:base]"
// 会匹配所有节点，性能极差

// ✅ 好：使用具体类型
"SELECT * FROM [cq:Page]"
// 只匹配页面节点，性能好
```

#### 2.1.4 WHERE 子句 - 条件表达式

**路径条件**:

```java
// ISDESCENDANTNODE - 查找指定路径下的所有子节点
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content/my-site')";

// ISCHILDNODE - 查找直接子节点（不递归）
String query2 = "SELECT * FROM [cq:Page] " +
                "WHERE ISCHILDNODE('/content/my-site')";

// ISDESCENDANTNODE 可以指定路径和名称
String query3 = "SELECT * FROM [cq:Page] " +
                "WHERE ISDESCENDANTNODE('/content', 'my-site')";
```

**属性条件**:

```java
// 精确匹配
String query = "SELECT * FROM [cq:Page] " +
               "WHERE jcr:title = '首页'";

// LIKE 模式匹配
String query2 = "SELECT * FROM [cq:Page] " +
                "WHERE jcr:title LIKE '%产品%'";
// % 表示任意字符，_ 表示单个字符

// CONTAINS 全文搜索（需要全文索引）
String query3 = "SELECT * FROM [cq:Page] " +
                "WHERE CONTAINS(jcr:title, '产品')";

// 属性存在性检查
String query4 = "SELECT * FROM [cq:Page] " +
                "WHERE productCategory IS NOT NULL";

// 属性不存在
String query5 = "SELECT * FROM [cq:Page] " +
                "WHERE productCategory IS NULL";
```

**比较操作符**:

```java
// 等于
"jcr:title = '值'"

// 不等于
"jcr:title <> '值'"

// 大于/小于（适用于数值和日期）
"jcr:created > TIMESTAMP '2024-01-01T00:00:00.000Z'"

// 大于等于/小于等于
"jcr:created >= TIMESTAMP '2024-01-01T00:00:00.000Z'"

// IN 操作符（匹配多个值）
"status IN ('published', 'draft')"

// NOT IN
"status NOT IN ('deleted', 'archived')"
```

**逻辑操作符**:

```java
// AND - 与
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND jcr:title LIKE '%产品%' " +
               "AND status = 'published'";

// OR - 或
String query2 = "SELECT * FROM [cq:Page] " +
                "WHERE ISDESCENDANTNODE('/content') " +
                "AND (jcr:title LIKE '%产品%' " +
                "     OR jcr:title LIKE '%商品%')";

// NOT - 非
String query3 = "SELECT * FROM [cq:Page] " +
                "WHERE ISDESCENDANTNODE('/content') " +
                "AND NOT (status = 'deleted')";
```

**日期和时间条件**:

```java
// 使用 TIMESTAMP 函数
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND jcr:created > TIMESTAMP '2024-01-01T00:00:00.000Z'";

// 查找最近修改的页面（相对时间）
// 注意：Oak SQL2 不支持 NOW()，需要在应用层计算
Calendar lastWeek = Calendar.getInstance();
lastWeek.add(Calendar.DAY_OF_YEAR, -7);
String timestamp = formatTimestamp(lastWeek);

String query2 = "SELECT * FROM [cq:Page] " +
                "WHERE ISDESCENDANTNODE('/content') " +
                "AND jcr:lastModified > TIMESTAMP '" + timestamp + "'";
```

#### 2.1.5 ORDER BY 子句

```java
// 按属性排序
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "ORDER BY jcr:title";

// 降序排序
String query2 = "SELECT * FROM [cq:Page] " +
                "WHERE ISDESCENDANTNODE('/content') " +
                "ORDER BY jcr:created DESC";

// 多字段排序
String query3 = "SELECT * FROM [cq:Page] " +
                "WHERE ISDESCENDANTNODE('/content') " +
                "ORDER BY status ASC, jcr:created DESC";
```

#### 2.1.6 LIMIT 和 OFFSET

```java
// 限制结果数量
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "LIMIT 100";

// 分页查询
String query2 = "SELECT * FROM [cq:Page] " +
                "WHERE ISDESCENDANTNODE('/content') " +
                "ORDER BY jcr:created DESC " +
                "LIMIT 50 OFFSET 100";
// LIMIT 50: 返回 50 条结果
// OFFSET 100: 跳过前 100 条结果
```

#### 2.1.7 CONTAINS 函数详解

**CONTAINS 语法**:
```sql
CONTAINS(属性名, '搜索词')
```

**特性**:
- 需要全文索引支持（Lucene 索引）
- 支持词搜索（单词边界）
- 支持短语搜索
- 不区分大小写（取决于索引配置）

**示例**:

```java
// 简单词搜索
String query = "SELECT * FROM [cq:Page] " +
               "WHERE CONTAINS(jcr:title, '产品')";

// 多个词（AND 关系）
String query2 = "SELECT * FROM [cq:Page] " +
                "WHERE CONTAINS(jcr:title, '产品 新品')";
// 查找标题同时包含 "产品" 和 "新品" 的页面

// 短语搜索（需要引号）
String query3 = "SELECT * FROM [cq:Page] " +
                "WHERE CONTAINS(jcr:title, '\"新产品\"')";
// 查找标题包含短语 "新产品" 的页面

// 多字段搜索
String query4 = "SELECT * FROM [cq:Page] " +
                "WHERE CONTAINS(jcr:title, '产品') " +
                "OR CONTAINS(jcr:description, '产品')";
```

**CONTAINS vs LIKE**:

| 特性 | CONTAINS | LIKE |
|------|----------|------|
| **索引支持** | 需要全文索引 | 需要属性索引 |
| **性能** | 通常更好（索引优化） | 取决于索引 |
| **功能** | 词搜索、短语搜索 | 模式匹配 |
| **大小写** | 不区分（通常） | 区分（取决于索引） |
| **推荐使用** | 文本内容搜索 | 精确模式匹配 |

#### 2.1.8 查询 jcr:content 节点的属性

**问题**: 页面内容通常存储在 `jcr:content` 子节点中

**解决方案 1: 查询 cq:PageContent 类型**

```java
// 查询页面内容节点
String query = "SELECT * FROM [cq:PageContent] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND CONTAINS(jcr:title, '产品')";

QueryResult result = jcrQuery.execute();
NodeIterator nodes = result.getNodes();
while (nodes.hasNext()) {
    Node contentNode = nodes.nextNode();
    // contentNode 是 jcr:content 节点
    Node pageNode = contentNode.getParent(); // 获取父页面节点
    String pagePath = pageNode.getPath();
}
```

**解决方案 2: 使用属性路径**

```java
// 在某些情况下，可以直接访问子节点属性
// 但这不是标准 SQL2，可能不支持
// 推荐使用方案 1
```

#### 2.1.9 实际查询示例

**示例 1: 基础属性查询**

```java
// 查找 jcr:title 包含 "产品" 的所有页面
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND CONTAINS(jcr:title, '产品')";

QueryManager queryManager = session.getWorkspace().getQueryManager();
Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
QueryResult result = jcrQuery.execute();

NodeIterator nodes = result.getNodes();
while (nodes.hasNext()) {
    Node pageNode = nodes.nextNode();
    System.out.println("找到页面: " + pageNode.getPath());
}
```

**示例 2: 使用 LIKE 查询**

```java
// 查找 jcr:title 包含 "产品" 的页面（使用 LIKE）
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND jcr:title LIKE '%产品%'";
// % 表示任意字符序列
// _ 表示单个字符

QueryManager queryManager = session.getWorkspace().getQueryManager();
Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
QueryResult result = jcrQuery.execute();
```

**示例 3: 查询自定义属性**

```java
// 查找自定义属性包含关键词的页面
String query = "SELECT * FROM [cq:PageContent] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND productCategory LIKE '%电子%'";

// 注意：需要确保属性已建立索引
```

**示例 4: 多条件组合查询**

```java
// AND 条件
String query = "SELECT * FROM [cq:PageContent] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND CONTAINS(jcr:title, '产品') " +
               "AND CONTAINS(jcr:description, '新品') " +
               "AND status = 'published'";

// OR 条件
String query2 = "SELECT * FROM [cq:PageContent] " +
                "WHERE ISDESCENDANTNODE('/content') " +
                "AND (CONTAINS(jcr:title, '产品') " +
                "     OR CONTAINS(jcr:title, '商品'))";

// 复杂组合
String query3 = "SELECT * FROM [cq:PageContent] " +
                "WHERE ISDESCENDANTNODE('/content') " +
                "AND (CONTAINS(jcr:title, '产品') " +
                "     OR CONTAINS(jcr:description, '产品')) " +
                "AND status IN ('published', 'draft') " +
                "AND jcr:created > TIMESTAMP '2024-01-01T00:00:00.000Z'";
```

**示例 5: 排序和分页**

```java
// 按修改时间倒序，限制 20 条
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND CONTAINS(jcr:title, '产品') " +
               "ORDER BY jcr:lastModified DESC " +
               "LIMIT 20";

// 分页查询
int pageSize = 20;
int pageNumber = 2; // 第 2 页
int offset = (pageNumber - 1) * pageSize;

String pagedQuery = "SELECT * FROM [cq:Page] " +
                    "WHERE ISDESCENDANTNODE('/content') " +
                    "AND CONTAINS(jcr:title, '产品') " +
                    "ORDER BY jcr:lastModified DESC " +
                    "LIMIT " + pageSize + " OFFSET " + offset;
```

#### 2.1.10 SQL2 查询转义

**字符串转义**:

```java
// SQL2 字符串中的单引号需要转义为两个单引号
public String escapeSQL2(String value) {
    return value.replace("'", "''");
}

// 示例
String keyword = "O'Brien"; // 包含单引号
String query = "SELECT * FROM [cq:Page] " +
               "WHERE jcr:title = '" + escapeSQL2(keyword) + "'";
// 结果: WHERE jcr:title = 'O''Brien'
```

#### 2.1.11 SQL2 查询性能提示

1. **总是使用索引**: 确保查询条件匹配已建立的索引
2. **限制查询范围**: 使用 ISDESCENDANTNODE 限制路径
3. **使用具体节点类型**: 避免 FROM [nt:base]
4. **优先使用 CONTAINS**: 对于文本搜索，CONTAINS 通常比 LIKE 性能更好
5. **使用 LIMIT**: 限制返回结果数量
6. **避免函数调用**: 在 WHERE 子句中避免使用函数（如 LOWER、UPPER）

### 2.2 XPath 查询详解

XPath 是 JCR 支持的另一种查询语言，语法更接近 XML 路径表达。在某些场景下，XPath 查询可能更直观。

#### 2.2.1 XPath 基础语法

**基本结构**:
```
/jcr:root/路径//元素类型[条件]
```

**关键符号**:
- `/` - 绝对路径（从根开始）
- `//` - 递归搜索（任意深度）
- `*` - 通配符（匹配所有）
- `@` - 属性访问符
- `[]` - 条件表达式
- `element(名称, 类型)` - 节点类型匹配

#### 2.2.2 XPath 查询示例

**示例 1: 基本路径查询**

```java
// 查找 /content 下所有页面节点
String xpathQuery = "/jcr:root/content//element(*, cq:Page)";

QueryManager queryManager = session.getWorkspace().getQueryManager();
Query query = queryManager.createQuery(xpathQuery, Query.XPATH);
QueryResult result = query.execute();

NodeIterator nodes = result.getNodes();
while (nodes.hasNext()) {
    Node node = nodes.nextNode();
    System.out.println("页面路径: " + node.getPath());
}
```

**示例 2: 属性条件查询**

```java
// 查找标题包含 "产品" 的页面
String xpathQuery = "/jcr:root/content//element(*, cq:Page)" +
                    "[jcr:contains(@jcr:title, '产品')]";

// 说明：
// - /jcr:root/content: 从 /content 开始
// - //element(*, cq:Page): 递归查找所有 cq:Page 类型节点
// - [jcr:contains(@jcr:title, '产品')]: 条件 - jcr:title 属性包含 "产品"
```

**示例 3: 精确匹配查询**

```java
// 查找标题等于 "首页" 的页面
String xpathQuery = "/jcr:root/content//element(*, cq:Page)" +
                    "[@jcr:title='首页']";

// 或者使用 text() 函数
String xpathQuery2 = "/jcr:root/content//element(*, cq:Page)" +
                     "[jcr:title/text()='首页']";
```

**示例 4: 多条件查询**

```java
// AND 条件：标题包含 "产品" 且描述包含 "新品"
String xpathQuery = "/jcr:root/content//element(*, cq:Page)" +
                    "[jcr:contains(@jcr:title, '产品') " +
                    " and jcr:contains(@jcr:description, '新品')]";

// OR 条件：标题包含 "产品" 或 "商品"
String xpathQuery2 = "/jcr:root/content//element(*, cq:Page)" +
                     "[jcr:contains(@jcr:title, '产品') " +
                     " or jcr:contains(@jcr:title, '商品')]";
```

**示例 5: 路径限制查询**

```java
// 只在 /content/my-site 下查找
String xpathQuery = "/jcr:root/content/my-site//element(*, cq:Page)" +
                    "[jcr:contains(@jcr:title, '产品')]";

// 限制深度（只查找直接子节点）
String xpathQuery2 = "/jcr:root/content/my-site/*/element(*, cq:Page)" +
                     "[jcr:contains(@jcr:title, '产品')]";
```

**示例 6: 节点存在性检查**

```java
// 查找有 jcr:content 子节点的页面
String xpathQuery = "/jcr:root/content//element(*, cq:Page)" +
                    "[jcr:content]";

// 查找有特定属性的节点
String xpathQuery2 = "/jcr:root/content//element(*, cq:Page)" +
                     "[@productCategory]";
```

**示例 7: 属性值比较**

```java
// 数值比较：查找修改时间大于某个值的页面
// 注意：XPath 对日期/时间比较支持有限，建议使用 SQL2

// 字符串比较：精确匹配
String xpathQuery = "/jcr:root/content//element(*, cq:Page)" +
                    "[@status='published']";
```

#### 2.2.3 XPath 函数

**常用函数**:

1. **jcr:contains()** - 全文搜索
```java
// 在属性中搜索文本
jcr:contains(@jcr:title, '关键词')
```

2. **jcr:like()** - 模式匹配（类似 SQL LIKE）
```java
// 注意：Oak 中可能不支持 jcr:like，建议使用 jcr:contains
```

3. **text()** - 获取文本内容
```java
// 获取属性值
@jcr:title/text()
```

4. **name()** - 获取节点名称
```java
// 按节点名称过滤
[name()='jcr:content']
```

5. **fn:upper-case() / fn:lower-case()** - 大小写转换
```java
// 注意：函数调用可能阻止索引使用
```

#### 2.2.4 XPath vs SQL2 对比

| 特性 | XPath | SQL2 |
|------|-------|------|
| **语法风格** | XML 路径风格 | SQL 风格 |
| **易读性** | 对熟悉 XML 的人更直观 | 对熟悉 SQL 的人更直观 |
| **功能** | 相对简单，功能有限 | 功能更强大，支持更多操作 |
| **性能** | 通常与 SQL2 相当 | 通常与 XPath 相当 |
| **索引支持** | 支持 | 支持 |
| **推荐使用** | 简单的路径和属性查询 | 复杂查询、多表关联、聚合等 |

**选择建议**:
- **使用 XPath**: 简单的路径查询、熟悉 XML 路径表达式
- **使用 SQL2**: 复杂条件、多表关联、聚合函数、需要更多控制

#### 2.2.5 XPath 查询优化

**优化技巧**:

1. **限制路径范围**
```java
// ❌ 不好：从根开始搜索
String query = "/jcr:root//element(*, cq:Page)";

// ✅ 好：限制搜索路径
String query = "/jcr:root/content/my-site//element(*, cq:Page)";
```

2. **使用具体节点类型**
```java
// ❌ 不好：使用通配符
String query = "/jcr:root/content//*";

// ✅ 好：使用具体类型
String query = "/jcr:root/content//element(*, cq:Page)";
```

3. **避免深度嵌套**
```java
// ❌ 避免过度嵌套的条件
String query = "/jcr:root/content//element(*, cq:Page)" +
               "[jcr:contains(@jcr:title, '产品') " +
               " and (jcr:contains(@jcr:description, '新品') " +
               "      or jcr:contains(@jcr:description, '特价'))]";

// ✅ 如果条件复杂，考虑使用 SQL2
```

#### 2.2.6 XPath 完整示例

```java
@Component(service = {})
public class XPathSearchService {
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    /**
     * 使用 XPath 查找页面
     */
    public List<String> searchPagesWithXPath(String keyword, String basePath) 
            throws Exception {
        
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "search-service");
        
        try (ResourceResolver resolver = 
                resourceResolverFactory.getServiceResourceResolver(authInfo)) {
            
            Session session = resolver.adaptTo(Session.class);
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            
            // 构建 XPath 查询
            String xpathQuery = "/jcr:root" + basePath + 
                               "//element(*, cq:Page)" +
                               "[jcr:contains(@jcr:title, '" + 
                               escapeXPath(keyword) + "')]";
            
            Query query = queryManager.createQuery(xpathQuery, Query.XPATH);
            QueryResult result = query.execute();
            
            List<String> pagePaths = new ArrayList<>();
            NodeIterator nodes = result.getNodes();
            
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                pagePaths.add(node.getPath());
            }
            
            return pagePaths;
        }
    }
    
    /**
     * 转义 XPath 查询中的特殊字符
     */
    private String escapeXPath(String keyword) {
        // XPath 字符串中的单引号需要转义为两个单引号
        return keyword.replace("'", "''");
    }
}
```

### 2.3 多条件组合查询

```java
// 查找标题包含 "产品" 且描述包含 "新品" 的页面
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND CONTAINS(jcr:title, '产品') " +
               "AND CONTAINS(jcr:description, '新品')";

// 或者使用 AND/OR 组合
String complexQuery = "SELECT * FROM [cq:Page] " +
                      "WHERE ISDESCENDANTNODE('/content') " +
                      "AND (CONTAINS(jcr:title, '产品') " +
                      "     OR CONTAINS(jcr:title, '商品'))";
```

---

## 3. 索引优化详解

### 3.1 为什么需要索引？

#### 3.1.1 没有索引的问题

**全表扫描的性能问题**:

```java
// 没有索引时，查询需要遍历所有节点
// 假设 /content 下有 100,000 个节点

// 查询: 查找 jcr:title = '首页' 的页面
// 执行过程:
// 1. 从 /content 开始
// 2. 递归遍历所有子节点
// 3. 检查每个节点的 jcr:title 属性
// 4. 比较属性值是否匹配
// 
// 最坏情况: 需要检查 100,000 个节点
// 平均情况: 需要检查 50,000 个节点
// 时间复杂度: O(n) - 线性时间
// 查询时间: 几秒到几分钟（取决于节点数量）
```

**性能影响**:
- **节点数量**: 10,000 节点 → 查询时间约 1-2 秒
- **节点数量**: 100,000 节点 → 查询时间约 10-30 秒
- **节点数量**: 1,000,000 节点 → 查询时间可能超过 1 分钟
- **并发查询**: 多个查询同时执行时，性能进一步下降

#### 3.1.2 有索引的优势

**索引的工作原理**:

```
索引结构（简化示意）:

属性值 → 节点路径映射表
─────────────────────
"首页"    → /content/my-site/en/home
"产品"    → /content/my-site/en/products
          → /content/my-site/en/products/electronics
"关于"    → /content/my-site/en/about

查询过程:
1. 查找索引: "首页" → 直接定位到节点路径
2. 访问节点: /content/my-site/en/home
3. 返回结果

时间复杂度: O(log n) - 对数时间（使用 B-Tree）
查询时间: 通常 < 100ms
```

**性能提升**:
- **节点数量**: 10,000 节点 → 查询时间约 10-50ms
- **节点数量**: 100,000 节点 → 查询时间约 20-100ms
- **节点数量**: 1,000,000 节点 → 查询时间约 50-200ms
- **性能提升**: 10-1000 倍

#### 3.1.3 索引的成本

**索引的代价**:
- **存储空间**: 索引需要额外的存储空间（通常 10-30% 的原始数据大小）
- **写入性能**: 插入/更新/删除节点时需要更新索引（轻微影响）
- **维护成本**: 需要定期重建索引（通常自动完成）

**何时使用索引**:
- ✅ **推荐**: 经常查询的属性
- ✅ **推荐**: 查询性能要求高的场景
- ❌ **不推荐**: 很少查询的属性
- ❌ **不推荐**: 频繁更新的属性（如果写入性能更重要）

### 3.2 Oak 索引类型详解

Oak（AEM 的底层存储引擎）支持多种索引类型，每种类型适用于不同的查询场景。

#### 3.2.1 Property Index（属性索引）

**适用场景**:
- ✅ 精确匹配查询（`property = 'value'`）
- ✅ 范围查询（`property > value`）
- ✅ IN 查询（`property IN ('value1', 'value2')`）
- ✅ 属性存在性检查（`property IS NOT NULL`）
- ❌ 不适用于全文搜索（CONTAINS）

**工作原理**:
```
Property Index 结构（B-Tree）:

属性值 → 节点路径列表
─────────────────────────────
"首页"     → [/content/my-site/en/home]
"产品"     → [/content/my-site/en/products,
              /content/my-site/en/products/electronics]
"关于"     → [/content/my-site/en/about]

查询: jcr:title = '产品'
→ 直接查找索引中的 "产品"
→ 返回对应的节点路径列表
→ 时间复杂度: O(log n)
```

**配置结构**:

```json
{
  "jcr:primaryType": "oak:QueryIndexDefinition",
  "type": "property",
  "propertyNames": ["jcr:title", "productCategory"],
  "async": "async",
  "reindex": true,
  "unique": false,
  "declaringNodeTypes": ["cq:Page"]
}
```

**配置属性说明**:
- **type**: 索引类型，固定为 "property"
- **propertyNames**: 要索引的属性名数组
- **async**: 异步索引（推荐 "async"）
- **reindex**: 是否重建索引（创建时设为 true）
- **unique**: 属性值是否唯一（可选）
- **declaringNodeTypes**: 限制索引的节点类型（可选）

**支持的查询类型**:

```java
// ✅ 精确匹配
"WHERE jcr:title = '首页'"

// ✅ 不等于
"WHERE jcr:title <> '首页'"

// ✅ IN 查询
"WHERE status IN ('published', 'draft')"

// ✅ 范围查询
"WHERE jcr:created > TIMESTAMP '2024-01-01T00:00:00.000Z'"

// ✅ 存在性检查
"WHERE productCategory IS NOT NULL"

// ❌ 不支持 LIKE（部分支持，但不推荐）
"WHERE jcr:title LIKE '%产品%'"

// ❌ 不支持 CONTAINS
"WHERE CONTAINS(jcr:title, '产品')"
```

**创建索引的 Groovy 脚本**:

```groovy
def session = resourceResolver.adaptTo(javax.jcr.Session.class)
def rootNode = session.rootNode

// 创建属性索引
def indexNode = rootNode.addNode("oak:index/titleIndex", "oak:QueryIndexDefinition")
indexNode.setProperty("jcr:primaryType", "oak:QueryIndexDefinition")
indexNode.setProperty("type", "property")

// 设置要索引的属性
indexNode.setProperty("propertyNames", ["jcr:title", "productCategory"] as String[])

// 使用异步索引（推荐）
indexNode.setProperty("async", "async")

// 触发索引重建
indexNode.setProperty("reindex", true)

// 可选：限制节点类型
// indexNode.setProperty("declaringNodeTypes", ["cq:Page"] as String[])

session.save()
println "属性索引创建成功，开始重建索引..."
```

**使用 CRXDE Lite 创建**:

1. 访问 `http://localhost:4502/crx/de/index.jsp`
2. 导航到 `/oak:index`
3. 创建新节点: `titleIndex`
4. 设置属性:
   - `jcr:primaryType`: `oak:QueryIndexDefinition`
   - `type`: `property`
   - `propertyNames`: `["jcr:title", "productCategory"]`
   - `async`: `async`
   - `reindex`: `true`
5. 保存

**性能特点**:
- **查询性能**: 优秀（O(log n)）
- **存储开销**: 中等（约 10-20% 的原始数据大小）
- **写入性能影响**: 较小（异步索引）
- **适用数据量**: 适合任意规模

#### 3.2.2 Fulltext Index（全文索引 / Lucene 索引）

**适用场景**:
- ✅ 全文搜索（`CONTAINS(property, 'keyword')`）
- ✅ 词搜索和短语搜索
- ✅ 多字段搜索
- ✅ 模糊搜索（部分支持）
- ✅ 相关性排序
- ❌ 精确匹配（可以使用，但 Property Index 更高效）

**工作原理**:

```
Lucene 全文索引结构（倒排索引）:

词 → 文档ID列表（包含位置信息）
─────────────────────────────────
"产品" → [doc1@title:0, doc2@title:0, doc3@description:5]
"新品" → [doc1@title:3, doc4@title:0]
"电子" → [doc2@description:2, doc5@title:0]

查询: CONTAINS(jcr:title, '产品')
→ 查找索引中的 "产品"
→ 返回包含该词的文档列表
→ 支持相关性评分和排序
```

**配置结构**:

```json
{
  "jcr:primaryType": "oak:QueryIndexDefinition",
  "type": "lucene",
  "async": "async",
  "reindex": true,
  "includedPaths": ["/content"],
  "excludedPaths": ["/content/dam"],
  "indexRules": {
    "jcr:primaryType": "nt:base",
    "properties": [
      {
        "name": "jcr:title",
        "propertyIndex": true,
        "analyzed": true,
        "nodeScopeIndex": false
      },
      {
        "name": "jcr:description",
        "propertyIndex": true,
        "analyzed": true
      }
    ]
  }
}
```

**配置属性说明**:
- **type**: 索引类型，固定为 "lucene"
- **async**: 异步索引（推荐 "async"）
- **includedPaths**: 索引包含的路径（可选，提高性能）
- **excludedPaths**: 索引排除的路径（可选，如 `/content/dam`）
- **indexRules**: 索引规则配置
  - **properties**: 要索引的属性列表
    - **propertyIndex**: 是否索引（true）
    - **analyzed**: 是否分析（true = 全文索引，false = 精确匹配）
    - **nodeScopeIndex**: 节点范围索引（可选）

**属性配置选项**:

```json
{
  "name": "jcr:title",
  "propertyIndex": true,      // 是否索引此属性
  "analyzed": true,           // true = 全文索引，false = 精确匹配
  "nodeScopeIndex": false,    // 节点范围索引
  "useInExcerpt": true,       // 是否在摘要中使用
  "boost": 2.0,              // 相关性权重（可选）
  "nullCheckEnabled": true    // 空值检查（可选）
}
```

**支持的查询类型**:

```java
// ✅ CONTAINS 全文搜索
"WHERE CONTAINS(jcr:title, '产品')"

// ✅ 多词搜索（AND）
"WHERE CONTAINS(jcr:title, '产品 新品')"

// ✅ 短语搜索
"WHERE CONTAINS(jcr:title, '\"新产品\"')"

// ✅ 多字段搜索
"WHERE CONTAINS(jcr:title, '产品') OR CONTAINS(jcr:description, '产品')"

// ✅ 相关性排序（自动支持）
// CONTAINS 查询结果按相关性排序

// ⚠️ 精确匹配（可以使用，但 Property Index 更高效）
"WHERE jcr:title = '产品'"
```

**创建全文索引的完整 Groovy 脚本**:

```groovy
def session = resourceResolver.adaptTo(javax.jcr.Session.class)
def rootNode = session.rootNode

// 检查索引是否已存在
if (rootNode.hasNode("oak:index/lucene-content")) {
    println "索引已存在，将更新配置"
    def indexNode = rootNode.getNode("oak:index/lucene-content")
    indexNode.setProperty("reindex", true)
} else {
    // 创建 Lucene 全文索引
    def indexNode = rootNode.addNode("oak:index/lucene-content", "oak:QueryIndexDefinition")
    indexNode.setProperty("jcr:primaryType", "oak:QueryIndexDefinition")
    indexNode.setProperty("type", "lucene")
    indexNode.setProperty("async", "async")
    indexNode.setProperty("reindex", true)

    // 设置索引范围（可选，但推荐）
    indexNode.setProperty("includedPaths", ["/content"] as String[])
    indexNode.setProperty("excludedPaths", ["/content/dam"] as String[])

    // 配置索引规则
    def indexRules = indexNode.addNode("indexRules", "nt:unstructured")
    
    // 为 cq:Page 类型配置规则
    def cqPage = indexRules.addNode("cq:Page", "nt:unstructured")
    def properties = cqPage.addNode("properties", "nt:unstructured")

    // jcr:title - 全文索引
    def titleProp = properties.addNode("jcr:title", "nt:unstructured")
    titleProp.setProperty("propertyIndex", true)
    titleProp.setProperty("analyzed", true)
    titleProp.setProperty("useInExcerpt", true)

    // jcr:description - 全文索引
    def descProp = properties.addNode("jcr:description", "nt:unstructured")
    descProp.setProperty("propertyIndex", true)
    descProp.setProperty("analyzed", true)
    descProp.setProperty("useInExcerpt", true)

    // productCategory - 精确匹配（如果需要）
    def categoryProp = properties.addNode("productCategory", "nt:unstructured")
    categoryProp.setProperty("propertyIndex", true)
    categoryProp.setProperty("analyzed", false)  // false = 精确匹配
}

session.save()
println "全文索引配置完成，开始重建索引..."
```

**性能特点**:
- **查询性能**: 优秀（O(log n)），支持相关性排序
- **存储开销**: 较大（约 20-40% 的原始数据大小）
- **写入性能影响**: 中等（异步索引，但索引更大）
- **适用数据量**: 适合任意规模
- **重建时间**: 较长（索引较大）

**分析器（Analyzer）配置**:

Lucene 索引使用分析器处理文本，默认使用标准分析器（StandardAnalyzer）:
- 分词（Tokenization）
- 小写转换（Lowercasing）
- 停用词过滤（Stop words）
- 词干提取（Stemming，可选）

可以通过配置自定义分析器（高级用法，通常使用默认即可）。

#### 3.2.3 Node Type Index（节点类型索引）

**适用场景**: 按节点类型查询

```json
{
  "jcr:primaryType": "oak:QueryIndexDefinition",
  "type": "property",
  "propertyNames": ["jcr:primaryType"],
  "async": "async"
}
```

**说明**: 
- Oak 默认已经为 `jcr:primaryType` 建立了索引
- 通常不需要手动创建
- 用于查询 `FROM [cq:Page]` 这样的节点类型查询

#### 3.2.4 索引类型选择指南

**选择流程图**:

```
需要查询的属性
    │
    ├─ 需要全文搜索（CONTAINS）？
    │   ├─ 是 → 使用 Lucene 全文索引
    │   └─ 否 ↓
    │
    ├─ 需要精确匹配（=, IN, >, <）？
    │   ├─ 是 → 使用 Property Index
    │   └─ 否 ↓
    │
    └─ 查询节点类型？
        └─ 是 → 使用默认 Node Type Index（通常已存在）
```

**对比表**:

| 特性 | Property Index | Lucene Index | Node Type Index |
|------|---------------|--------------|-----------------|
| **精确匹配** | ✅ 优秀 | ⚠️ 可用（但效率较低） | ✅ 优秀 |
| **范围查询** | ✅ 支持 | ❌ 不支持 | ❌ 不支持 |
| **全文搜索** | ❌ 不支持 | ✅ 优秀 | ❌ 不支持 |
| **存储开销** | 小（10-20%） | 大（20-40%） | 很小（<5%） |
| **查询性能** | 优秀 | 优秀 | 优秀 |
| **重建时间** | 快 | 慢 | 快 |
| **适用场景** | 精确匹配、范围查询 | 文本搜索 | 节点类型查询 |

**实际选择建议**:

1. **标题搜索**:
   ```java
   // 场景: 搜索页面标题
   // 查询: CONTAINS(jcr:title, '关键词')
   // 选择: Lucene 全文索引
   ```

2. **状态查询**:
   ```java
   // 场景: 查找特定状态的页面
   // 查询: status = 'published'
   // 选择: Property Index
   ```

3. **日期范围查询**:
   ```java
   // 场景: 查找特定日期范围的页面
   // 查询: jcr:created > TIMESTAMP '2024-01-01'
   // 选择: Property Index
   ```

4. **组合查询**:
   ```java
   // 场景: 标题搜索 + 状态过滤
   // 查询: CONTAINS(jcr:title, '产品') AND status = 'published'
   // 选择: 
   //   - jcr:title: Lucene 索引
   //   - status: Property Index
   //   两个索引可以组合使用
   ```

### 3.3 索引管理详解

#### 3.3.1 索引生命周期

**索引的创建和更新流程**:

```
1. 创建索引定义
   ↓
2. 设置 reindex = true
   ↓
3. 保存节点
   ↓
4. 异步索引任务启动
   ↓
5. 遍历所有节点，构建索引
   ↓
6. reindex 自动设为 false（完成后）
   ↓
7. 索引可用，查询可以使用索引
```

**增量更新**:
- 当节点被创建/更新/删除时，索引会自动更新（异步）
- 不需要手动触发重建（除非索引损坏或配置更改）

#### 3.3.2 检查索引状态

**检查索引是否存在**:

```groovy
def session = resourceResolver.adaptTo(javax.jcr.Session.class)
def rootNode = session.rootNode

def indexPath = "oak:index/lucene-content"
if (rootNode.hasNode(indexPath)) {
    println "索引存在"
    def indexNode = rootNode.getNode(indexPath)
    
    // 检查索引类型
    if (indexNode.hasProperty("type")) {
        println "索引类型: ${indexNode.getProperty('type').string}"
    }
    
    // 检查是否正在重建
    if (indexNode.hasProperty("reindex")) {
        boolean reindexing = indexNode.getProperty("reindex").boolean
        println "索引重建中: ${reindexing}"
    }
    
    // 检查重建次数
    if (indexNode.hasProperty("reindexCount")) {
        long count = indexNode.getProperty("reindexCount").long
        println "重建次数: ${count}"
    }
    
    // 检查索引大小（如果可用）
    if (indexNode.hasProperty("size")) {
        long size = indexNode.getProperty("size").long
        println "索引大小: ${size} bytes"
    }
} else {
    println "索引不存在"
}
```

**检查所有索引**:

```groovy
def session = resourceResolver.adaptTo(javax.jcr.Session.class)
def rootNode = session.rootNode

if (rootNode.hasNode("oak:index")) {
    def indexParent = rootNode.getNode("oak:index")
    indexParent.nodes.each { indexNode ->
        println "\n索引: ${indexNode.name}"
        if (indexNode.hasProperty("type")) {
            println "  类型: ${indexNode.getProperty('type').string}"
        }
        if (indexNode.hasProperty("reindex")) {
            println "  重建中: ${indexNode.getProperty('reindex').boolean}"
        }
        if (indexNode.hasProperty("propertyNames")) {
            def props = indexNode.getProperty("propertyNames").values*.string
            println "  属性: ${props.join(', ')}"
        }
    }
}
```

#### 3.3.3 手动触发索引重建

**何时需要重建索引**:
- 索引配置更改（添加/删除属性、修改配置）
- 索引损坏或不一致
- 数据迁移后
- 索引长时间未更新

**触发重建的方法**:

```groovy
// 方法 1: 设置 reindex 属性
def session = resourceResolver.adaptTo(javax.jcr.Session.class)
def rootNode = session.rootNode

def indexNode = rootNode.getNode("oak:index/lucene-content")
indexNode.setProperty("reindex", true)
session.save()
println "索引重建已触发"

// 方法 2: 删除并重新创建（不推荐，会丢失索引数据）
// 只在索引严重损坏时使用
```

**使用 AEM 控制台触发**:
1. 访问 `http://localhost:4502/system/console/jmx`
2. 找到 `org.apache.jackrabbit.oak:name=AsyncIndexUpdate,type=IndexUpdate`
3. 调用 `startReindex()` 方法

#### 3.3.4 检查查询是否使用索引

**方法 1: 查看日志**

在 `log4j2.xml` 中启用查询日志:

```xml
<Logger name="org.apache.jackrabbit.oak.query" level="DEBUG"/>
```

查询执行后，日志中会显示:
```
Query: SELECT * FROM [cq:Page] WHERE CONTAINS(jcr:title, '产品')
Plan: [cq:Page] as [a] /* lucene-content:jcr:title:产品 */
```

如果看到 `lucene-content`，说明使用了索引。

**方法 2: 使用 QueryPlan**

```java
QueryManager queryManager = session.getWorkspace().getQueryManager();
String queryString = "SELECT * FROM [cq:Page] " +
                     "WHERE CONTAINS(jcr:title, '产品')";

Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);

// 获取执行计划（某些 AEM 版本支持）
String plan = query.getPlan();
System.out.println("执行计划: " + plan);
// 输出可能包含: "lucene-content" 表示使用了索引
```

**方法 3: 性能对比**

```java
// 有索引的查询（应该很快）
long startTime = System.currentTimeMillis();
QueryResult result = query.execute();
long duration = System.currentTimeMillis() - startTime;

if (duration < 100) {
    System.out.println("查询快速，可能使用了索引: " + duration + "ms");
} else {
    System.out.println("查询较慢，可能未使用索引: " + duration + "ms");
}
```

#### 3.3.5 索引维护最佳实践

**定期检查**:
- 每周检查索引状态
- 监控索引大小
- 检查重建次数（异常增加可能表示问题）

**性能监控**:
- 记录查询执行时间
- 识别慢查询
- 分析是否需要新索引

**索引优化**:
- 只索引需要的属性
- 使用 `includedPaths` 和 `excludedPaths` 限制范围
- 定期清理不再使用的索引

**故障处理**:
- 索引损坏：触发重建
- 查询慢：检查是否使用索引，考虑创建新索引
- 索引太大：考虑限制索引范围或减少索引属性

#### 3.3.6 索引管理工具类

```java
@Component(service = IndexManagementService.class)
public class IndexManagementService {
    
    private static final Logger LOG = LoggerFactory.getLogger(IndexManagementService.class);
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    /**
     * 检查索引状态
     */
    public IndexStatus checkIndexStatus(String indexName) throws Exception {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "index-service");
        
        try (ResourceResolver resolver = 
                resourceResolverFactory.getServiceResourceResolver(authInfo)) {
            
            Session session = resolver.adaptTo(Session.class);
            Node rootNode = session.getRootNode();
            
            String indexPath = "oak:index/" + indexName;
            if (!rootNode.hasNode(indexPath)) {
                return new IndexStatus(false, "索引不存在");
            }
            
            Node indexNode = rootNode.getNode(indexPath);
            IndexStatus status = new IndexStatus();
            status.setExists(true);
            
            if (indexNode.hasProperty("reindex")) {
                status.setReindexing(indexNode.getProperty("reindex").getBoolean());
            }
            
            if (indexNode.hasProperty("reindexCount")) {
                status.setReindexCount(indexNode.getProperty("reindexCount").getLong());
            }
            
            if (indexNode.hasProperty("type")) {
                status.setType(indexNode.getProperty("type").getString());
            }
            
            return status;
        }
    }
    
    /**
     * 触发索引重建
     */
    public void rebuildIndex(String indexName) throws Exception {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "index-service");
        
        try (ResourceResolver resolver = 
                resourceResolverFactory.getServiceResourceResolver(authInfo)) {
            
            Session session = resolver.adaptTo(Session.class);
            Node rootNode = session.getRootNode();
            
            String indexPath = "oak:index/" + indexName;
            if (!rootNode.hasNode(indexPath)) {
                throw new IllegalArgumentException("索引不存在: " + indexName);
            }
            
            Node indexNode = rootNode.getNode(indexPath);
            indexNode.setProperty("reindex", true);
            session.save();
            
            LOG.info("索引重建已触发: {}", indexName);
        }
    }
    
    public static class IndexStatus {
        private boolean exists;
        private boolean reindexing;
        private long reindexCount;
        private String type;
        private String message;
        
        public IndexStatus() {}
        
        public IndexStatus(boolean exists, String message) {
            this.exists = exists;
            this.message = message;
        }
        
        // Getters and Setters
        public boolean isExists() { return exists; }
        public void setExists(boolean exists) { this.exists = exists; }
        
        public boolean isReindexing() { return reindexing; }
        public void setReindexing(boolean reindexing) { this.reindexing = reindexing; }
        
        public long getReindexCount() { return reindexCount; }
        public void setReindexCount(long reindexCount) { this.reindexCount = reindexCount; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
```

---

## 4. 查询优化技巧

### 4.1 限制查询范围

**❌ 不好：查询整个 /content**

```java
String query = "SELECT * FROM [cq:Page] " +
               "WHERE CONTAINS(jcr:title, '产品')";
// 会搜索所有节点，包括 /content/dam 等
```

**✅ 好：限制路径范围**

```java
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content/my-site') " +
               "AND CONTAINS(jcr:title, '产品')";
// 只搜索 /content/my-site 下的节点
```

### 4.2 使用精确的节点类型

**❌ 不好：使用通用类型**

```java
String query = "SELECT * FROM [nt:base] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND CONTAINS(jcr:title, '产品')";
// 会匹配所有节点类型，性能差
```

**✅ 好：使用具体类型**

```java
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND CONTAINS(jcr:title, '产品')";
// 只匹配页面节点，性能好
```

### 4.3 避免在 WHERE 子句中使用函数

**❌ 不好：使用函数**

```java
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND LOWER(jcr:title) LIKE '%产品%'";
// LOWER() 函数会阻止索引使用
```

**✅ 好：直接查询（如果索引支持）**

```java
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND CONTAINS(jcr:title, '产品')";
// 使用全文索引的 CONTAINS，支持索引
```

### 4.4 限制返回结果数量

```java
// 使用 LIMIT 限制结果数量
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND CONTAINS(jcr:title, '产品') " +
               "LIMIT 100";

// 或者使用分页
String pagedQuery = "SELECT * FROM [cq:Page] " +
                    "WHERE ISDESCENDANTNODE('/content') " +
                    "AND CONTAINS(jcr:title, '产品') " +
                    "LIMIT 50 OFFSET 100";
```

### 4.5 使用绑定变量（如果支持）

```java
// 使用参数化查询（某些 AEM 版本支持）
String query = "SELECT * FROM [cq:Page] " +
               "WHERE ISDESCENDANTNODE('/content') " +
               "AND CONTAINS(jcr:title, $keyword)";

// 设置参数
Map<String, String> bindings = new HashMap<>();
bindings.put("keyword", "产品");
```

---

## 5. 实际应用示例

### 5.1 查找标题包含关键词的页面

```java
@Component(service = {})
public class PageSearchService {
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    public List<Page> searchPagesByTitle(String keyword) throws Exception {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "search-service");
        
        try (ResourceResolver resolver = 
                resourceResolverFactory.getServiceResourceResolver(authInfo)) {
            
            Session session = resolver.adaptTo(Session.class);
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            
            // 构建查询
            String query = "SELECT * FROM [cq:Page] " +
                          "WHERE ISDESCENDANTNODE('/content') " +
                          "AND CONTAINS(jcr:title, '" + escapeQuery(keyword) + "')";
            
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResult result = jcrQuery.execute();
            
            List<Page> pages = new ArrayList<>();
            NodeIterator nodes = result.getNodes();
            
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                Resource resource = resolver.getResource(node.getPath());
                PageManager pageManager = resolver.adaptTo(PageManager.class);
                Page page = pageManager.getPage(node.getPath());
                if (page != null) {
                    pages.add(page);
                }
            }
            
            return pages;
        }
    }
    
    private String escapeQuery(String keyword) {
        // 转义查询中的特殊字符
        return keyword.replace("'", "''")
                     .replace("\\", "\\\\");
    }
}
```

### 5.2 查找自定义属性包含关键词的页面

```java
public List<Page> searchPagesByCustomProperty(String propertyName, String keyword) 
        throws Exception {
    
    try (ResourceResolver resolver = 
            resourceResolverFactory.getServiceResourceResolver(authInfo)) {
        
        Session session = resolver.adaptTo(Session.class);
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        
        // 查询 jcr:content 节点的属性
        String query = "SELECT * FROM [cq:PageContent] " +
                      "WHERE ISDESCENDANTNODE('/content') " +
                      "AND CONTAINS(" + propertyName + ", '" + 
                      escapeQuery(keyword) + "')";
        
        Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
        QueryResult result = jcrQuery.execute();
        
        List<Page> pages = new ArrayList<>();
        NodeIterator nodes = result.getNodes();
        
        while (nodes.hasNext()) {
            Node contentNode = nodes.nextNode();
            // 获取父页面节点
            Node pageNode = contentNode.getParent();
            PageManager pageManager = resolver.adaptTo(PageManager.class);
            Page page = pageManager.getPage(pageNode.getPath());
            if (page != null) {
                pages.add(page);
            }
        }
        
        return pages;
    }
}
```

### 5.3 多条件组合搜索

```java
public List<Page> searchPagesAdvanced(
        String titleKeyword, 
        String descriptionKeyword,
        String category) throws Exception {
    
    try (ResourceResolver resolver = 
            resourceResolverFactory.getServiceResourceResolver(authInfo)) {
        
        Session session = resolver.adaptTo(Session.class);
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM [cq:PageContent] ");
        queryBuilder.append("WHERE ISDESCENDANTNODE('/content') ");
        
        List<String> conditions = new ArrayList<>();
        
        if (titleKeyword != null && !titleKeyword.isEmpty()) {
            conditions.add("CONTAINS(jcr:title, '" + 
                          escapeQuery(titleKeyword) + "')");
        }
        
        if (descriptionKeyword != null && !descriptionKeyword.isEmpty()) {
            conditions.add("CONTAINS(jcr:description, '" + 
                          escapeQuery(descriptionKeyword) + "')");
        }
        
        if (category != null && !category.isEmpty()) {
            conditions.add("productCategory = '" + 
                          escapeQuery(category) + "'");
        }
        
        if (!conditions.isEmpty()) {
            queryBuilder.append("AND ");
            queryBuilder.append(String.join(" AND ", conditions));
        }
        
        Query jcrQuery = queryManager.createQuery(
            queryBuilder.toString(), Query.JCR_SQL2);
        QueryResult result = jcrQuery.execute();
        
        // 处理结果...
        return processResults(result, resolver);
    }
}
```

### 5.4 使用 Groovy Console 快速查询

```groovy
// 在 AEM Groovy Console 中执行
// 访问: http://localhost:4502/groovyconsole

import javax.jcr.query.*

def session = resourceResolver.adaptTo(javax.jcr.Session.class)
def queryManager = session.workspace.queryManager

// 查询标题包含 "产品" 的页面
def query = """
    SELECT * FROM [cq:Page] 
    WHERE ISDESCENDANTNODE('/content') 
    AND CONTAINS(jcr:title, '产品')
"""

def jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2)
def result = jcrQuery.execute()

def count = 0
result.nodes.each { node ->
    println "找到页面: ${node.path}"
    count++
}

println "总共找到 ${count} 个页面"
```

---

## 6. 性能监控和调试

### 6.1 启用查询日志

**在 AEM 日志配置中启用**:

```
# log4j2.xml 或 slinglog.config
org.apache.jackrabbit.oak.query.level=DEBUG
```

**查看查询执行计划**:

```java
// 查询执行后，检查日志
// 会看到类似以下信息：
// Query: SELECT * FROM [cq:Page] WHERE ...
// Index used: lucene-content
// Execution time: 45ms
// Results: 23
```

### 6.2 测量查询性能

```java
public List<Page> searchWithTiming(String keyword) throws Exception {
    long startTime = System.currentTimeMillis();
    
    try (ResourceResolver resolver = 
            resourceResolverFactory.getServiceResourceResolver(authInfo)) {
        
        // 执行查询
        List<Page> results = executeSearch(resolver, keyword);
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("查询耗时: {}ms, 结果数量: {}", duration, results.size());
        
        return results;
    }
}
```

### 6.3 检查索引使用情况

```groovy
// 检查查询是否使用了索引
def session = resourceResolver.adaptTo(javax.jcr.Session.class)
def queryManager = session.workspace.queryManager

def query = """
    SELECT * FROM [cq:Page] 
    WHERE ISDESCENDANTNODE('/content') 
    AND CONTAINS(jcr:title, '产品')
"""

def jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2)

// 执行查询
def result = jcrQuery.execute()

// 检查执行计划（在日志中查看）
// 如果看到 "index used: lucene-content" 说明使用了索引
// 如果看到 "traversal" 说明没有使用索引，需要优化
```

---

## 7. 常见问题和解决方案

### 问题 1: 查询很慢

**原因**:
- 没有建立索引
- 查询范围太大
- 使用了无法使用索引的查询条件

**解决方案**:
1. 检查并创建适当的索引
2. 限制查询路径范围
3. 使用 CONTAINS 而不是 LIKE（如果使用全文索引）
4. 确保查询条件匹配索引配置

### 问题 2: 查询返回空结果

**原因**:
- 索引未重建
- 查询语法错误
- 属性名不正确

**解决方案**:
1. 触发索引重建
2. 检查查询语法
3. 验证属性名和节点类型

### 问题 3: 索引重建很慢

**原因**:
- 节点数量太多
- 索引配置不合理

**解决方案**:
1. 使用异步索引（`async: "async"`）
2. 限制索引范围（`includedPaths`, `excludedPaths`）
3. 在低峰期重建索引

### 问题 4: 查询结果不准确

**原因**:
- 索引未及时更新
- 查询条件不正确

**解决方案**:
1. 等待索引更新完成
2. 检查查询条件逻辑
3. 验证数据是否正确

---

## 8. 最佳实践总结

### 8.1 查询设计原则

1. **总是使用索引**: 确保查询条件匹配已建立的索引
2. **限制查询范围**: 使用 `ISDESCENDANTNODE` 限制路径
3. **使用具体节点类型**: 避免使用 `nt:base`
4. **限制结果数量**: 使用 `LIMIT` 避免返回过多结果
5. **避免函数调用**: 在 WHERE 子句中避免使用函数

### 8.2 索引设计原则

1. **按需创建索引**: 只为经常查询的属性创建索引
2. **使用异步索引**: 避免阻塞主操作
3. **限制索引范围**: 只索引需要的路径
4. **定期维护**: 监控索引状态，及时重建

### 8.3 性能优化检查清单

- [ ] 查询是否使用了索引？（检查日志）
- [ ] 查询范围是否合理？（限制路径）
- [ ] 是否限制了结果数量？（使用 LIMIT）
- [ ] 索引是否及时更新？（检查 reindex 状态）
- [ ] 查询条件是否最优？（避免函数调用）

---

## 9. 完整示例：页面搜索工具

```java
@Component(service = PageSearchService.class)
public class PageSearchService {
    
    private static final Logger LOG = LoggerFactory.getLogger(PageSearchService.class);
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    /**
     * 搜索页面
     * 
     * @param searchCriteria 搜索条件
     * @return 搜索结果
     */
    public SearchResult searchPages(SearchCriteria searchCriteria) throws Exception {
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "search-service");
        
        try (ResourceResolver resolver = 
                resourceResolverFactory.getServiceResourceResolver(authInfo)) {
            
            Session session = resolver.adaptTo(Session.class);
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            
            // 构建查询
            String query = buildQuery(searchCriteria);
            LOG.debug("执行查询: {}", query);
            
            Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
            QueryResult result = jcrQuery.execute();
            
            // 处理结果
            List<PageInfo> pages = processResults(result, resolver);
            
            long duration = System.currentTimeMillis() - startTime;
            LOG.info("查询完成，耗时: {}ms, 结果数: {}", duration, pages.size());
            
            return new SearchResult(pages, duration);
        }
    }
    
    private String buildQuery(SearchCriteria criteria) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM [cq:Page] ");
        query.append("WHERE ISDESCENDANTNODE('").append(criteria.getBasePath()).append("') ");
        
        List<String> conditions = new ArrayList<>();
        
        if (criteria.getTitleKeyword() != null) {
            conditions.add("CONTAINS(jcr:title, '" + 
                          escapeQuery(criteria.getTitleKeyword()) + "')");
        }
        
        if (criteria.getDescriptionKeyword() != null) {
            conditions.add("CONTAINS(jcr:description, '" + 
                          escapeQuery(criteria.getDescriptionKeyword()) + "')");
        }
        
        if (!conditions.isEmpty()) {
            query.append("AND ");
            query.append(String.join(" AND ", conditions));
        }
        
        if (criteria.getLimit() > 0) {
            query.append(" LIMIT ").append(criteria.getLimit());
        }
        
        return query.toString();
    }
    
    private List<PageInfo> processResults(QueryResult result, ResourceResolver resolver) 
            throws Exception {
        List<PageInfo> pages = new ArrayList<>();
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        
        NodeIterator nodes = result.getNodes();
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            Page page = pageManager.getPage(node.getPath());
            if (page != null) {
                pages.add(new PageInfo(page));
            }
        }
        
        return pages;
    }
    
    private String escapeQuery(String keyword) {
        return keyword.replace("'", "''")
                     .replace("\\", "\\\\");
    }
    
    // 搜索条件类
    public static class SearchCriteria {
        private String basePath = "/content";
        private String titleKeyword;
        private String descriptionKeyword;
        private int limit = 100;
        
        // Getters and Setters
        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
        
        public String getTitleKeyword() { return titleKeyword; }
        public void setTitleKeyword(String titleKeyword) { 
            this.titleKeyword = titleKeyword; 
        }
        
        public String getDescriptionKeyword() { return descriptionKeyword; }
        public void setDescriptionKeyword(String descriptionKeyword) { 
            this.descriptionKeyword = descriptionKeyword; 
        }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
    
    // 搜索结果类
    public static class SearchResult {
        private List<PageInfo> pages;
        private long executionTime;
        
        public SearchResult(List<PageInfo> pages, long executionTime) {
            this.pages = pages;
            this.executionTime = executionTime;
        }
        
        // Getters
        public List<PageInfo> getPages() { return pages; }
        public long getExecutionTime() { return executionTime; }
    }
    
    // 页面信息类
    public static class PageInfo {
        private String path;
        private String title;
        private Date lastModified;
        
        public PageInfo(Page page) {
            this.path = page.getPath();
            this.title = page.getTitle();
            this.lastModified = page.getLastModified().getTime();
        }
        
        // Getters
        public String getPath() { return path; }
        public String getTitle() { return title; }
        public Date getLastModified() { return lastModified; }
    }
}
```

---

## 10. 高级主题

### 10.1 聚合函数和统计查询

虽然 JCR SQL2 对聚合函数的支持有限，但在某些场景下仍然有用。

#### 10.1.1 COUNT 查询

**统计节点数量**:

```java
// 注意：Oak SQL2 不直接支持 COUNT(*)
// 需要通过遍历结果来计算

public long countPages(String basePath, String keyword) throws Exception {
    Map<String, Object> authInfo = new HashMap<>();
    authInfo.put(ResourceResolverFactory.SUBSERVICE, "search-service");
    
    try (ResourceResolver resolver = 
            resourceResolverFactory.getServiceResourceResolver(authInfo)) {
        
        Session session = resolver.adaptTo(Session.class);
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        
        String query = "SELECT * FROM [cq:Page] " +
                       "WHERE ISDESCENDANTNODE('" + basePath + "') " +
                       "AND CONTAINS(jcr:title, '" + escapeSQL2(keyword) + "')";
        
        Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
        QueryResult result = jcrQuery.execute();
        
        // 遍历结果计数（对于大量结果，考虑使用 LIMIT）
        long count = 0;
        NodeIterator nodes = result.getNodes();
        while (nodes.hasNext()) {
            nodes.nextNode();
            count++;
        }
        
        return count;
    }
}
```

**使用 LIMIT 优化计数**:

```java
// 如果只需要知道是否有结果，使用 LIMIT 1
public boolean hasPages(String basePath, String keyword) throws Exception {
    String query = "SELECT * FROM [cq:Page] " +
                   "WHERE ISDESCENDANTNODE('" + basePath + "') " +
                   "AND CONTAINS(jcr:title, '" + escapeSQL2(keyword) + "') " +
                   "LIMIT 1";
    
    QueryResult result = executeQuery(query);
    return result.getNodes().hasNext();
}
```

#### 10.1.2 分组统计

**按属性值分组统计**:

```java
// 统计每个状态下的页面数量
public Map<String, Long> countPagesByStatus(String basePath) throws Exception {
    Map<String, Long> statusCount = new HashMap<>();
    
    // 先获取所有不同的状态值
    String query = "SELECT * FROM [cq:PageContent] " +
                   "WHERE ISDESCENDANTNODE('" + basePath + "') " +
                   "AND status IS NOT NULL";
    
    QueryResult result = executeQuery(query);
    NodeIterator nodes = result.getNodes();
    
    while (nodes.hasNext()) {
        Node node = nodes.nextNode();
        if (node.hasProperty("status")) {
            String status = node.getProperty("status").getString();
            statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1);
        }
    }
    
    return statusCount;
}
```

**注意**: Oak SQL2 不支持 `GROUP BY`，需要在应用层实现分组逻辑。

### 10.2 查询超时和资源限制

#### 10.2.1 设置查询超时

```java
@Component(service = QueryService.class)
public class QueryService {
    
    private static final int QUERY_TIMEOUT_SECONDS = 30;
    
    public QueryResult executeQueryWithTimeout(String queryString) throws Exception {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "query-service");
        
        try (ResourceResolver resolver = 
                resourceResolverFactory.getServiceResourceResolver(authInfo)) {
            
            Session session = resolver.adaptTo(Session.class);
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            
            // 设置查询超时（秒）
            // 注意：某些 AEM 版本可能不支持此方法
            try {
                // 使用反射设置超时（如果支持）
                Method setTimeoutMethod = query.getClass().getMethod("setLimit", long.class);
                // 或者使用其他超时机制
            } catch (Exception e) {
                // 不支持超时设置
            }
            
            // 使用线程和 Future 实现超时
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<QueryResult> future = executor.submit(() -> query.execute());
            
            try {
                return future.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new QueryTimeoutException("查询超时: " + QUERY_TIMEOUT_SECONDS + " 秒");
            } finally {
                executor.shutdown();
            }
        }
    }
}
```

#### 10.2.2 限制查询结果数量

```java
// 始终使用 LIMIT 避免返回过多结果
public List<String> searchPages(String keyword, int maxResults) throws Exception {
    String query = "SELECT * FROM [cq:Page] " +
                   "WHERE ISDESCENDANTNODE('/content') " +
                   "AND CONTAINS(jcr:title, '" + escapeSQL2(keyword) + "') " +
                   "LIMIT " + Math.min(maxResults, 1000); // 限制最大 1000 条
    
    QueryResult result = executeQuery(query);
    List<String> pagePaths = new ArrayList<>();
    
    NodeIterator nodes = result.getNodes();
    int count = 0;
    while (nodes.hasNext() && count < maxResults) {
        pagePaths.add(nodes.nextNode().getPath());
        count++;
    }
    
    return pagePaths;
}
```

#### 10.2.3 内存管理

**流式处理大量结果**:

```java
public void processLargeResultSet(String query, ResultProcessor processor) 
        throws Exception {
    
    QueryResult result = executeQuery(query);
    NodeIterator nodes = result.getNodes();
    
    int batchSize = 100;
    int count = 0;
    List<Node> batch = new ArrayList<>();
    
    while (nodes.hasNext()) {
        batch.add(nodes.nextNode());
        count++;
        
        // 批量处理
        if (batch.size() >= batchSize) {
            processor.processBatch(batch);
            batch.clear();
            
            // 可选：强制垃圾回收（谨慎使用）
            if (count % 1000 == 0) {
                System.gc();
            }
        }
    }
    
    // 处理剩余结果
    if (!batch.isEmpty()) {
        processor.processBatch(batch);
    }
    
    LOG.info("处理完成，共处理 {} 个节点", count);
}

public interface ResultProcessor {
    void processBatch(List<Node> nodes) throws Exception;
}
```

### 10.3 批量查询优化

#### 10.3.1 批量查询多个关键词

**方法 1: 使用 OR 条件（适合少量关键词）**:

```java
public List<String> searchMultipleKeywords(List<String> keywords, int limit) 
        throws Exception {
    
    if (keywords.isEmpty()) {
        return Collections.emptyList();
    }
    
    // 构建 OR 条件
    StringBuilder orConditions = new StringBuilder();
    for (int i = 0; i < keywords.size(); i++) {
        if (i > 0) {
            orConditions.append(" OR ");
        }
        orConditions.append("CONTAINS(jcr:title, '")
                    .append(escapeSQL2(keywords.get(i)))
                    .append("')");
    }
    
    String query = "SELECT * FROM [cq:Page] " +
                   "WHERE ISDESCENDANTNODE('/content') " +
                   "AND (" + orConditions.toString() + ") " +
                   "LIMIT " + limit;
    
    return executeQueryAndGetPaths(query);
}
```

**方法 2: 分别查询后合并（适合大量关键词）**:

```java
public List<String> searchMultipleKeywordsParallel(List<String> keywords, int limit) 
        throws Exception {
    
    ExecutorService executor = Executors.newFixedThreadPool(5);
    List<Future<List<String>>> futures = new ArrayList<>();
    
    // 并行查询每个关键词
    for (String keyword : keywords) {
        Future<List<String>> future = executor.submit(() -> 
            searchPages(keyword, limit / keywords.size())
        );
        futures.add(future);
    }
    
    // 合并结果
    Set<String> uniquePaths = new LinkedHashSet<>();
    for (Future<List<String>> future : futures) {
        try {
            uniquePaths.addAll(future.get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            LOG.error("查询失败", e);
        }
    }
    
    executor.shutdown();
    
    // 限制最终结果数量
    return uniquePaths.stream()
                     .limit(limit)
                     .collect(Collectors.toList());
}
```

#### 10.3.2 查询结果去重

```java
// 使用 Set 自动去重
public List<String> searchWithDeduplication(String keyword) throws Exception {
    String query = "SELECT * FROM [cq:Page] " +
                   "WHERE ISDESCENDANTNODE('/content') " +
                   "AND (CONTAINS(jcr:title, '" + escapeSQL2(keyword) + "') " +
                   "     OR CONTAINS(jcr:description, '" + escapeSQL2(keyword) + "'))";
    
    QueryResult result = executeQuery(query);
    Set<String> uniquePaths = new LinkedHashSet<>(); // 保持顺序
    
    NodeIterator nodes = result.getNodes();
    while (nodes.hasNext()) {
        uniquePaths.add(nodes.nextNode().getPath());
    }
    
    return new ArrayList<>(uniquePaths);
}
```

### 10.4 查询结果缓存

#### 10.4.1 简单缓存实现

```java
@Component(service = CachedSearchService.class)
public class CachedSearchService {
    
    private final Cache<String, List<String>> queryCache;
    
    public CachedSearchService() {
        // 使用 Caffeine 缓存（需要添加依赖）
        this.queryCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    }
    
    public List<String> searchWithCache(String keyword, String basePath) 
            throws Exception {
        
        String cacheKey = basePath + ":" + keyword;
        
        // 尝试从缓存获取
        List<String> cached = queryCache.getIfPresent(cacheKey);
        if (cached != null) {
            LOG.debug("缓存命中: {}", cacheKey);
            return new ArrayList<>(cached); // 返回副本
        }
        
        // 缓存未命中，执行查询
        List<String> results = executeSearch(keyword, basePath);
        
        // 存入缓存
        queryCache.put(cacheKey, new ArrayList<>(results));
        
        return results;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        queryCache.invalidateAll();
    }
    
    /**
     * 清除特定路径的缓存
     */
    public void clearCacheForPath(String basePath) {
        queryCache.asMap().keySet().removeIf(key -> key.startsWith(basePath + ":"));
    }
}
```

#### 10.4.2 使用 AEM 的缓存机制

```java
@Component(service = AemCachedSearchService.class)
public class AemCachedSearchService {
    
    @Reference
    private CacheManager cacheManager;
    
    private Cache<String, List<String>> queryCache;
    
    @Activate
    protected void activate() {
        // 使用 AEM 的缓存管理器
        queryCache = cacheManager.getCache("query-results");
    }
    
    public List<String> searchWithAemCache(String keyword, String basePath) 
            throws Exception {
        
        String cacheKey = basePath + ":" + keyword;
        
        // 从 AEM 缓存获取
        List<String> cached = queryCache.get(cacheKey, () -> {
            try {
                return executeSearch(keyword, basePath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        return new ArrayList<>(cached);
    }
}
```

**缓存失效策略**:
- 时间过期：5-10 分钟
- 事件驱动：内容更新时清除相关缓存
- 手动清除：管理员操作

### 10.5 性能基准测试数据

#### 10.5.1 典型性能指标

**测试环境**: AEM 6.5, Oak 1.22, 100,000 个页面节点

| 查询类型 | 无索引 | Property Index | Lucene Index | 性能提升 |
|---------|--------|----------------|--------------|---------|
| **精确匹配** (`jcr:title = '值'`) | 15-30 秒 | 20-50ms | 50-100ms | 300-1500x |
| **全文搜索** (`CONTAINS(jcr:title, '关键词')`) | 20-40 秒 | 不支持 | 30-80ms | 250-1300x |
| **范围查询** (`jcr:created > date`) | 18-35 秒 | 25-60ms | 不支持 | 300-1400x |
| **多条件 AND** | 25-50 秒 | 40-100ms | 60-150ms | 250-1250x |
| **多条件 OR** | 30-60 秒 | 50-120ms | 80-200ms | 250-1200x |

**注意**: 实际性能取决于：
- 节点数量
- 索引配置
- 服务器负载
- 查询复杂度

#### 10.5.2 索引重建时间参考

| 节点数量 | Property Index | Lucene Index |
|---------|----------------|--------------|
| 10,000 | 5-15 秒 | 30-60 秒 |
| 100,000 | 30-90 秒 | 5-15 分钟 |
| 1,000,000 | 5-15 分钟 | 30-60 分钟 |

**优化建议**:
- 使用 `includedPaths` 限制索引范围
- 在低峰期重建索引
- 使用异步索引（`async: "async"`）

### 10.6 查询结果分页最佳实践

#### 10.6.1 基于 OFFSET 的分页

```java
public class PaginatedSearchResult {
    private List<String> results;
    private int totalCount;
    private int pageNumber;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    
    // Getters and Setters
}

public PaginatedSearchResult searchWithPagination(
        String keyword, 
        String basePath, 
        int pageNumber, 
        int pageSize) throws Exception {
    
    // 限制最大页面大小
    pageSize = Math.min(pageSize, 100);
    int offset = (pageNumber - 1) * pageSize;
    
    // 查询当前页
    String query = "SELECT * FROM [cq:Page] " +
                   "WHERE ISDESCENDANTNODE('" + basePath + "') " +
                   "AND CONTAINS(jcr:title, '" + escapeSQL2(keyword) + "') " +
                   "ORDER BY jcr:lastModified DESC " +
                   "LIMIT " + (pageSize + 1) + " OFFSET " + offset; // +1 用于判断是否有下一页
    
    QueryResult result = executeQuery(query);
    NodeIterator nodes = result.getNodes();
    
    List<String> pageResults = new ArrayList<>();
    int count = 0;
    boolean hasNext = false;
    
    while (nodes.hasNext() && count < pageSize) {
        pageResults.add(nodes.nextNode().getPath());
        count++;
    }
    
    // 检查是否有下一页
    if (nodes.hasNext()) {
        hasNext = true;
    }
    
    PaginatedSearchResult paginatedResult = new PaginatedSearchResult();
    paginatedResult.setResults(pageResults);
    paginatedResult.setPageNumber(pageNumber);
    paginatedResult.setPageSize(pageSize);
    paginatedResult.setHasNext(hasNext);
    paginatedResult.setHasPrevious(pageNumber > 1);
    
    // 注意：获取总数需要额外查询，性能开销大
    // 对于大量结果，考虑不提供总数，只提供"是否有下一页"
    
    return paginatedResult;
}
```

#### 10.6.2 基于游标的分页（推荐）

```java
// 使用 jcr:path 作为游标（更高效）
public PaginatedSearchResult searchWithCursor(
        String keyword, 
        String basePath, 
        String cursor,  // 上一页最后一个节点的路径
        int pageSize) throws Exception {
    
    pageSize = Math.min(pageSize, 100);
    
    String query;
    if (cursor == null || cursor.isEmpty()) {
        // 第一页
        query = "SELECT * FROM [cq:Page] " +
                "WHERE ISDESCENDANTNODE('" + basePath + "') " +
                "AND CONTAINS(jcr:title, '" + escapeSQL2(keyword) + "') " +
                "ORDER BY jcr:path ASC " +
                "LIMIT " + (pageSize + 1);
    } else {
        // 后续页：从游标位置开始
        query = "SELECT * FROM [cq:Page] " +
                "WHERE ISDESCENDANTNODE('" + basePath + "') " +
                "AND CONTAINS(jcr:title, '" + escapeSQL2(keyword) + "') " +
                "AND jcr:path > '" + escapeSQL2(cursor) + "' " +
                "ORDER BY jcr:path ASC " +
                "LIMIT " + (pageSize + 1);
    }
    
    QueryResult result = executeQuery(query);
    NodeIterator nodes = result.getNodes();
    
    List<String> pageResults = new ArrayList<>();
    String nextCursor = null;
    int count = 0;
    
    while (nodes.hasNext() && count < pageSize) {
        Node node = nodes.nextNode();
        pageResults.add(node.getPath());
        nextCursor = node.getPath(); // 更新游标
        count++;
    }
    
    boolean hasNext = nodes.hasNext();
    
    PaginatedSearchResult paginatedResult = new PaginatedSearchResult();
    paginatedResult.setResults(pageResults);
    paginatedResult.setPageSize(pageSize);
    paginatedResult.setHasNext(hasNext);
    paginatedResult.setNextCursor(hasNext ? nextCursor : null);
    
    return paginatedResult;
}
```

**游标分页的优势**:
- ✅ 性能更好（不需要 OFFSET）
- ✅ 结果一致性更好（不受数据变化影响）
- ✅ 适合大数据量场景

### 10.7 查询监控和告警

#### 10.7.1 慢查询监控

```java
@Component(service = QueryMonitorService.class)
public class QueryMonitorService {
    
    private static final Logger LOG = LoggerFactory.getLogger(QueryMonitorService.class);
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000; // 1 秒
    
    public QueryResult executeWithMonitoring(String queryString) throws Exception {
        long startTime = System.currentTimeMillis();
        
        try {
            QueryResult result = executeQuery(queryString);
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录慢查询
            if (duration > SLOW_QUERY_THRESHOLD_MS) {
                LOG.warn("慢查询检测: 耗时 {}ms, 查询: {}", duration, queryString);
                
                // 可选：发送告警
                sendAlert("慢查询", queryString, duration);
            }
            
            // 记录查询统计
            recordQueryMetrics(queryString, duration, result.getNodes().getSize());
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LOG.error("查询失败: 耗时 {}ms, 查询: {}", duration, queryString, e);
            throw e;
        }
    }
    
    private void recordQueryMetrics(String query, long duration, long resultCount) {
        // 记录到指标系统（如 Prometheus, JMX）
        // 用于分析和优化
    }
    
    private void sendAlert(String type, String query, long duration) {
        // 发送告警（如邮件、Slack、PagerDuty）
    }
}
```

#### 10.7.2 查询统计报告

```java
public class QueryStatistics {
    private String queryPattern; // 查询模式（参数化后）
    private long executionCount;
    private long totalDuration;
    private long minDuration;
    private long maxDuration;
    private long totalResults;
    
    public double getAverageDuration() {
        return executionCount > 0 ? (double) totalDuration / executionCount : 0;
    }
    
    public double getAverageResults() {
        return executionCount > 0 ? (double) totalResults / executionCount : 0;
    }
    
    // Getters and Setters
}

@Component(service = QueryStatisticsService.class)
public class QueryStatisticsService {
    
    private final Map<String, QueryStatistics> statistics = new ConcurrentHashMap<>();
    
    public void recordQuery(String queryPattern, long duration, long resultCount) {
        QueryStatistics stats = statistics.computeIfAbsent(
            queryPattern, 
            k -> new QueryStatistics()
        );
        
        stats.setQueryPattern(queryPattern);
        stats.setExecutionCount(stats.getExecutionCount() + 1);
        stats.setTotalDuration(stats.getTotalDuration() + duration);
        stats.setMinDuration(Math.min(stats.getMinDuration(), duration));
        stats.setMaxDuration(Math.max(stats.getMaxDuration(), duration));
        stats.setTotalResults(stats.getTotalResults() + resultCount);
    }
    
    public List<QueryStatistics> getTopSlowQueries(int limit) {
        return statistics.values().stream()
            .sorted((a, b) -> Long.compare(b.getAverageDuration(), a.getAverageDuration()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    public void generateReport() {
        LOG.info("=== 查询统计报告 ===");
        LOG.info("总查询数: {}", statistics.size());
        
        List<QueryStatistics> slowQueries = getTopSlowQueries(10);
        LOG.info("最慢的 10 个查询:");
        for (QueryStatistics stats : slowQueries) {
            LOG.info("  查询: {}", stats.getQueryPattern());
            LOG.info("    执行次数: {}", stats.getExecutionCount());
            LOG.info("    平均耗时: {}ms", stats.getAverageDuration());
            LOG.info("    最大耗时: {}ms", stats.getMaxDuration());
        }
    }
}
```

## 11. 总结

在 AEM 的 `/content` 下快速查找页面节点的关键点：

1. **使用 JCR 查询**: 不要遍历所有节点，使用 SQL2 或 XPath 查询
2. **建立索引**: 为经常查询的属性创建索引（Property Index 或 Fulltext Index）
3. **优化查询**: 限制查询范围、使用具体节点类型、避免函数调用
4. **监控性能**: 检查查询是否使用了索引，测量查询耗时
5. **定期维护**: 监控索引状态，及时重建索引

通过遵循这些最佳实践，可以在包含大量节点的 `/content` 目录下实现快速、高效的搜索。

