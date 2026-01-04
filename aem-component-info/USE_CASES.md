# 使用场景集合

## 实际应用场景和解决方案

本文档收集了各种实际使用场景，展示如何在真实项目中使用组件信息提取工具。

---

## 场景 1: 组件迁移准备

### 需求
将 AEM 组件迁移到 React，需要了解每个组件的完整配置。

### 解决方案

```java
@Component(service = ComponentMigrationService.class)
public class ComponentMigrationService {

    @Reference
    private SlingRepository repository;

    public void prepareMigration(String basePath) {
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            ComponentExporter exporter = new ComponentExporter();
            
            // 提取所有组件
            List<Map<String, Object>> components = 
                extractor.extractComponentsFromPath(basePath);
            
            // 导出为 JSON
            exporter.exportComponentsToJson(components, "migration/components");
            
            // 为每个组件生成 React 建议
            for (Map<String, Object> component : components) {
                String componentName = extractComponentName(component);
                exporter.exportReactComponentSuggestions(
                    component, 
                    "migration/react/" + componentName + ".md"
                );
            }
            
        } finally {
            session.logout();
        }
    }
}
```

---

## 场景 2: 组件文档自动生成

### 需求
自动为所有组件生成文档。

### 解决方案

```java
public class ComponentDocumentationGenerator {

    public void generateDocumentation(String basePath) {
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            List<Map<String, Object>> components = 
                extractor.extractComponentsFromPath(basePath);
            
            for (Map<String, Object> component : components) {
                generateMarkdownDocumentation(component);
            }
            
        } finally {
            session.logout();
        }
    }
    
    private void generateMarkdownDocumentation(Map<String, Object> componentInfo) {
        @SuppressWarnings("unchecked")
        Map<String, String> basic = (Map<String, String>) componentInfo.get("basicProperties");
        
        StringBuilder doc = new StringBuilder();
        doc.append("# ").append(basic.get("jcr:title")).append("\n\n");
        doc.append("**资源类型:** `").append(basic.get("sling:resourceType")).append("`\n\n");
        doc.append("## 描述\n\n").append(basic.get("jcr:description")).append("\n\n");
        
        // 添加对话框字段文档
        @SuppressWarnings("unchecked")
        Map<String, Object> dialog = (Map<String, Object>) componentInfo.get("dialog");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) dialog.get("fields");
        
        if (fields != null && !fields.isEmpty()) {
            doc.append("## 属性\n\n");
            doc.append("| 名称 | 类型 | 必填 | 说明 |\n");
            doc.append("|------|------|------|------|\n");
            
            for (Map<String, Object> field : fields) {
                @SuppressWarnings("unchecked")
                Map<String, String> props = (Map<String, String>) field.get("properties");
                String name = props.get("name");
                String label = props.get("fieldLabel");
                Boolean required = (Boolean) field.get("required");
                String nodeType = (String) field.get("nodeType");
                
                doc.append("| ").append(name).append(" | ")
                    .append(extractType(nodeType)).append(" | ")
                    .append(required != null && required ? "是" : "否").append(" | ")
                    .append(label != null ? label : "").append(" |\n");
            }
        }
        
        // 写入文件
        String fileName = basic.get("componentName") + ".md";
        writeFile("docs/components/" + fileName, doc.toString());
    }
}
```

---

## 场景 3: 组件使用情况审计

### 需求
分析项目中哪些组件被使用，使用频率如何。

### 解决方案

```java
public class ComponentUsageAuditor {

    public Map<String, ComponentUsageStats> auditComponentUsage(String basePath) {
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            List<Map<String, Object>> components = 
                extractor.extractComponentsFromPath(basePath);
            
            Map<String, ComponentUsageStats> statsMap = new HashMap<>();
            
            for (Map<String, Object> component : components) {
                @SuppressWarnings("unchecked")
                Map<String, String> basic = (Map<String, String>) component.get("basicProperties");
                String resourceType = basic.get("sling:resourceType");
                
                // 查找使用该组件的页面
                List<Node> pages = ComponentQueryUtil.findPagesUsingComponent(
                    session, resourceType
                );
                
                ComponentUsageStats stats = new ComponentUsageStats();
                stats.resourceType = resourceType;
                stats.componentName = basic.get("jcr:title");
                stats.usageCount = pages.size();
                stats.pages = pages.stream()
                    .map(this::extractPagePath)
                    .collect(Collectors.toList());
                
                statsMap.put(resourceType, stats);
            }
            
            // 生成报告
            generateUsageReport(statsMap);
            
            return statsMap;
            
        } finally {
            session.logout();
        }
    }
    
    private void generateUsageReport(Map<String, ComponentUsageStats> stats) {
        // 按使用频率排序
        List<ComponentUsageStats> sorted = stats.values().stream()
            .sorted((a, b) -> Integer.compare(b.usageCount, a.usageCount))
            .collect(Collectors.toList());
        
        // 生成报告
        StringBuilder report = new StringBuilder();
        report.append("# 组件使用情况报告\n\n");
        report.append("| 组件 | 使用次数 |\n");
        report.append("|------|----------|\n");
        
        for (ComponentUsageStats stat : sorted) {
            report.append("| ").append(stat.componentName)
                  .append(" | ").append(stat.usageCount).append(" |\n");
        }
        
        writeFile("reports/component-usage.md", report.toString());
    }
}
```

