package com.aem.component.info;

import com.aem.component.util.JCRUtil;
import javax.jcr.Node;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组件对比工具
 * 
 * 用于对比两个 AEM 组件的差异，帮助理解组件之间的区别和变化。
 * 
 * 使用场景：
 * - 对比不同版本的组件
 * - 对比自定义组件和核心组件
 * - 分析组件配置差异
 * 
 * 使用示例：
 * <pre>
 * ComponentComparator comparator = new ComponentComparator(session);
 * Map<String, Object> diff = comparator.compareComponents(
 *     "/apps/myproject/components/v1/mycomponent",
 *     "/apps/myproject/components/v2/mycomponent"
 * );
 * </pre>
 */
public class ComponentComparator {

    private final Session session;
    private final ComponentInfoExtractor extractor;

    public ComponentComparator(Session session) {
        this.session = session;
        this.extractor = new ComponentInfoExtractor(session);
    }

    /**
     * 对比两个组件
     * 
     * @param componentPath1 第一个组件路径
     * @param componentPath2 第二个组件路径
     * @return 对比结果，包含差异信息
     */
    public Map<String, Object> compareComponents(String componentPath1, String componentPath2) {
        Map<String, Object> comparison = new HashMap<>();
        
        // 提取两个组件的信息
        Map<String, Object> info1 = extractor.extractComponentInfo(componentPath1);
        Map<String, Object> info2 = extractor.extractComponentInfo(componentPath2);
        
        comparison.put("component1", componentPath1);
        comparison.put("component2", componentPath2);
        
        // 对比基本属性
        Map<String, Object> basicDiff = compareBasicProperties(info1, info2);
        comparison.put("basicProperties", basicDiff);
        
        // 对比对话框
        Map<String, Object> dialogDiff = compareDialogs(info1, info2);
        comparison.put("dialog", dialogDiff);
        
        // 对比字段
        Map<String, Object> fieldsDiff = compareFields(info1, info2);
        comparison.put("fields", fieldsDiff);
        
        // 对比依赖
        Map<String, Object> dependenciesDiff = compareDependencies(info1, info2);
        comparison.put("dependencies", dependenciesDiff);
        
        // 总结
        Map<String, Object> summary = generateSummary(comparison);
        comparison.put("summary", summary);
        
        return comparison;
    }

    /**
     * 对比基本属性
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> compareBasicProperties(Map<String, Object> info1, 
                                                       Map<String, Object> info2) {
        Map<String, Object> diff = new HashMap<>();
        
        Map<String, String> props1 = (Map<String, String>) info1.get("basicProperties");
        Map<String, String> props2 = (Map<String, String>) info2.get("basicProperties");
        
        if (props1 == null || props2 == null) {
            diff.put("error", "无法获取基本属性");
            return diff;
        }
        
        List<String> differences = new ArrayList<>();
        List<String> onlyIn1 = new ArrayList<>();
        List<String> onlyIn2 = new ArrayList<>();
        
        // 检查所有属性
        for (String key : props1.keySet()) {
            String value1 = props1.get(key);
            String value2 = props2.get(key);
            
            if (value2 == null) {
                onlyIn1.add(key);
            } else if (!value1.equals(value2)) {
                differences.add(key + ": '" + value1 + "' vs '" + value2 + "'");
            }
        }
        
        // 检查只在第二个组件中存在的属性
        for (String key : props2.keySet()) {
            if (!props1.containsKey(key)) {
                onlyIn2.add(key);
            }
        }
        
        diff.put("differences", differences);
        diff.put("onlyInComponent1", onlyIn1);
        diff.put("onlyInComponent2", onlyIn2);
        diff.put("identical", differences.isEmpty() && onlyIn1.isEmpty() && onlyIn2.isEmpty());
        
        return diff;
    }

    /**
     * 对比对话框
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> compareDialogs(Map<String, Object> info1, 
                                               Map<String, Object> info2) {
        Map<String, Object> diff = new HashMap<>();
        
        Map<String, Object> dialog1 = (Map<String, Object>) info1.get("dialog");
        Map<String, Object> dialog2 = (Map<String, Object>) info2.get("dialog");
        
        String type1 = (String) dialog1.get("type");
        String type2 = (String) dialog2.get("type");
        
        diff.put("type1", type1);
        diff.put("type2", type2);
        diff.put("typeDifferent", !type1.equals(type2));
        
        // 如果类型相同，进一步对比
        if (type1.equals(type2) && !"none".equals(type1)) {
            List<Map<String, Object>> fields1 = 
                (List<Map<String, Object>>) dialog1.get("fields");
            List<Map<String, Object>> fields2 = 
                (List<Map<String, Object>>) dialog2.get("fields");
            
            diff.put("fieldsCount1", fields1 != null ? fields1.size() : 0);
            diff.put("fieldsCount2", fields2 != null ? fields2.size() : 0);
        }
        
        return diff;
    }

    /**
     * 对比字段
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> compareFields(Map<String, Object> info1, 
                                             Map<String, Object> info2) {
        Map<String, Object> diff = new HashMap<>();
        
        Map<String, Object> dialog1 = (Map<String, Object>) info1.get("dialog");
        Map<String, Object> dialog2 = (Map<String, Object>) info2.get("dialog");
        
        List<Map<String, Object>> fields1 = 
            (List<Map<String, Object>>) dialog1.get("fields");
        List<Map<String, Object>> fields2 = 
            (List<Map<String, Object>>) dialog2.get("fields");
        
        if (fields1 == null) fields1 = new ArrayList<>();
        if (fields2 == null) fields2 = new ArrayList<>();
        
        // 构建字段映射（以字段名称为键）
        Map<String, Map<String, Object>> fieldsMap1 = new HashMap<>();
        Map<String, Map<String, Object>> fieldsMap2 = new HashMap<>();
        
        for (Map<String, Object> field : fields1) {
            @SuppressWarnings("unchecked")
            Map<String, String> props = (Map<String, String>) field.get("properties");
            if (props != null) {
                String name = props.get("name");
                if (name != null) {
                    fieldsMap1.put(name, field);
                }
            }
        }
        
        for (Map<String, Object> field : fields2) {
            @SuppressWarnings("unchecked")
            Map<String, String> props = (Map<String, String>) field.get("properties");
            if (props != null) {
                String name = props.get("name");
                if (name != null) {
                    fieldsMap2.put(name, field);
                }
            }
        }
        
        // 找出差异
        List<String> onlyIn1 = new ArrayList<>();
        List<String> onlyIn2 = new ArrayList<>();
        List<String> different = new ArrayList<>();
        
        for (String fieldName : fieldsMap1.keySet()) {
            if (!fieldsMap2.containsKey(fieldName)) {
                onlyIn1.add(fieldName);
            } else {
                // 可以进一步对比字段属性
                Map<String, Object> f1 = fieldsMap1.get(fieldName);
                Map<String, Object> f2 = fieldsMap2.get(fieldName);
                if (!fieldsEqual(f1, f2)) {
                    different.add(fieldName);
                }
            }
        }
        
        for (String fieldName : fieldsMap2.keySet()) {
            if (!fieldsMap1.containsKey(fieldName)) {
                onlyIn2.add(fieldName);
            }
        }
        
        diff.put("onlyInComponent1", onlyIn1);
        diff.put("onlyInComponent2", onlyIn2);
        diff.put("different", different);
        diff.put("identical", onlyIn1.isEmpty() && onlyIn2.isEmpty() && different.isEmpty());
        
        return diff;
    }

    /**
     * 检查两个字段是否相等
     */
    @SuppressWarnings("unchecked")
    private boolean fieldsEqual(Map<String, Object> field1, Map<String, Object> field2) {
        Map<String, String> props1 = (Map<String, String>) field1.get("properties");
        Map<String, String> props2 = (Map<String, String>) field2.get("properties");
        
        if (props1 == null || props2 == null) {
            return props1 == props2;
        }
        
        // 对比关键属性
        String name1 = props1.get("name");
        String name2 = props2.get("name");
        String type1 = (String) field1.get("nodeType");
        String type2 = (String) field2.get("nodeType");
        
        return name1 != null && name1.equals(name2) && 
               type1 != null && type1.equals(type2);
    }

