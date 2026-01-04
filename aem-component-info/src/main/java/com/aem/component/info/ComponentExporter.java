package com.aem.component.info;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 组件导出器
 * 
 * 将提取的组件信息导出为 JSON 格式，方便后续处理和 React 重构。
 * 
 * 使用示例：
 * <pre>
 * ComponentExporter exporter = new ComponentExporter();
 * exporter.exportComponentToJson(componentInfo, "/path/to/output.json");
 * </pre>
 */
public class ComponentExporter {

    private static final Logger log = LoggerFactory.getLogger(ComponentExporter.class);
    
    private final ObjectMapper objectMapper;

    public ComponentExporter() {
        this.objectMapper = new ObjectMapper();
        // 格式化 JSON 输出，便于阅读
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // 处理空值
        this.objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    }

    /**
     * 导出单个组件信息为 JSON 文件
     * 
     * @param componentInfo 组件信息 Map
     * @param outputPath 输出文件路径
     * @throws IOException 文件写入错误
     */
    public void exportComponentToJson(Map<String, Object> componentInfo, String outputPath) 
            throws IOException {
        
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        
        // 创建父目录（如果不存在）
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // 写入 JSON 文件
        objectMapper.writeValue(outputFile, componentInfo);
        log.info("组件信息已导出到: " + outputPath);
    }

    /**
     * 导出组件信息为 JSON 字符串
     * 
     * @param componentInfo 组件信息 Map
     * @return JSON 字符串
     * @throws IOException 序列化错误
     */
    public String exportComponentToJsonString(Map<String, Object> componentInfo) 
            throws IOException {
        return objectMapper.writeValueAsString(componentInfo);
    }

    /**
     * 批量导出多个组件信息
     * 
     * @param componentsInfo 组件信息列表
     * @param outputDirectory 输出目录
     * @throws IOException 文件写入错误
     */
    public void exportComponentsToJson(List<Map<String, Object>> componentsInfo, 
                                      String outputDirectory) throws IOException {
        
        // 创建输出目录
        Files.createDirectories(Paths.get(outputDirectory));
        
        int exportedCount = 0;
        for (Map<String, Object> componentInfo : componentsInfo) {
            try {
                // 从组件信息中获取组件名称
                String componentName = extractComponentName(componentInfo);
                String outputPath = outputDirectory + File.separator + componentName + ".json";
                
                exportComponentToJson(componentInfo, outputPath);
                exportedCount++;
                
            } catch (Exception e) {
                log.error("导出组件时出错: " + e.getMessage(), e);
            }
        }
        
        log.info("成功导出 " + exportedCount + " 个组件到目录: " + outputDirectory);
        
        // 导出索引文件
        exportIndexFile(componentsInfo, outputDirectory);
    }

    /**
     * 导出索引文件（列出所有导出的组件）
     * 
     * @param componentsInfo 组件信息列表
     * @param outputDirectory 输出目录
     * @throws IOException 文件写入错误
     */
    private void exportIndexFile(List<Map<String, Object>> componentsInfo, 
                                 String outputDirectory) throws IOException {
        
        // 创建索引数据结构
        Map<String, Object> index = new java.util.HashMap<>();
        index.put("exportedAt", System.currentTimeMillis());
        index.put("totalComponents", componentsInfo.size());
        
        // 组件列表（简化信息）
        List<Map<String, String>> componentList = new java.util.ArrayList<>();
        for (Map<String, Object> componentInfo : componentsInfo) {
            Map<String, String> componentSummary = new java.util.HashMap<>();
            
            @SuppressWarnings("unchecked")
            Map<String, String> basicProperties = 
                (Map<String, String>) componentInfo.get("basicProperties");
            
            if (basicProperties != null) {
                componentSummary.put("name", basicProperties.get("componentName"));
                componentSummary.put("title", basicProperties.get("jcr:title"));
                componentSummary.put("resourceType", basicProperties.get("sling:resourceType"));
                componentSummary.put("path", basicProperties.get("componentPath"));
                componentSummary.put("group", basicProperties.get("componentGroup"));
                componentSummary.put("file", extractComponentName(componentInfo) + ".json");
            }
            
            componentList.add(componentSummary);
        }
        
        index.put("components", componentList);
        
        // 写入索引文件
        String indexPath = outputDirectory + File.separator + "index.json";
        objectMapper.writeValue(new File(indexPath), index);
        log.info("索引文件已导出到: " + indexPath);
    }

