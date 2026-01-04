#!/usr/bin/env node

/**
 * AEM 组件依赖分析工具
 * 
 * 用于分析 AEM 组件的完整依赖信息，支持 React 迁移场景
 * 
 * 使用方法:
 *   node component-dependency-analyzer.js <component-path> [options]
 * 
 * 示例:
 *   node component-dependency-analyzer.js /apps/myapp/components/card --output deps.json
 */

const fs = require('fs');
const path = require('path');

// 配置
const config = {
    aemBaseUrl: process.env.AEM_HOST || 'http://localhost:4502',
    credentials: process.env.AEM_USER && process.env.AEM_PASS 
        ? `${process.env.AEM_USER}:${process.env.AEM_PASS}`
        : 'admin:admin',
    outputDir: './component-dependencies'
};

class ComponentDependencyAnalyzer {
    constructor(config) {
        this.config = config;
        this.baseUrl = config.aemBaseUrl;
        this.auth = Buffer.from(config.credentials).toString('base64');
    }

    /**
     * 分析组件依赖
     */
    async analyzeComponent(componentPath) {
        console.log(`分析组件: ${componentPath}`);
        
        const deps = {
            component: {
                resourceType: componentPath,
                path: componentPath,
                analyzedAt: new Date().toISOString()
            },
            clientlibs: {
                categories: [],
                css: [],
                js: [],
                dependencies: {}
            },
            slingModels: [],
            childComponents: [],
            dialog: null,
            properties: {},
            templates: [],
            assets: []
        };

        try {
            // 1. 获取组件基本信息
            await this.analyzeComponentInfo(componentPath, deps);
            
            // 2. 分析 HTL 模板
            await this.analyzeHTLTemplate(componentPath, deps);
            
            // 3. 分析 ClientLibs
            await this.analyzeClientLibs(deps);
            
            // 4. 分析对话框
            await this.analyzeDialog(componentPath, deps);
            
            // 5. 分析子组件（递归）
            await this.analyzeChildComponents(deps);
            
            console.log('分析完成！');
            return deps;
            
        } catch (error) {
            console.error('分析过程中出错:', error);
            throw error;
        }
    }

    /**
     * 分析组件基本信息
     */
    async analyzeComponentInfo(componentPath, deps) {
        try {
            const url = `${this.baseUrl}${componentPath}.json`;
            const response = await this.fetchJSON(url);
            
            deps.component.resourceSuperType = response['sling:resourceSuperType'] || null;
            deps.component.title = response['jcr:title'] || null;
            deps.component.description = response['jcr:description'] || null;
            deps.properties = response;
        } catch (error) {
            console.warn(`无法获取组件信息: ${componentPath}`, error.message);
        }
    }

    /**
     * 分析 HTL 模板
     */
    async analyzeHTLTemplate(componentPath, deps) {
        const componentName = path.basename(componentPath);
        const possibleTemplates = [
            `${componentPath}/${componentName}.html`,
            `${componentPath}/template.html`,
            `${componentPath}/${componentName}.jsp`
        ];

        for (const templatePath of possibleTemplates) {
            try {
                const content = await this.fetchText(templatePath);
                deps.component.template = {
                    path: templatePath,
                    type: templatePath.endsWith('.html') ? 'HTL' : 'JSP',
                    content: content
                };
                
                this.parseHTLContent(content, deps);
                break;
            } catch (error) {
                // 继续尝试下一个
            }
        }
    }