    /**
     * 对比依赖关系
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> compareDependencies(Map<String, Object> info1, 
                                                    Map<String, Object> info2) {
        Map<String, Object> diff = new HashMap<>();
        
        Map<String, Object> deps1 = (Map<String, Object>) info1.get("dependencies");
        Map<String, Object> deps2 = (Map<String, Object>) info2.get("dependencies");
        
        String superType1 = (String) deps1.get("resourceSuperType");
        String superType2 = (String) deps2.get("resourceSuperType");
        
        diff.put("superType1", superType1);
        diff.put("superType2", superType2);
        diff.put("superTypeDifferent", 
            (superType1 == null ? "" : superType1).equals(superType2 == null ? "" : superType2) ? false : true);
        
        return diff;
    }

    /**
     * 生成对比总结
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> generateSummary(Map<String, Object> comparison) {
        Map<String, Object> summary = new HashMap<>();
        
        Map<String, Object> basicDiff = (Map<String, Object>) comparison.get("basicProperties");
        Map<String, Object> fieldsDiff = (Map<String, Object>) comparison.get("fields");
        
        boolean basicIdentical = (Boolean) basicDiff.get("identical");
        boolean fieldsIdentical = (Boolean) fieldsDiff.get("identical");
        
        summary.put("componentsIdentical", basicIdentical && fieldsIdentical);
        summary.put("basicPropertiesIdentical", basicIdentical);
        summary.put("fieldsIdentical", fieldsIdentical);
        
        @SuppressWarnings("unchecked")
        List<String> basicDifferences = (List<String>) basicDiff.get("differences");
        summary.put("basicPropertyDifferences", basicDifferences != null ? basicDifferences.size() : 0);
        
        @SuppressWarnings("unchecked")
        List<String> differentFields = (List<String>) fieldsDiff.get("different");
        summary.put("differentFields", differentFields != null ? differentFields.size() : 0);
        
        return summary;
    }

    /**
     * 对比组件并导出为 JSON
     * 
     * @param componentPath1 第一个组件路径
     * @param componentPath2 第二个组件路径
     * @param outputPath 输出文件路径
     */
    public void compareAndExport(String componentPath1, String componentPath2, String outputPath) {
        try {
            Map<String, Object> comparison = compareComponents(componentPath1, componentPath2);
            ComponentExporter exporter = new ComponentExporter();
            exporter.exportComponentToJson(comparison, outputPath);
        } catch (Exception e) {
            System.err.println("对比并导出组件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

