package com.aem.component.info;

import com.aem.component.util.JCRUtil;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话框分析器
 * 
 * 分析 AEM 组件的对话框配置，提取字段定义、验证规则、默认值等信息。
 * 对话框定义了组件作者在编辑组件时可以配置的属性。
 * 
 * 支持两种对话框类型：
 * 1. 触摸优化对话框（_cq_dialog.xml）- Granite UI，现代标准
 * 2. 经典对话框（dialog/.content.xml）- ExtJS，旧版
 */
public class DialogAnalyzer {

    /**
     * 分析组件的对话框
     * 
     * @param componentNode 组件节点
     * @return 对话框分析结果
     */
    public static Map<String, Object> analyzeDialog(Node componentNode) {
        Map<String, Object> dialogInfo = new HashMap<>();
        
        // 检查触摸优化对话框（优先）
        Node touchDialog = JCRUtil.getChildNode(componentNode, "_cq_dialog");
        if (touchDialog != null) {
            dialogInfo.put("type", "touch");
            dialogInfo.put("touchDialog", analyzeTouchDialog(touchDialog));
        }
        
        // 检查经典对话框
        Node classicDialog = JCRUtil.getChildNode(componentNode, "dialog");
        if (classicDialog != null) {
            if (dialogInfo.isEmpty()) {
                dialogInfo.put("type", "classic");
            } else {
                dialogInfo.put("hasClassicDialog", true);
            }
            dialogInfo.put("classicDialog", analyzeClassicDialog(classicDialog));
        }
        
        if (dialogInfo.isEmpty()) {
            dialogInfo.put("type", "none");
            dialogInfo.put("message", "组件没有对话框配置");
        }
        
        return dialogInfo;
    }

    /**
     * 分析触摸优化对话框（Granite UI）
     * 
     * 触摸对话框使用 Granite UI 组件，结构通常是：
     * _cq_dialog
     *   - content
     *     - items
     *       - tabs (如果是标签页)
     *         - items
     *           - container
     *             - items
     *               - 具体字段（textfield, textarea, select 等）
     * 
     * @param dialogNode 对话框节点
     * @return 分析结果
     */
    public static Map<String, Object> analyzeTouchDialog(Node dialogNode) {
        Map<String, Object> dialogAnalysis = new HashMap<>();
        
        try {
            // 获取对话框的基本属性
            Map<String, String> dialogProperties = JCRUtil.getAllProperties(dialogNode);
            dialogAnalysis.put("properties", dialogProperties);
            
            // 查找 content 节点
            Node contentNode = JCRUtil.getChildNode(dialogNode, "content");
            if (contentNode != null) {
                Map<String, Object> contentAnalysis = analyzeDialogContent(contentNode);
                dialogAnalysis.put("content", contentAnalysis);
            }
            
            // 提取所有字段
            List<Map<String, Object>> fields = extractFieldsFromDialog(dialogNode);
            dialogAnalysis.put("fields", fields);
            
        } catch (Exception e) {
            System.err.println("分析触摸对话框时出错: " + e.getMessage());
            dialogAnalysis.put("error", e.getMessage());
        }
        
        return dialogAnalysis;
    }

    /**
     * 分析对话框内容结构
     * 
     * @param contentNode content 节点
     * @return 内容分析结果
     */
    private static Map<String, Object> analyzeDialogContent(Node contentNode) 
            throws RepositoryException {
        Map<String, Object> contentAnalysis = new HashMap<>();
        
        // 获取 items 节点
        Node itemsNode = JCRUtil.getChildNode(contentNode, "items");
        if (itemsNode != null) {
            List<Map<String, Object>> items = analyzeDialogItems(itemsNode);
            contentAnalysis.put("items", items);
        }
        
        return contentAnalysis;
    }

    /**
     * 分析对话框中的 items（可能是标签页、字段组等）
     * 
     * @param itemsNode items 节点
     * @return items 分析结果列表
     */
    private static List<Map<String, Object>> analyzeDialogItems(Node itemsNode) 
            throws RepositoryException {
        List<Map<String, Object>> itemsList = new ArrayList<>();
        
        NodeIterator nodeIterator = itemsNode.getNodes();
        while (nodeIterator.hasNext()) {
            Node itemNode = nodeIterator.nextNode();
            Map<String, Object> itemInfo = analyzeDialogItem(itemNode);
            itemsList.add(itemInfo);
        }
        
        return itemsList;
    }