    /**
     * 解析 HTL 模板内容
     */
    parseHTLContent(content, deps) {
        // 解析 ClientLib categories
        const clientLibRegex = /clientlib\.(css|js|all)\s*@\s*categories=['"]([^'"]+)['"]/g;
        let match;
        while ((match = clientLibRegex.exec(content)) !== null) {
            const type = match[1];
            const categories = match[2].split(',').map(c => c.trim());
            
            categories.forEach(category => {
                if (!deps.clientlibs.categories.includes(category)) {
                    deps.clientlibs.categories.push(category);
                }
                
                // 记录类型
                if (type === 'css' || type === 'all') {
                    if (!deps.clientlibs.css.find(c => c.category === category)) {
                        deps.clientlibs.css.push({ category, files: [] });
                    }
                }
                if (type === 'js' || type === 'all') {
                    if (!deps.clientlibs.js.find(j => j.category === category)) {
                        deps.clientlibs.js.push({ category, files: [] });
                    }
                }
            });
        }

        // 解析 Sling Models
        const modelRegex = /data-sly-use\.\w+\s*=\s*['"]([^'"]+)['"]/g;
        while ((match = modelRegex.exec(content)) !== null) {
            const modelClass = match[1];
            if (!deps.slingModels.includes(modelClass)) {
                deps.slingModels.push(modelClass);
            }
        }

        // 解析子组件
        const resourceRegex = /data-sly-resource\s*=\s*['"]([^'"]+)['"][^>]*resourceType=['"]([^'"]+)['"]/g;
        while ((match = resourceRegex.exec(content)) !== null) {
            deps.childComponents.push({
                path: match[1],
                resourceType: match[2]
            });
        }

        // 解析模板调用
        const templateRegex = /data-sly-call\s*=\s*\${\s*(\w+)\.(\w+)\s*@/g;
        while ((match = templateRegex.exec(content)) !== null) {
            deps.templates.push({
                object: match[1],
                method: match[2]
            });
        }
    }

    /**
     * 分析 ClientLibs
     */
    async analyzeClientLibs(deps) {
        const processed = new Set();
        const queue = [...deps.clientlibs.categories];

        while (queue.length > 0) {
            const category = queue.shift();
            if (processed.has(category)) continue;
            processed.add(category);

            try {
                const clientLibInfo = await this.findClientLib(category);
                if (clientLibInfo) {
                    // 更新 CSS 文件列表
                    const cssEntry = deps.clientlibs.css.find(c => c.category === category);
                    if (cssEntry) {
                        cssEntry.files = clientLibInfo.cssFiles;
                        cssEntry.path = clientLibInfo.path;
                    }

                    // 更新 JS 文件列表
                    const jsEntry = deps.clientlibs.js.find(j => j.category === category);
                    if (jsEntry) {
                        jsEntry.files = clientLibInfo.jsFiles;
                        jsEntry.path = clientLibInfo.path;
                    }

                    // 处理依赖
                    if (clientLibInfo.dependencies && clientLibInfo.dependencies.length > 0) {
                        deps.clientlibs.dependencies[category] = clientLibInfo.dependencies;
                        queue.push(...clientLibInfo.dependencies);
                    }
                }
            } catch (error) {
                console.warn(`无法分析 ClientLib: ${category}`, error.message);
            }
        }
    }

    /**
     * 查找 ClientLib 信息
     */
    async findClientLib(category) {
        // 这里需要实现实际的 ClientLib 查找逻辑
        // 可以通过 JCR 查询或 API 调用
        
        // 示例：通过代理路径获取信息
        try {
            // 尝试获取 ClientLib 信息（如果 AEM 提供了相关 API）
            const proxyPath = `/etc.clientlibs`;
            // 实际实现需要根据 AEM 版本和配置调整
            
            return {
                path: `/apps/myapp/clientlibs/${category}`,
                cssFiles: [],
                jsFiles: [],
                dependencies: []
            };
        } catch (error) {
            return null;
        }
    }

    /**
     * 分析对话框配置
     */
    async analyzeDialog(componentPath, deps) {
        try {
            const dialogPath = `${componentPath}/_cq_dialog.json`;
            deps.dialog = await this.fetchJSON(dialogPath);
        } catch (error) {
            // 对话框可能不存在，这是正常的
        }
    }

    /**
     * 分析子组件（递归）
     */
    async analyzeChildComponents(deps) {
        for (const child of deps.childComponents) {
            if (child.resourceType && !child.resourceType.startsWith('foundation/')) {
                try {
                    // 递归分析子组件
                    const childDeps = await this.analyzeComponent(child.resourceType);
                    child.dependencies = childDeps;
                } catch (error) {
                    console.warn(`无法分析子组件: ${child.resourceType}`, error.message);
                }
            }
        }
    }

    /**
     * 获取 JSON 数据
     */
    async fetchJSON(url) {
        const fullUrl = url.startsWith('http') ? url : `${this.baseUrl}${url}`;
        
        // 注意：在实际环境中需要使用 fetch 或 axios
        // 这里使用示例代码
        const https = require('https');
        const http = require('http');
        const urlObj = new URL(fullUrl);
        const client = urlObj.protocol === 'https:' ? https : http;
        
        return new Promise((resolve, reject) => {
            const options = {
                hostname: urlObj.hostname,
                port: urlObj.port || (urlObj.protocol === 'https:' ? 443 : 80),
                path: urlObj.pathname + urlObj.search,
                method: 'GET',
                headers: {
                    'Authorization': `Basic ${this.auth}`,
                    'Accept': 'application/json'
                }
            };

            const req = client.request(options, (res) => {
                let data = '';
                res.on('data', (chunk) => { data += chunk; });
                res.on('end', () => {
                    try {
                        resolve(JSON.parse(data));
                    } catch (e) {
                        reject(new Error(`无法解析 JSON: ${e.message}`));
                    }
                });
            });

            req.on('error', reject);
            req.end();
        });
    }

    /**
     * 获取文本内容
     */
    async fetchText(url) {
        const fullUrl = url.startsWith('http') ? url : `${this.baseUrl}${url}`;
        const https = require('https');
        const http = require('http');
        const urlObj = new URL(fullUrl);
        const client = urlObj.protocol === 'https:' ? https : http;
        
        return new Promise((resolve, reject) => {
            const options = {
                hostname: urlObj.hostname,
                port: urlObj.port || (urlObj.protocol === 'https:' ? 443 : 80),
                path: urlObj.pathname + urlObj.search,
                method: 'GET',
                headers: {
                    'Authorization': `Basic ${this.auth}`,
                    'Accept': 'text/html,text/plain'
                }
            };

            const req = client.request(options, (res) => {
                let data = '';
                res.on('data', (chunk) => { data += chunk; });
                res.on('end', () => resolve(data));
            });

            req.on('error', reject);
            req.end();
        });
    }

    /**
     * 生成 React 迁移报告
     */
    generateReactMigrationReport(deps) {
        const report = {
            component: deps.component.resourceType,
            reactStructure: {
                componentPath: this.toReactComponentPath(deps.component.resourceType),
                stylesPath: this.toReactStylesPath(deps.component.resourceType),
                modelPath: this.toReactModelPath(deps.slingModels[0]),
                assetsPath: this.toReactAssetsPath(deps.component.resourceType)
            },
            dependencies: {
                cssFiles: deps.clientlibs.css.flatMap(c => c.files),
                jsFiles: deps.clientlibs.js.flatMap(j => j.files),
                models: deps.slingModels,
                childComponents: deps.childComponents.map(c => c.resourceType)
            },
            migrationSteps: this.generateMigrationSteps(deps)
        };

        return report;
    }

    toReactComponentPath(resourceType) {
        const parts = resourceType.split('/').slice(1); // 移除 'apps'
        return `src/components/${parts.join('/')}`;
    }

    toReactStylesPath(resourceType) {
        return `${this.toReactComponentPath(resourceType)}/Component.module.css`;
    }

    toReactModelPath(modelClass) {
        if (!modelClass) return null;
        const className = modelClass.split('.').pop();
        return `src/models/${className}.js`;
    }

    toReactAssetsPath(resourceType) {
        return `${this.toReactComponentPath(resourceType)}/assets`;
    }

    generateMigrationSteps(deps) {
        const steps = [];
        
        steps.push('1. 创建 React 组件目录结构');
        steps.push('2. 迁移样式文件 (CSS)');
        
        if (deps.slingModels.length > 0) {
            steps.push('3. 创建数据模型 (从 Sling Model 迁移)');
        }
        
        steps.push('4. 实现 React 组件逻辑');
        steps.push('5. 处理子组件集成');
        
        if (deps.dialog) {
            steps.push('6. 迁移对话框配置为组件 Props');
        }
        
        steps.push('7. 测试和验证');
        
        return steps;
    }
}

// 命令行接口
async function main() {
    const args = process.argv.slice(2);
    
    if (args.length === 0) {
        console.log(`
使用方法:
  node component-dependency-analyzer.js <component-path> [options]

参数:
  component-path    组件路径，例如: /apps/myapp/components/card

选项:
  --output <file>   输出文件路径 (默认: component-dependencies.json)
  --format <format> 输出格式: json, react (默认: json)
  --recursive        递归分析子组件

环境变量:
  AEM_HOST          AEM 主机地址 (默认: http://localhost:4502)
  AEM_USER          AEM 用户名 (默认: admin)
  AEM_PASS          AEM 密码 (默认: admin)

示例:
  node component-dependency-analyzer.js /apps/myapp/components/card --output card-deps.json
  node component-dependency-analyzer.js /apps/myapp/components/card --format react
        `);
        process.exit(1);
    }

    const componentPath = args[0];
    const outputIndex = args.indexOf('--output');
    const formatIndex = args.indexOf('--format');
    const recursive = args.includes('--recursive');

    const outputFile = outputIndex >= 0 && args[outputIndex + 1] 
        ? args[outputIndex + 1] 
        : 'component-dependencies.json';
    const format = formatIndex >= 0 && args[formatIndex + 1] 
        ? args[formatIndex + 1] 
        : 'json';

    console.log(`开始分析组件: ${componentPath}`);
    console.log(`输出文件: ${outputFile}`);
    console.log(`格式: ${format}`);

    const analyzer = new ComponentDependencyAnalyzer(config);
    
    try {
        const deps = await analyzer.analyzeComponent(componentPath);
        
        let output;
        if (format === 'react') {
            output = analyzer.generateReactMigrationReport(deps);
        } else {
            output = deps;
        }

        fs.writeFileSync(outputFile, JSON.stringify(output, null, 2));
        console.log(`\n分析完成！结果已保存到: ${outputFile}`);
        
    } catch (error) {
        console.error('分析失败:', error);
        process.exit(1);
    }
}

// 如果直接运行此脚本
if (require.main === module) {
    main();
}

module.exports = ComponentDependencyAnalyzer;