---

## 场景 4: 组件配置合规性检查

### 需求
检查所有组件是否符合项目规范（如有对话框、有文档等）。

### 解决方案

```java
public class ComponentComplianceChecker {

    public ComplianceReport checkCompliance(String basePath) {
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            List<Map<String, Object>> components = 
                extractor.extractComponentsFromPath(basePath);
            
            ComplianceReport report = new ComplianceReport();
            
            for (Map<String, Object> component : components) {
                @SuppressWarnings("unchecked")
                Map<String, String> basic = (Map<String, String>) component.get("basicProperties");
                String componentName = basic.get("jcr:title");
                
                ComponentCompliance compliance = new ComponentCompliance();
                compliance.componentName = componentName;
                compliance.resourceType = basic.get("sling:resourceType");
                
                // 检查是否有对话框
                @SuppressWarnings("unchecked")
                Map<String, Object> dialog = (Map<String, Object>) component.get("dialog");
                compliance.hasDialog = !"none".equals(dialog.get("type"));
                
                // 检查是否有描述
                compliance.hasDescription = 
                    basic.get("jcr:description") != null && 
                    !basic.get("jcr:description").isEmpty();
                
                // 检查是否有组件分组
                compliance.hasComponentGroup = 
                    basic.get("componentGroup") != null &&
                    !basic.get("componentGroup").isEmpty();
                
                // 计算合规分数
                compliance.score = calculateScore(compliance);
                
                report.components.add(compliance);
            }
            
            // 生成报告
            generateComplianceReport(report);
            
            return report;
            
        } finally {
            session.logout();
        }
    }
    
    private int calculateScore(ComponentCompliance compliance) {
        int score = 0;
        if (compliance.hasDialog) score += 30;
        if (compliance.hasDescription) score += 30;
        if (compliance.hasComponentGroup) score += 40;
        return score;
    }
}
```

---

## 场景 5: 组件版本对比

### 需求
对比组件不同版本的差异，了解配置变化。

### 解决方案

```java
public class ComponentVersionComparator {

    public void compareVersions(String v1Path, String v2Path) {
        Session session = repository.loginAdministrative(null);
        try {
            ComponentComparator comparator = new ComponentComparator(session);
            Map<String, Object> comparison = comparator.compareComponents(v1Path, v2Path);
            
            // 导出对比结果
            ComponentExporter exporter = new ComponentExporter();
            exporter.exportComponentToJson(comparison, "comparison/v1-vs-v2.json");
            
            // 生成对比报告
            generateComparisonReport(comparison);
            
        } finally {
            session.logout();
        }
    }
    
    private void generateComparisonReport(Map<String, Object> comparison) {
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) comparison.get("summary");
        boolean identical = (Boolean) summary.get("componentsIdentical");
        
        if (identical) {
            System.out.println("组件配置完全相同");
        } else {
            // 列出差异
            @SuppressWarnings("unchecked")
            Map<String, Object> basicDiff = (Map<String, Object>) comparison.get("basicProperties");
            @SuppressWarnings("unchecked")
            List<String> differences = (List<String>) basicDiff.get("differences");
            
            System.out.println("发现 " + differences.size() + " 处差异：");
            for (String diff : differences) {
                System.out.println("  - " + diff);
            }
        }
    }
}
```

---

## 场景 6: 组件依赖关系分析

### 需求
分析组件之间的依赖关系，构建依赖图。

### 解决方案