    /**
     * 从组件信息中提取组件名称（用于文件名）
     * 
     * @param componentInfo 组件信息
     * @return 组件名称
     */
    private String extractComponentName(Map<String, Object> componentInfo) {
        // 尝试从不同位置获取组件名称
        @SuppressWarnings("unchecked")
        Map<String, String> basicProperties = 
            (Map<String, String>) componentInfo.get("basicProperties");
        
        if (basicProperties != null) {
            String name = basicProperties.get("componentName");
            if (name != null && !name.isEmpty()) {
                return sanitizeFileName(name);
            }
            
            String resourceType = basicProperties.get("sling:resourceType");
            if (resourceType != null && !resourceType.isEmpty()) {
                // 从 resourceType 中提取组件名（最后一个路径段）
                String[] parts = resourceType.split("/");
                return sanitizeFileName(parts[parts.length - 1]);
            }
        }
        
        // 如果都获取不到，使用时间戳
        return "component_" + System.currentTimeMillis();
    }

    /**
     * 清理文件名，移除不安全的字符
     * 
     * @param fileName 原始文件名
     * @return 清理后的文件名
     */
    private String sanitizeFileName(String fileName) {
        // 移除路径分隔符和其他不安全字符
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * 导出为 React 组件结构建议（Markdown 格式）
     * 
     * 根据提取的组件信息，生成 React 重构建议文档。
     * 
     * @param componentInfo 组件信息
     * @param outputPath 输出文件路径
     * @throws IOException 文件写入错误
     */
    public void exportReactComponentSuggestions(Map<String, Object> componentInfo, 
                                                String outputPath) throws IOException {
        
        StringBuilder markdown = new StringBuilder();
        
        @SuppressWarnings("unchecked")
        Map<String, String> basicProperties = 
            (Map<String, String>) componentInfo.get("basicProperties");
        
        if (basicProperties != null) {
            String title = basicProperties.get("jcr:title");
            String description = basicProperties.get("jcr:description");
            String resourceType = basicProperties.get("sling:resourceType");
            
            markdown.append("# ").append(title != null ? title : "Component").append("\n\n");
            markdown.append("**Resource Type:** `").append(resourceType).append("`\n\n");
            
            if (description != null && !description.isEmpty()) {
                markdown.append("**Description:** ").append(description).append("\n\n");
            }
            
            markdown.append("## React 组件建议\n\n");
            
            // 提取对话框字段，生成 Props 接口建议
            @SuppressWarnings("unchecked")
            Map<String, Object> dialogInfo = (Map<String, Object>) componentInfo.get("dialog");
            
            if (dialogInfo != null && !"none".equals(dialogInfo.get("type"))) {
                markdown.append("### Props 接口\n\n");
                markdown.append("```typescript\n");
                markdown.append("interface ComponentProps {\n");
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> fields = 
                    (List<Map<String, Object>>) dialogInfo.get("fields");
                
                if (fields != null) {
                    for (Map<String, Object> field : fields) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> properties = 
                            (Map<String, String>) field.get("properties");
                        
                        if (properties != null) {
                            String fieldName = properties.get("name");
                            String fieldLabel = properties.get("fieldLabel");
                            String nodeType = (String) field.get("nodeType");
                            
                            if (fieldName != null && !fieldName.isEmpty()) {
                                String propType = inferTypeFromFieldType(nodeType);
                                String optional = Boolean.TRUE.equals(field.get("required")) ? "" : "?";
                                String comment = fieldLabel != null ? " // " + fieldLabel : "";
                                
                                markdown.append("  ").append(fieldName)
                                        .append(optional).append(": ").append(propType)
                                        .append(";").append(comment).append("\n");
                            }
                        }
                    }
                }
                
                markdown.append("}\n");
                markdown.append("```\n\n");
            }
            
            markdown.append("### 组件结构建议\n\n");
            markdown.append("根据 AEM 组件的结构，建议创建以下 React 组件文件：\n\n");
            markdown.append("- `Component.tsx` - 主组件\n");
            markdown.append("- `Component.module.css` - 样式文件\n");
            markdown.append("- `Component.types.ts` - TypeScript 类型定义\n");
            markdown.append("- `index.ts` - 导出文件\n\n");
        }
        
        // 写入文件
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(markdown.toString());
        }
        
        log.info("React 组件建议已导出到: " + outputPath);
    }

    /**
     * 根据字段类型推断 TypeScript 类型
     * 
     * @param nodeType 节点类型
     * @return TypeScript 类型字符串
     */
    private String inferTypeFromFieldType(String nodeType) {
        if (nodeType == null) {
            return "string";
        }
        
        if (nodeType.contains("numberfield")) {
            return "number";
        } else if (nodeType.contains("checkbox") || nodeType.contains("switch")) {
            return "boolean";
        } else if (nodeType.contains("datepicker")) {
            return "Date | string";
        } else if (nodeType.contains("select") || nodeType.contains("radio")) {
            return "string"; // 可以进一步细化为 union type
        } else {
            return "string";
        }
    }
}

