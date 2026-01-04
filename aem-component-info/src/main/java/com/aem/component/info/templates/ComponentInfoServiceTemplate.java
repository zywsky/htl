package com.aem.component.info.templates;

import com.aem.component.info.ComponentExporter;
import com.aem.component.info.ComponentInfoExtractor;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 组件信息服务模板
 * 
 * 这是一个完整的 OSGi 服务模板，展示了如何创建一个生产就绪的组件信息服务。
 * 你可以基于这个模板创建自己的服务。
 * 
 * 特性：
 * - OSGi 服务配置
 * - 缓存支持
 * - 错误处理
 * - 资源管理
 * - 日志记录
 */
@Component(
    service = ComponentInfoServiceTemplate.class,
    immediate = true
)
@Designate(ocd = ComponentInfoServiceTemplate.ServiceConfig.class)
public class ComponentInfoServiceTemplate {

    private static final Logger log = LoggerFactory.getLogger(ComponentInfoServiceTemplate.class);

    @Reference
    private SlingRepository repository;

    // 配置参数
    private String allowedBasePath;
    private int maxDepth;
    private boolean enableCache;
    private long cacheExpirationTime;

    // 缓存
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * OSGi 配置接口
     */
    @org.osgi.service.metatype.annotations.ObjectClassDefinition(
        name = "Component Info Service Configuration",
        description = "组件信息服务配置"
    )
    public @interface ServiceConfig {
        
        @org.osgi.service.metatype.annotations.AttributeDefinition(
            name = "Allowed Base Path",
            description = "允许访问的基础路径（安全限制）"
        )
        String allowedBasePath() default "/apps/myproject";

        @org.osgi.service.metatype.annotations.AttributeDefinition(
            name = "Max Depth",
            description = "最大搜索深度"
        )
        int maxDepth() default 5;

        @org.osgi.service.metatype.annotations.AttributeDefinition(
            name = "Enable Cache",
            description = "是否启用缓存"
        )
        boolean enableCache() default true;

        @org.osgi.service.metatype.annotations.AttributeDefinition(
            name = "Cache Expiration Time (seconds)",
            description = "缓存过期时间（秒）"
        )
        long cacheExpirationTime() default 3600;
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        final Map<String, Object> data;
        final long timestamp;

        CacheEntry(Map<String, Object> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long expirationTime) {
            return System.currentTimeMillis() - timestamp > expirationTime * 1000;
        }
    }

    /**
     * 激活服务
     */
    @Activate
    protected void activate(ServiceConfig config) {
        this.allowedBasePath = config.allowedBasePath();
        this.maxDepth = config.maxDepth();
        this.enableCache = config.enableCache();
        this.cacheExpirationTime = config.cacheExpirationTime();

        log.info("组件信息服务已激活 - 基础路径: {}, 最大深度: {}, 缓存: {}", 
            allowedBasePath, maxDepth, enableCache);
    }

    /**
     * 停用服务
     */
    @Deactivate
    protected void deactivate() {
        clearCache();
        log.info("组件信息服务已停用");
    }

    /**
     * 提取组件信息
     * 
     * @param componentPath 组件路径
     * @return 组件信息 Map
     */
    public Map<String, Object> getComponentInfo(String componentPath) {
        // 验证路径
        validatePath(componentPath);

        // 检查缓存
        if (enableCache) {
            CacheEntry cached = cache.get(componentPath);
            if (cached != null && !cached.isExpired(cacheExpirationTime)) {
                log.debug("从缓存获取组件信息: {}", componentPath);
                return cached.data;
            }
        }

        // 提取信息
        Session session = null;
        try {
            // 使用服务用户（生产环境推荐）
            // session = repository.loginService("component-reader", null);
            
            // 开发环境可以使用管理会话
            session = repository.loginAdministrative(null);

            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            Map<String, Object> componentInfo = extractor.extractComponentInfo(componentPath);

            // 存入缓存
            if (enableCache) {
                cache.put(componentPath, new CacheEntry(componentInfo));
            }

            return componentInfo;

        } catch (RepositoryException e) {
            log.error("提取组件信息失败: " + componentPath, e);
            return createErrorResponse("提取失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("意外错误", e);
            return createErrorResponse("系统错误");
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * 提取简化组件信息
     */
    public Map<String, Object> getComponentInfoSimple(String componentPath) {
        validatePath(componentPath);

        Session session = null;
        try {
            session = repository.loginAdministrative(null);
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            return extractor.extractComponentInfoSimple(componentPath);
        } catch (RepositoryException e) {
            log.error("提取简化组件信息失败: " + componentPath, e);
            return createErrorResponse("提取失败: " + e.getMessage());
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * 导出组件信息为 JSON 字符串
     */
    public String exportComponentInfoAsJson(String componentPath) {
        Map<String, Object> componentInfo = getComponentInfo(componentPath);
        try {
            ComponentExporter exporter = new ComponentExporter();
            return exporter.exportComponentToJsonString(componentInfo);
        } catch (Exception e) {
            log.error("导出组件信息失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 验证路径
     */
    private void validatePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("路径不能为空");
        }

        if (!path.startsWith(allowedBasePath) && !path.startsWith("/libs/")) {
            throw new SecurityException("不允许访问该路径: " + path);
        }

        if (path.contains("..")) {
            throw new IllegalArgumentException("无效的路径: " + path);
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> error = new java.util.HashMap<>();
        error.put("error", errorMessage);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        cache.clear();
        log.info("缓存已清理");
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("size", cache.size());
        stats.put("enabled", enableCache);
        stats.put("expirationTime", cacheExpirationTime);
        return stats;
    }
}