```java
public class ComponentDependencyAnalyzer {

    public DependencyGraph analyzeDependencies(String basePath) {
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            List<Map<String, Object>> components = 
                extractor.extractComponentsFromPath(basePath);
            
            DependencyGraph graph = new DependencyGraph();
            
            for (Map<String, Object> component : components) {
                @SuppressWarnings("unchecked")
                Map<String, String> basic = (Map<String, String>) component.get("basicProperties");
                String resourceType = basic.get("sling:resourceType");
                String superType = basic.get("sling:resourceSuperType");
                
                if (superType != null && !superType.isEmpty()) {
                    graph.addDependency(resourceType, superType);
                }
            }
            
            // 检测循环依赖
            detectCircularDependencies(graph);
            
            // 生成依赖图
            generateDependencyGraph(graph);
            
            return graph;
            
        } finally {
            session.logout();
        }
    }
    
    private void generateDependencyGraph(DependencyGraph graph) {
        // 使用 Graphviz 或其他工具生成可视化图表
        StringBuilder dot = new StringBuilder();
        dot.append("digraph ComponentDependencies {\n");
        
        for (Map.Entry<String, List<String>> entry : graph.getEdges().entrySet()) {
            String from = entry.getKey();
            for (String to : entry.getValue()) {
                dot.append("  \"").append(from).append("\" -> \"").append(to).append("\"\n");
            }
        }
        
        dot.append("}\n");
        
        writeFile("reports/dependency-graph.dot", dot.toString());
    }
}
```

---

## 场景 7: 批量组件重构辅助

### 需求
批量修改组件配置，需要先了解当前配置。

### 解决方案

```java
public class ComponentRefactoringHelper {

    public void analyzeBeforeRefactoring(String basePath) {
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            ComponentExporter exporter = new ComponentExporter();
            
            // 提取当前配置
            List<Map<String, Object>> components = 
                extractor.extractComponentsFromPath(basePath);
            
            // 导出为备份
            exporter.exportComponentsToJson(components, "backup/before-refactoring");
            
            // 分析需要修改的组件
            List<String> componentsToUpdate = identifyComponentsToUpdate(components);
            
            // 生成重构计划
            generateRefactoringPlan(componentsToUpdate);
            
        } finally {
            session.logout();
        }
    }
    
    private List<String> identifyComponentsToUpdate(List<Map<String, Object>> components) {
        List<String> toUpdate = new ArrayList<>();
        
        for (Map<String, Object> component : components) {
            @SuppressWarnings("unchecked")
            Map<String, String> basic = (Map<String, String>) component.get("basicProperties");
            
            // 例如：找出所有没有对话框的组件
            @SuppressWarnings("unchecked")
            Map<String, Object> dialog = (Map<String, Object>) component.get("dialog");
            if ("none".equals(dialog.get("type"))) {
                toUpdate.add(basic.get("sling:resourceType"));
            }
        }
        
        return toUpdate;
    }
}
```

---

## 场景 8: API 文档生成

### 需求
为组件生成 API 文档，包含所有可配置属性。

### 解决方案

```java
public class ComponentAPIDocumentationGenerator {

    public void generateAPIDocs(String basePath) {
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            List<Map<String, Object>> components = 
                extractor.extractComponentsFromPath(basePath);
            
            for (Map<String, Object> component : components) {
                generateAPI DocForComponent(component);
            }
            
        } finally {
            session.logout();
        }
    }
    
    private void generateAPIDocForComponent(Map<String, Object> componentInfo) {
        @SuppressWarnings("unchecked")
        Map<String, String> basic = (Map<String, String>) componentInfo.get("basicProperties");
        
        // 生成 OpenAPI/Swagger 文档
        StringBuilder apiDoc = new StringBuilder();
        apiDoc.append("openapi: 3.0.0\n");
        apiDoc.append("info:\n");
        apiDoc.append("  title: ").append(basic.get("jcr:title")).append("\n");
        apiDoc.append("  description: ").append(basic.get("jcr:description")).append("\n");
        
        // 添加属性定义
        @SuppressWarnings("unchecked")
        Map<String, Object> dialog = (Map<String, Object>) componentInfo.get("dialog");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) dialog.get("fields");
        
        // ... 生成属性定义 ...
        
        writeFile("api-docs/" + basic.get("componentName") + ".yaml", apiDoc.toString());
    }
}
```

---

## 总结

这些场景展示了组件信息提取工具在实际项目中的各种应用：

1. ✅ 组件迁移准备
2. ✅ 文档自动生成
3. ✅ 使用情况审计
4. ✅ 合规性检查
5. ✅ 版本对比
6. ✅ 依赖关系分析
7. ✅ 批量重构辅助
8. ✅ API 文档生成

你可以根据项目需求，组合使用这些场景，或者基于这些示例创建自己的解决方案！

