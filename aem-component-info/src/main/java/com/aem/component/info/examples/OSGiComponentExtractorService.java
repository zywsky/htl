package com.aem.component.info.examples;

import com.aem.component.info.ComponentExporter;
import com.aem.component.info.ComponentInfoExtractor;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * OSGi 服务示例 - 在 AEM bundle 中使用组件提取器
 * 
 * 这是一个完整的 OSGi 服务示例，展示如何在 AEM bundle 中正确使用
 * ComponentInfoExtractor 来提取组件信息。
 * 
 * 使用方法：
 * 1. 将这个类放在你的 AEM bundle 中
 * 2. 确保依赖了必要的 OSGi 服务（SlingRepository）
 * 3. 通过 HTTP Servlet、Sling Model 或其他方式调用服务方法
 */
@Component(
    service = OSGiComponentExtractorService.class,
    immediate = true
)
public class OSGiComponentExtractorService {

    private static final Logger log = LoggerFactory.getLogger(OSGiComponentExtractorService.class);

    // 注入 SlingRepository 服务
    @Reference
    private SlingRepository repository;

    /**
     * 提取组件信息的服务方法
     * 
     * @param componentPath 组件路径
     * @return 组件信息 Map
     */
    public Map<String, Object> extractComponentInfo(String componentPath) {
        Session session = null;
        try {
            // 使用管理会话（具有完整权限）
            // 注意：在生产环境中，应该使用适当的权限会话
            session = repository.loginAdministrative(null);
            
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            return extractor.extractComponentInfo(componentPath);
            
        } catch (RepositoryException e) {
            log.error("提取组件信息时出错: " + e.getMessage(), e);
            return null;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * 批量提取组件信息
     * 
     * @param basePath 组件基础路径
     * @return 组件信息列表
     */
    public List<Map<String, Object>> extractComponentsFromPath(String basePath) {
        Session session = null;
        try {
            session = repository.loginAdministrative(null);
            
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            return extractor.extractComponentsFromPath(basePath);
            
        } catch (RepositoryException e) {
            log.error("批量提取组件信息时出错: " + e.getMessage(), e);
            return null;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * 导出组件信息为 JSON 字符串
     * 
     * @param componentPath 组件路径
     * @return JSON 字符串
     */
    public String exportComponentInfoAsJson(String componentPath) {
        Session session = null;
        try {
            session = repository.loginAdministrative(null);
            
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            ComponentExporter exporter = new ComponentExporter();
            
            Map<String, Object> componentInfo = extractor.extractComponentInfo(componentPath);
            return exporter.exportComponentToJsonString(componentInfo);
            
        } catch (Exception e) {
            log.error("导出组件信息时出错: " + e.getMessage(), e);
            return null;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * 导出组件信息到文件系统
     * 
     * @param componentPath 组件路径
     * @param outputPath 输出文件路径
     * @return 是否成功
     */
    public boolean exportComponentInfoToFile(String componentPath, String outputPath) {
        Session session = null;
        try {
            session = repository.loginAdministrative(null);
            
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            ComponentExporter exporter = new ComponentExporter();
            
            Map<String, Object> componentInfo = extractor.extractComponentInfo(componentPath);
            exporter.exportComponentToJson(componentInfo, outputPath);
            
            return true;
            
        } catch (Exception e) {
            log.error("导出组件信息到文件时出错: " + e.getMessage(), e);
            return false;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * 使用资源解析器会话（推荐用于 HTTP 请求处理）
     * 
     * 这个方法展示了如何使用 ResourceResolver 来获取会话，
     * 这更符合 AEM 的最佳实践。
     * 
     * @param resourceResolver ResourceResolver 实例（通常从 SlingHttpServletRequest 获取）
     * @param componentPath 组件路径
     * @return 组件信息 Map
     */
    public Map<String, Object> extractComponentInfoWithResourceResolver(
            org.apache.sling.api.resource.ResourceResolver resourceResolver,
            String componentPath) {
        
        try {
            // 从 ResourceResolver 获取会话
            Session session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                log.error("无法从 ResourceResolver 获取 Session");
                return null;
            }
            
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            return extractor.extractComponentInfo(componentPath);
            
        } catch (Exception e) {
            log.error("提取组件信息时出错: " + e.getMessage(), e);
            return null;
        }
        // 注意：不要关闭 ResourceResolver 的 Session，它会被 ResourceResolver 管理
    }
}

