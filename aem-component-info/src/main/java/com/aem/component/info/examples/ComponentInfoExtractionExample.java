package com.aem.component.info.examples;

import com.aem.component.info.ComponentExporter;
import com.aem.component.info.ComponentInfoExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 组件信息提取完整示例
 * 
 * 这个示例展示了如何使用 ComponentInfoExtractor 来提取组件信息，
 * 并使用 ComponentExporter 导出为 JSON 格式。
 * 
 * 这是学习 AEM 组件信息提取的完整流程示例。
 */
public class ComponentInfoExtractionExample {

    private static final Logger log = LoggerFactory.getLogger(ComponentInfoExtractionExample.class);

    // 示例：要分析的组件路径（根据你的实际环境修改）
    private static final String EXAMPLE_COMPONENT_PATH = "/apps/myproject/components/mycomponent";
    private static final String EXAMPLE_COMPONENTS_BASE_PATH = "/apps/myproject/components";

    /**
     * 主方法
     */
    public static void main(String[] args) {
        ComponentInfoExtractionExample example = new ComponentInfoExtractionExample();
        
        // 注意：这里需要实际的 Session 对象
        // 在实际环境中，你应该从 OSGi 服务或 Repository 获取 Session
        Session session = null; // TODO: 获取实际的 Session
        
        if (session == null) {
            log.error("无法获取 JCR Session。请在 AEM bundle 中运行此示例。");
            log.info("在 AEM bundle 中，你可以这样获取 Session：");
            log.info("@Reference");
            log.info("private SlingRepository repository;");
            log.info("Session session = repository.loginAdministrative(null);");
            return;
        }
        
        try {
            // 示例 1: 提取单个组件信息
            example.extractSingleComponent(session);
            
            // 示例 2: 批量提取组件信息
            example.extractMultipleComponents(session);
            
            // 示例 3: 导出组件信息
            example.exportComponentInfo(session);
            
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * 示例 1: 提取单个组件信息
     */
    private void extractSingleComponent(Session session) {
        log.info("\n=== 示例 1: 提取单个组件信息 ===\n");
        
        ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        
        // 提取完整信息
        Map<String, Object> componentInfo = extractor.extractComponentInfo(EXAMPLE_COMPONENT_PATH);
        
        // 打印基本信息
        @SuppressWarnings("unchecked")
        Map<String, String> basicProperties = 
            (Map<String, String>) componentInfo.get("basicProperties");
        
        if (basicProperties != null) {
            log.info("组件名称: " + basicProperties.get("jcr:title"));
            log.info("Resource Type: " + basicProperties.get("sling:resourceType"));
            log.info("组件分组: " + basicProperties.get("componentGroup"));
            log.info("组件路径: " + basicProperties.get("componentPath"));
        }
        
        // 检查对话框
        @SuppressWarnings("unchecked")
        Map<String, Object> dialogInfo = (Map<String, Object>) componentInfo.get("dialog");
        if (dialogInfo != null && !"none".equals(dialogInfo.get("type"))) {
            log.info("对话框类型: " + dialogInfo.get("type"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = 
                (List<Map<String, Object>>) dialogInfo.get("fields");
            if (fields != null) {
                log.info("对话框字段数量: " + fields.size());
            }
        } else {
            log.info("组件没有对话框配置");
        }
    }

    /**
     * 示例 2: 批量提取组件信息
     */
    private void extractMultipleComponents(Session session) {
        log.info("\n=== 示例 2: 批量提取组件信息 ===\n");
        
        ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        
        // 从指定路径提取所有组件
        List<Map<String, Object>> componentsInfo = 
            extractor.extractComponentsFromPath(EXAMPLE_COMPONENTS_BASE_PATH);
        
        log.info("找到 " + componentsInfo.size() + " 个组件");
        
        // 打印每个组件的基本信息
        for (Map<String, Object> componentInfo : componentsInfo) {
            @SuppressWarnings("unchecked")
            Map<String, String> basicProperties = 
                (Map<String, String>) componentInfo.get("basicProperties");
            
            if (basicProperties != null) {
                String title = basicProperties.get("jcr:title");
                String resourceType = basicProperties.get("sling:resourceType");
                log.info("  - " + title + " (" + resourceType + ")");
            }
        }
    }

    /**
     * 示例 3: 导出组件信息为 JSON
     */
    private void exportComponentInfo(Session session) {
        log.info("\n=== 示例 3: 导出组件信息 ===\n");
        
        ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        ComponentExporter exporter = new ComponentExporter();
        
        // 提取组件信息
        Map<String, Object> componentInfo = extractor.extractComponentInfo(EXAMPLE_COMPONENT_PATH);
        
        try {
            // 导出为 JSON 文件
            String outputPath = "output/component-info.json";
            exporter.exportComponentToJson(componentInfo, outputPath);
            log.info("组件信息已导出到: " + outputPath);
            
            // 导出为 JSON 字符串（可以在控制台查看）
            String jsonString = exporter.exportComponentToJsonString(componentInfo);
            log.info("\n导出的 JSON (前 500 字符):\n" + 
                    jsonString.substring(0, Math.min(500, jsonString.length())) + "...");
            
            // 导出 React 组件建议
            String reactSuggestionPath = "output/react-component-suggestion.md";
            exporter.exportReactComponentSuggestions(componentInfo, reactSuggestionPath);
            log.info("React 组件建议已导出到: " + reactSuggestionPath);
            
        } catch (IOException e) {
            log.error("导出组件信息时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 示例 4: 提取简化信息（快速查询）
     */
    private void extractSimpleComponentInfo(Session session) {
        log.info("\n=== 示例 4: 提取简化组件信息 ===\n");
        
        ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        
        // 提取简化信息（不包含对话框详细分析等）
        Map<String, Object> simpleInfo = extractor.extractComponentInfoSimple(EXAMPLE_COMPONENT_PATH);
        
        log.info("简化组件信息:");
        for (Map.Entry<String, Object> entry : simpleInfo.entrySet()) {
            log.info("  " + entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * 示例 5: 批量导出组件信息
     */
    private void exportMultipleComponents(Session session) {
        log.info("\n=== 示例 5: 批量导出组件信息 ===\n");
        
        ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        ComponentExporter exporter = new ComponentExporter();
        
        // 提取所有组件信息
        List<Map<String, Object>> componentsInfo = 
            extractor.extractComponentsFromPath(EXAMPLE_COMPONENTS_BASE_PATH);
        
        try {
            // 批量导出到目录
            String outputDirectory = "output/components";
            exporter.exportComponentsToJson(componentsInfo, outputDirectory);
            log.info("所有组件信息已导出到目录: " + outputDirectory);
            
        } catch (IOException e) {
            log.error("批量导出组件信息时出错: " + e.getMessage(), e);
        }
    }
}

