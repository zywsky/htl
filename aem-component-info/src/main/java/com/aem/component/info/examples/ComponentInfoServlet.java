package com.aem.component.info.examples;

import com.aem.component.info.ComponentExporter;
import com.aem.component.info.ComponentInfoExtractor;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP Servlet 示例 - 通过 REST API 访问组件信息
 * 
 * 这个 Servlet 提供了通过 HTTP 请求获取组件信息的 REST API。
 * 
 * 使用示例：
 * GET /bin/componentinfo?path=/apps/myproject/components/mycomponent
 * GET /bin/componentinfo/batch?basePath=/apps/myproject/components
 * GET /bin/componentinfo/simple?path=/apps/myproject/components/mycomponent
 * 
 * 部署后访问：
 * http://localhost:4502/bin/componentinfo?path=/apps/myproject/components/mycomponent
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/componentinfo",
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=json"
    }
)
public class ComponentInfoServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(ComponentInfoServlet.class);

    @Reference
    private SlingRepository repository;

    @Override
    protected void doGet(SlingHttpServletRequest request, 
                        SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // 获取请求路径信息
            String requestPath = request.getPathInfo();
            
            // 处理批量请求
            if (requestPath != null && requestPath.contains("/batch")) {
                handleBatchRequest(request, response);
                return;
            }
            
            // 处理简化信息请求
            if (requestPath != null && requestPath.contains("/simple")) {
                handleSimpleRequest(request, response);
                return;
            }
            
            // 处理单个组件请求
            handleSingleComponentRequest(request, response);
            
        } catch (Exception e) {
            log.error("处理请求时出错", e);
            response.setStatus(500);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 处理单个组件信息请求
     * 
     * 参数：
     * - path: 组件路径（必需）
     * - format: 输出格式（json|pretty，默认 json）
     */
    private void handleSingleComponentRequest(SlingHttpServletRequest request,
                                             SlingHttpServletResponse response)
            throws RepositoryException, IOException {
        
        String componentPath = request.getParameter("path");
        if (componentPath == null || componentPath.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"缺少 path 参数\"}");
            return;
        }
        
        String format = request.getParameter("format");
        boolean pretty = "pretty".equals(format);
        
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            ComponentExporter exporter = new ComponentExporter();
            
            // 提取组件信息
            Map<String, Object> componentInfo = extractor.extractComponentInfo(componentPath);
            
            // 检查是否有错误
            if (componentInfo.containsKey("error")) {
                response.setStatus(404);
                response.getWriter().write(exporter.exportComponentToJsonString(componentInfo));
                return;
            }
            
            // 导出为 JSON
            if (pretty) {
                // 格式化输出
                String json = exporter.exportComponentToJsonString(componentInfo);
                response.getWriter().write(json);
            } else {
                // 紧凑输出
            String json = exporter.exportComponentToJsonString(componentInfo);
            response.getWriter().write(json);
            }
            
        } finally {
            session.logout();
        }
    }

    /**
     * 处理批量组件信息请求
     * 
     * 参数：
     * - basePath: 组件基础路径（必需）
     * - limit: 限制返回数量（可选）
     */
    private void handleBatchRequest(SlingHttpServletRequest request,
                                   SlingHttpServletResponse response)
            throws RepositoryException, IOException {
        
        String basePath = request.getParameter("basePath");
        if (basePath == null || basePath.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"缺少 basePath 参数\"}");
            return;
        }
        
        String limitStr = request.getParameter("limit");
        int limit = limitStr != null ? Integer.parseInt(limitStr) : -1;
        
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            ComponentExporter exporter = new ComponentExporter();
            
            // 批量提取组件信息
            List<Map<String, Object>> components = extractor.extractComponentsFromPath(basePath);
            
            // 应用限制
            if (limit > 0 && components.size() > limit) {
                components = components.subList(0, limit);
            }
            
            // 构建响应
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("total", components.size());
            responseData.put("basePath", basePath);
            responseData.put("components", components);
            
            // 导出为 JSON
            String json = exporter.exportComponentToJsonString(responseData);
            response.getWriter().write(json);
            
        } finally {
            session.logout();
        }
    }

    /**
     * 处理简化组件信息请求
     * 
     * 返回组件的简化信息（不包含详细的对话框分析等）
     */
    private void handleSimpleRequest(SlingHttpServletRequest request,
                                     SlingHttpServletResponse response)
            throws RepositoryException, IOException {
        
        String componentPath = request.getParameter("path");
        if (componentPath == null || componentPath.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"缺少 path 参数\"}");
            return;
        }
        
        Session session = repository.loginAdministrative(null);
        try {
            ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
            ComponentExporter exporter = new ComponentExporter();
            
            // 提取简化信息
            Map<String, Object> componentInfo = extractor.extractComponentInfoSimple(componentPath);
            
            // 导出为 JSON
                String json = exporter.exportComponentToJsonString(componentInfo);
                response.getWriter().write(json);
            
        } finally {
            session.logout();
        }
    }
}