    /**
     * 分析单个对话框项
     * 
     * @param itemNode 项节点
     * @return 项的信息
     */
    private static Map<String, Object> analyzeDialogItem(Node itemNode) 
            throws RepositoryException {
        Map<String, Object> itemInfo = new HashMap<>();
        
        String nodeName = JCRUtil.getName(itemNode);
        String nodeType = itemNode.getPrimaryNodeType().getName();
        
        itemInfo.put("name", nodeName);
        itemInfo.put("nodeType", nodeType);
        
        // 获取属性
        Map<String, String> properties = JCRUtil.getAllProperties(itemNode);
        itemInfo.put("properties", properties);
        
        // 检查是否有子 items
        Node childItemsNode = JCRUtil.getChildNode(itemNode, "items");
        if (childItemsNode != null) {
            List<Map<String, Object>> childItems = analyzeDialogItems(childItemsNode);
            itemInfo.put("childItems", childItems);
        }
        
        return itemInfo;
    }

    /**
     * 从对话框节点中提取所有字段定义
     * 
     * 字段是实际的数据输入控件，如 textfield、textarea、select 等。
     * 
     * @param dialogNode 对话框节点
     * @return 字段列表
     */
    private static List<Map<String, Object>> extractFieldsFromDialog(Node dialogNode) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        try {
            // 递归搜索所有节点，查找字段节点
            extractFieldsRecursive(dialogNode, fields);
        } catch (Exception e) {
            System.err.println("提取字段时出错: " + e.getMessage());
        }
        
