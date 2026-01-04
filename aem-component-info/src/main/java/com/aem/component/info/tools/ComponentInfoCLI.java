package com.aem.component.info.tools;

import com.aem.component.info.ComponentExporter;
import com.aem.component.info.ComponentInfoExtractor;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 命令行工具 - 组件信息提取 CLI
 * 
 * 这是一个可以从命令行直接运行的工具，用于提取和导出组件信息。
 * 
 * 使用方式：
 * java -cp ... ComponentInfoCLI --path /apps/myproject/components/mycomponent --output output.json
 * java -cp ... ComponentInfoCLI --basePath /apps/myproject/components --outputDir output/
 * 
 * 参数说明：
 * --path: 单个组件路径
 * --basePath: 组件基础路径（批量提取）
 * --output: 输出文件路径（单个组件）
 * --outputDir: 输出目录（批量提取）
 * --format: 输出格式（json|pretty，默认 json）
 * --simple: 是否只提取简化信息
 */
public class ComponentInfoCLI {

    private static final Logger log = LoggerFactory.getLogger(ComponentInfoCLI.class);

    private SlingRepository repository;
    private ComponentInfoExtractor extractor;
    private ComponentExporter exporter;

    /**
     * 主方法
     */
    public static void main(String[] args) {
        ComponentInfoCLI cli = new ComponentInfoCLI();
        
        // 解析命令行参数
        Map<String, String> params = parseArgs(args);
        
        if (params.isEmpty() || params.containsKey("--help")) {
            printUsage();
            return;
        }
        
        // 初始化（注意：需要实际的 Repository 对象）
        // 在实际使用中，需要通过 OSGi 或其他方式获取 Repository
        log.warn("注意：需要在实际的 AEM 环境中运行，或通过 OSGi 服务获取 Repository");
        
        // 执行命令
        try {
            if (params.containsKey("--path")) {
                cli.handleSingleComponent(params);
            } else if (params.containsKey("--basePath")) {
                cli.handleBatchComponents(params);
            } else {
                log.error("缺少必需参数 --path 或 --basePath");
                printUsage();
            }
        } catch (Exception e) {
            log.error("执行命令时出错: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * 处理单个组件提取
     */
    private void handleSingleComponent(Map<String, String> params) 
            throws RepositoryException, IOException {
        
        String componentPath = params.get("--path");
        String outputPath = params.getOrDefault("--output", "component-info.json");
        boolean simple = params.containsKey("--simple");
        
        log.info("提取组件: " + componentPath);
        log.info("输出文件: " + outputPath);
        
        // 注意：这里需要实际的 Repository
        // Session session = repository.loginAdministrative(null);
        // try {
        //     ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        //     ComponentExporter exporter = new ComponentExporter();
        //     
        //     Map<String, Object> componentInfo;
        //     if (simple) {
        //         componentInfo = extractor.extractComponentInfoSimple(componentPath);
        //     } else {
        //         componentInfo = extractor.extractComponentInfo(componentPath);
        //     }
        //     
        //     exporter.exportComponentToJson(componentInfo, outputPath);
        //     log.info("组件信息已导出到: " + outputPath);
        // } finally {
        //     session.logout();
        // }
        
        log.info("（示例代码，需要实际的 Repository 对象）");
    }

    /**
     * 处理批量组件提取
     */
    private void handleBatchComponents(Map<String, String> params) 
            throws RepositoryException, IOException {
        
        String basePath = params.get("--basePath");
        String outputDir = params.getOrDefault("--outputDir", "output/components");
        
        log.info("批量提取组件: " + basePath);
        log.info("输出目录: " + outputDir);
        
        // 注意：这里需要实际的 Repository
        // Session session = repository.loginAdministrative(null);
        // try {
        //     ComponentInfoExtractor extractor = new ComponentInfoExtractor(session);
        //     ComponentExporter exporter = new ComponentExporter();
        //     
        //     List<Map<String, Object>> components = 
        //         extractor.extractComponentsFromPath(basePath);
        //     
        //     exporter.exportComponentsToJson(components, outputDir);
        //     log.info("已导出 " + components.size() + " 个组件到: " + outputDir);
        // } finally {
        //     session.logout();
        // }
        
        log.info("（示例代码，需要实际的 Repository 对象）");
    }

    /**
     * 解析命令行参数
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new java.util.HashMap<>();
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    params.put(arg, args[i + 1]);
                    i++;
                } else {
                    params.put(arg, "true");
                }
            }
        }
        
        return params;
    }

    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("组件信息提取 CLI 工具");
        System.out.println();
        System.out.println("用法:");
        System.out.println("  java ComponentInfoCLI [选项]");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  --path <路径>              单个组件路径");
        System.out.println("  --basePath <路径>          组件基础路径（批量提取）");
        System.out.println("  --output <文件>            输出文件路径（单个组件，默认: component-info.json）");
        System.out.println("  --outputDir <目录>         输出目录（批量提取，默认: output/components）");
        System.out.println("  --format <格式>            输出格式: json|pretty（默认: json）");
        System.out.println("  --simple                   只提取简化信息");
        System.out.println("  --help                     显示帮助信息");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  # 提取单个组件");
        System.out.println("  java ComponentInfoCLI --path /apps/myproject/components/mycomponent --output mycomponent.json");
        System.out.println();
        System.out.println("  # 批量提取组件");
        System.out.println("  java ComponentInfoCLI --basePath /apps/myproject/components --outputDir output/");
        System.out.println();
        System.out.println("  # 提取简化信息");
        System.out.println("  java ComponentInfoCLI --path /apps/myproject/components/mycomponent --simple --output simple.json");
    }
}

