package com.aem.component.info.examples;

import com.aem.component.info.ComponentComparator;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;

/**
 * 组件对比示例
 * 
 * 展示如何使用 ComponentComparator 来对比两个组件的差异。
 * 
 * 使用场景：
 * - 对比组件不同版本
 * - 对比自定义组件和核心组件
 * - 分析组件配置变化
 */
@Component(service = ComponentComparisonExample.class)
public class ComponentComparisonExample {

    private static final Logger log = LoggerFactory.getLogger(ComponentComparisonExample.class);

    @Reference
    private SlingRepository repository;

    /**
     * 对比两个组件版本
     */
    public void compareComponentVersions() {
        String v1Path = "/apps/myproject/components/v1/mycomponent";
        String v2Path = "/apps/myproject/components/v2/mycomponent";
        
        Session session = repository.loginAdministrative(null);
        try {
            ComponentComparator comparator = new ComponentComparator(session);
            Map<String, Object> comparison = comparator.compareComponents(v1Path, v2Path);
            
            // 打印对比结果
            printComparisonResult(comparison);
            
            // 导出对比结果
            comparator.compareAndExport(v1Path, v2Path, "output/component-comparison.json");
            
        } catch (RepositoryException e) {
            log.error("对比组件时出错", e);
        } finally {
            session.logout();
        }
    }

    /**
     * 对比自定义组件和核心组件
     */
    public void compareWithCoreComponent() {
        String customPath = "/apps/myproject/components/mycomponent";
        String corePath = "/libs/core/wcm/components/text/v2/text";
        
        Session session = repository.loginAdministrative(null);
        try {
            ComponentComparator comparator = new ComponentComparator(session);
            Map<String, Object> comparison = comparator.compareComponents(customPath, corePath);
            
            printComparisonResult(comparison);
            
        } catch (RepositoryException e) {
            log.error("对比组件时出错", e);
        } finally {
            session.logout();
        }
    }

    /**
     * 打印对比结果
     */
    @SuppressWarnings("unchecked")
    private void printComparisonResult(Map<String, Object> comparison) {
        log.info("=== 组件对比结果 ===");
        log.info("组件1: " + comparison.get("component1"));
        log.info("组件2: " + comparison.get("component2"));
        
        // 基本属性对比
        Map<String, Object> basicDiff = (Map<String, Object>) comparison.get("basicProperties");
        log.info("\n基本属性对比:");
        log.info("  是否相同: " + basicDiff.get("identical"));
        
        @SuppressWarnings("unchecked")
        List<String> differences = (List<String>) basicDiff.get("differences");
        if (differences != null && !differences.isEmpty()) {
            log.info("  差异:");
            for (String diff : differences) {
                log.info("    - " + diff);
            }
        }
        
        // 字段对比
        Map<String, Object> fieldsDiff = (Map<String, Object>) comparison.get("fields");
        log.info("\n字段对比:");
        log.info("  是否相同: " + fieldsDiff.get("identical"));
        
        @SuppressWarnings("unchecked")
        List<String> differentFields = (List<String>) fieldsDiff.get("different");
        if (differentFields != null && !differentFields.isEmpty()) {
            log.info("  不同字段: " + differentFields.size());
        }
        
        // 总结
        Map<String, Object> summary = (Map<String, Object>) comparison.get("summary");
        log.info("\n总结:");
        log.info("  组件是否完全相同: " + summary.get("componentsIdentical"));
    }
}