        return fields;
    }

    /**
     * 递归提取字段
     * 
     * @param node 当前节点
     * @param fields 字段列表（用于收集结果）
     */
    private static void extractFieldsRecursive(Node node, List<Map<String, Object>> fields) 
            throws RepositoryException {
        
        String nodeType = node.getPrimaryNodeType().getName();
        
        // 检查是否是字段节点
        // Granite UI 字段通常以 "granite/ui/components/coral/foundation/form/" 开头
        if (isFieldNode(nodeType)) {
            Map<String, Object> fieldInfo = extractFieldInfo(node);
            fields.add(fieldInfo);
        }
        
        // 递归处理子节点
        NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            extractFieldsRecursive(nodeIterator.nextNode(), fields);
        }
    }

    /**
     * 判断是否是字段节点
     * 
     * @param nodeType 节点类型
     * @return 如果是字段节点则返回 true
     */
    private static boolean isFieldNode(String nodeType) {
        // Granite UI 字段节点类型
        String[] fieldTypes = {
            "granite/ui/components/coral/foundation/form/textfield",
            "granite/ui/components/coral/foundation/form/textarea",
            "granite/ui/components/coral/foundation/form/numberfield",
            "granite/ui/components/coral/foundation/form/checkbox",
            "granite/ui/components/coral/foundation/form/radio",
            "granite/ui/components/coral/foundation/form/select",
            "granite/ui/components/coral/foundation/form/pathfield",
            "granite/ui/components/coral/foundation/form/datepicker",
            "granite/ui/components/coral/foundation/form/switch",
            "granite/ui/components/coral/foundation/form/colorfield",
            "granite/ui/components/coral/foundation/form/range",
            // 其他常见字段类型
            "granite/ui/components/coral/foundation/form/hidden",
            "granite/ui/components/foundation/form/autocomplete"
        };
        
        for (String fieldType : fieldTypes) {
            if (nodeType.contains(fieldType) || nodeType.endsWith(fieldType.substring(fieldType.lastIndexOf('/')))) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 提取字段的详细信息
     * 
     * 字段的关键属性：
     * - name: 字段名称（对应组件的属性名）
     * - fieldLabel: 字段标签（显示给用户）
     * - value: 默认值
     * - required: 是否必填
     * - disabled: 是否禁用
     * - jcr:title: 标题
     * 
     * @param fieldNode 字段节点
     * @return 字段信息
     */
    private static Map<String, Object> extractFieldInfo(Node fieldNode) 
            throws RepositoryException {
        Map<String, Object> fieldInfo = new HashMap<>();
        
        // 节点名称和类型
        fieldInfo.put("nodeName", JCRUtil.getName(fieldNode));
        fieldInfo.put("nodeType", fieldNode.getPrimaryNodeType().getName());
        fieldInfo.put("path", JCRUtil.getPath(fieldNode));
        
        // 提取所有属性
        Map<String, String> properties = JCRUtil.getAllProperties(fieldNode);
        fieldInfo.put("properties", properties);
        
        // 关键字段属性
        String fieldName = JCRUtil.getProperty(fieldNode, "name");
        String fieldLabel = JCRUtil.getProperty(fieldNode, "fieldLabel");
        String title = JCRUtil.getProperty(fieldNode, "jcr:title");
        String value = JCRUtil.getProperty(fieldNode, "value");
        String required = JCRUtil.getProperty(fieldNode, "required");
        
        fieldInfo.put("name", fieldName);
        fieldInfo.put("fieldLabel", fieldLabel != null ? fieldLabel : title);
        fieldInfo.put("defaultValue", value);
        fieldInfo.put("required", "true".equals(required));
        
        // 检查是否有选项（对于 select、radio 等字段）
        Node itemsNode = JCRUtil.getChildNode(fieldNode, "items");
        if (itemsNode != null) {
            List<Map<String, String>> options = extractFieldOptions(itemsNode);
            fieldInfo.put("options", options);
        }
        
        // 检查是否有验证规则
        Node validationNode = JCRUtil.getChildNode(fieldNode, "validation");
        if (validationNode != null) {
            Map<String, String> validation = JCRUtil.getAllProperties(validationNode);
            fieldInfo.put("validation", validation);
        }
        
        return fieldInfo;
    }

    /**
     * 提取字段的选项（用于 select、radio 等字段）
     * 
     * @param itemsNode items 节点
     * @return 选项列表
     */
    private static List<Map<String, String>> extractFieldOptions(Node itemsNode) 
            throws RepositoryException {
        List<Map<String, String>> options = new ArrayList<>();
        
        NodeIterator nodeIterator = itemsNode.getNodes();
        while (nodeIterator.hasNext()) {
            Node optionNode = nodeIterator.nextNode();
            Map<String, String> option = new HashMap<>();
            
            option.put("text", JCRUtil.getProperty(optionNode, "text"));
            option.put("value", JCRUtil.getProperty(optionNode, "value"));
            option.put("jcr:title", JCRUtil.getProperty(optionNode, "jcr:title"));
            
            options.add(option);
        }
        
        return options;
    }

    /**
     * 分析经典对话框（ExtJS）
     * 
     * 经典对话框使用 ExtJS 组件，结构有所不同。
     * 这里提供基本的分析框架，可以根据需要扩展。
     * 
     * @param dialogNode 对话框节点
     * @return 分析结果
     */
    public static Map<String, Object> analyzeClassicDialog(Node dialogNode) {
        Map<String, Object> dialogAnalysis = new HashMap<>();
        
        try {
            dialogAnalysis.put("type", "classic");
            
            // 获取对话框属性
            Map<String, String> properties = JCRUtil.getAllProperties(dialogNode);
            dialogAnalysis.put("properties", properties);
            
            // 提取字段（经典对话框的结构与触摸对话框不同）
            List<Map<String, Object>> fields = extractFieldsFromDialog(dialogNode);
            dialogAnalysis.put("fields", fields);
            
        } catch (Exception e) {
            System.err.println("分析经典对话框时出错: " + e.getMessage());
            dialogAnalysis.put("error", e.getMessage());
        }
        
        return dialogAnalysis;
    }

    /**
     * 分析设计对话框
     * 
     * 设计对话框用于配置组件的设计模式属性。
     * 
     * @param componentNode 组件节点
     * @return 设计对话框分析结果
     */
    public static Map<String, Object> analyzeDesignDialog(Node componentNode) {
        Map<String, Object> designDialogInfo = new HashMap<>();
        
        Node designDialogNode = JCRUtil.getChildNode(componentNode, "_cq_design_dialog");
        if (designDialogNode == null) {
            designDialogInfo.put("exists", false);
            return designDialogInfo;
        }
        
        designDialogInfo.put("exists", true);
        designDialogInfo.put("analysis", analyzeTouchDialog(designDialogNode));
        
        return designDialogInfo;
    }
}

