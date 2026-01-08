#!/usr/bin/env node

/**
 * AEM 组件资源收集器
 * 
 * 用于收集 AEM 组件的完整信息，包括 HTML、Java、CSS、JS 等所有相关资源
 * 支持 React 迁移场景
 * 
 * 使用方法:
 *   node component-resource-collector.js <component-path> [options]
 * 
 * 示例:
 *   node component-resource-collector.js /apps/myapp/components/hero --output ./collected/hero
 */

const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { URL } = require('url');

// 配置
const config = {
    aemBaseUrl: process.env.AEM_HOST || 'http://localhost:4502',
    credentials: process.env.AEM_USER && process.env.AEM_PASS 
        ? `${process.env.AEM_USER}:${process.env.AEM_PASS}`
        : 'admin:admin',
    outputDir: './collected'
};

/**
 * AEM 客户端类
 */
class AEMClient {
    constructor(config) {
        this.baseUrl = config.aemBaseUrl;
        this.auth = Buffer.from(config.credentials).toString('base64');
    }

    /**
     * 获取 JSON 数据
     */
    async getJSON(urlPath) {
        const fullUrl = urlPath.startsWith('http') ? urlPath : `${this.baseUrl}${urlPath}`;
        const content = await this.getText(fullUrl);
        try {
            return JSON.parse(content);
        } catch (e) {
            throw new Error(`无法解析 JSON: ${e.message}`);
        }
    }

    /**
     * 获取文本内容
     */
    async getText(urlPath) {
        const fullUrl = urlPath.startsWith('http') ? urlPath : `${this.baseUrl}${urlPath}`;
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
                    'Accept': 'application/json,text/html,text/plain'
                },
                rejectUnauthorized: false
            };

            const req = client.request(options, (res) => {
                if (res.statusCode !== 200) {
                    reject(new Error(`HTTP ${res.statusCode}: ${res.statusMessage}`));
                    return;
                }
                
                let data = '';
                res.on('data', (chunk) => { data += chunk; });
                res.on('end', () => resolve(data));
            });

            req.on('error', reject);
            req.setTimeout(30000, () => {
                req.destroy();
                reject(new Error('请求超时'));
            });
            req.end();
        });
    }

    /**
     * 获取二进制文件
     */
    async getBinary(urlPath) {
        const fullUrl = urlPath.startsWith('http') ? urlPath : `${this.baseUrl}${urlPath}`;
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
                    'Accept': '*/*'
                },
                rejectUnauthorized: false
            };

            const chunks = [];
            const req = client.request(options, (res) => {
                if (res.statusCode !== 200) {
                    reject(new Error(`HTTP ${res.statusCode}: ${res.statusMessage}`));
                    return;
                }
                
                res.on('data', (chunk) => chunks.push(chunk));
                res.on('end', () => resolve(Buffer.concat(chunks)));
            });

            req.on('error', reject);
            req.setTimeout(30000, () => {
                req.destroy();
                reject(new Error('请求超时'));
            });
            req.end();
        });
    }
}

/**
 * 组件资源收集器
 */
class ComponentResourceCollector {
    constructor(config) {
        this.config = config;
        this.aemClient = new AEMClient(config);
        this.collectedData = {
            component: {},
            files: {
                html: [],
                java: [],
                css: [],
                js: [],
                assets: []
            },
            configurations: {},
            dependencies: {
                clientlibs: [],
                slingModels: [],
                childComponents: [],
                inheritance: []
            },
            reactMigration: {}
        };
    }

    /**
     * 收集组件信息
     */
    async collect(componentPath, options = {}) {
        console.log(`\n开始收集组件: ${componentPath}`);
        console.log('='.repeat(60));

        try {
            // 1. 收集组件基本信息
            await this.collectComponentInfo(componentPath);
            
            // 2. 收集模板文件
            await this.collectTemplateFiles(componentPath);
            
            // 3. 收集配置文件
            await this.collectConfigurations(componentPath);
            
            // 4. 分析 HTL 模板内容
            await this.analyzeHTLContent(componentPath);
            
            // 5. 收集 ClientLibs
            if (options.includeDependencies !== false) {
                await this.collectClientLibs();
            }
            
            // 6. 收集 Sling Models
            if (options.includeDependencies !== false) {
                await this.collectSlingModels();
            }
            
            // 7. 收集子组件
            if (options.recursive && options.includeDependencies !== false) {
                await this.collectChildComponents(options.depth || 3);
            }
            
            // 8. 分析继承关系
            if (options.includeDependencies !== false) {
                await this.analyzeInheritance(componentPath);
            }
            
            // 9. 收集静态资源
            await this.collectAssets();
            
            // 10. 生成 React 迁移信息
            if (options.format === 'react') {
                await this.generateReactMigrationInfo();
            }

            console.log('\n收集完成！');
            return this.collectedData;
            
        } catch (error) {
            console.error('\n收集过程中出错:', error);
            throw error;
        }
    }

    /**
     * 收集组件基本信息
     */
    async collectComponentInfo(componentPath) {
        console.log('1. 收集组件基本信息...');
        
        try {
            const contentXml = await this.aemClient.getJSON(`${componentPath}.json`);
            
            this.collectedData.component = {
                path: componentPath,
                resourceType: contentXml['sling:resourceType'] || componentPath,
                title: contentXml['jcr:title'] || null,
                description: contentXml['jcr:description'] || null,
                resourceSuperType: contentXml['sling:resourceSuperType'] || null,
                componentGroup: contentXml['componentGroup'] || null,
                isContainer: contentXml['cq:isContainer'] === true,
                allowParents: this.parseArrayProperty(contentXml['allowParents']),
                allowedChildren: this.parseArrayProperty(contentXml['allowedChildren']),
                properties: contentXml
            };
            
            console.log(`   ✓ 组件名称: ${this.collectedData.component.title || 'N/A'}`);
            console.log(`   ✓ 资源类型: ${this.collectedData.component.resourceType}`);
            
        } catch (error) {
            console.warn(`   ⚠ 无法获取组件信息: ${error.message}`);
        }
    }

    /**
     * 收集模板文件
     */
    async collectTemplateFiles(componentPath) {
        console.log('2. 收集模板文件...');
        
        const componentName = path.basename(componentPath);
        const possibleTemplates = [
            `${componentPath}/${componentName}.html`,
            `${componentPath}/template.html`,
            `${componentPath}/${componentName}.jsp`
        ];

        for (const templatePath of possibleTemplates) {
            try {
                const content = await this.aemClient.getText(templatePath);
                if (content) {
                    const templateInfo = {
                        path: templatePath,
                        type: templatePath.endsWith('.html') ? 'HTL' : 'JSP',
                        content: content,
                        size: content.length
                    };
                    
                    this.collectedData.files.html.push(templateInfo);
                    console.log(`   ✓ 找到模板: ${templatePath} (${templateInfo.type})`);
                    break;
                }
            } catch (error) {
                // 继续尝试下一个
            }
        }
        
        if (this.collectedData.files.html.length === 0) {
            console.warn('   ⚠ 未找到模板文件');
        }
    }

    /**
     * 收集配置文件
     */
    async collectConfigurations(componentPath) {
        console.log('3. 收集配置文件...');
        
        const configs = [
            { name: 'contentXml', path: `${componentPath}.json` },
            { name: 'dialog', path: `${componentPath}/_cq_dialog.json` },
            { name: 'designDialog', path: `${componentPath}/_cq_design_dialog.json` },
            { name: 'editConfig', path: `${componentPath}/_cq_editConfig.json` },
            { name: 'htmlTag', path: `${componentPath}/_cq_htmlTag.json` },
            { name: 'template', path: `${componentPath}/_cq_template.json` },
            { name: 'childEditConfig', path: `${componentPath}/_cq_childEditConfig.json` }
        ];

        for (const config of configs) {
            try {
                const data = await this.aemClient.getJSON(config.path);
                this.collectedData.configurations[config.name] = data;
                console.log(`   ✓ 收集配置: ${config.name}`);
            } catch (error) {
                // 配置文件可能不存在，这是正常的
            }
        }
    }

    /**
     * 分析 HTL 模板内容
     */
    async analyzeHTLContent(componentPath) {
        console.log('4. 分析 HTL 模板内容...');
        
        if (this.collectedData.files.html.length === 0) {
            return;
        }

        const htlContent = this.collectedData.files.html[0].content;
        
        // 解析 ClientLibs 引用
        const clientLibRegex = /clientlib\.(css|js|all)\s*@\s*categories=['"]([^'"]+)['"]/g;
        let match;
        while ((match = clientLibRegex.exec(htlContent)) !== null) {
            const type = match[1];
            const categories = match[2].split(',').map(c => c.trim());
            
            categories.forEach(category => {
                if (!this.collectedData.dependencies.clientlibs.find(c => c.category === category)) {
                    this.collectedData.dependencies.clientlibs.push({
                        category: category,
                        type: type,
                        path: null,
                        files: []
                    });
                }
            });
        }

        // 解析 Sling Model 引用
        const modelRegex = /data-sly-use\.\w+\s*=\s*['"]([^'"]+)['"]/g;
        while ((match = modelRegex.exec(htlContent)) !== null) {
            const modelClass = match[1];
            if (!this.collectedData.dependencies.slingModels.includes(modelClass)) {
                this.collectedData.dependencies.slingModels.push(modelClass);
            }
        }

        // 解析子组件引用
        const resourceRegex = /data-sly-resource\s*=\s*['"]([^'"]+)['"][^>]*resourceType=['"]([^'"]+)['"]/g;
        while ((match = resourceRegex.exec(htlContent)) !== null) {
            this.collectedData.dependencies.childComponents.push({
                path: match[1],
                resourceType: match[2]
            });
        }

        console.log(`   ✓ ClientLibs: ${this.collectedData.dependencies.clientlibs.length}`);
        console.log(`   ✓ Sling Models: ${this.collectedData.dependencies.slingModels.length}`);
        console.log(`   ✓ 子组件: ${this.collectedData.dependencies.childComponents.length}`);
    }

    /**
     * 收集 ClientLibs
     */
    async collectClientLibs() {
        console.log('5. 收集 ClientLibs...');
        
        for (const clientLib of this.collectedData.dependencies.clientlibs) {
            try {
                // 尝试查找 ClientLib 路径
                const clientLibPath = await this.findClientLibPath(clientLib.category);
                if (clientLibPath) {
                    clientLib.path = clientLibPath;
                    
                    // 收集 CSS 文件
                    const cssFiles = await this.collectClientLibFiles(clientLibPath, 'css');
                    cssFiles.forEach(file => {
                        this.collectedData.files.css.push({
                            ...file,
                            category: clientLib.category
                        });
                    });
                    
                    // 收集 JS 文件
                    const jsFiles = await this.collectClientLibFiles(clientLibPath, 'js');
                    jsFiles.forEach(file => {
                        this.collectedData.files.js.push({
                            ...file,
                            category: clientLib.category
                        });
                    });
                    
                    clientLib.files = [...cssFiles, ...jsFiles];
                    console.log(`   ✓ ${clientLib.category}: ${clientLib.files.length} 个文件`);
                }
            } catch (error) {
                console.warn(`   ⚠ 无法收集 ClientLib: ${clientLib.category} - ${error.message}`);
            }
        }
    }

    /**
     * 查找 ClientLib 路径
     */
    async findClientLibPath(category) {
        // 常见的 ClientLib 路径模式
        const possiblePaths = [
            `/apps/myapp/clientlibs/${category.replace(/\./g, '/')}`,
            `/apps/myapp/clientlibs/components/${category.split('.').pop()}`,
            `/etc/clientlibs/${category.replace(/\./g, '/')}`
        ];

        for (const path of possiblePaths) {
            try {
                await this.aemClient.getJSON(`${path}.json`);
                return path;
            } catch (error) {
                // 继续尝试下一个
            }
        }

        return null;
    }

    /**
     * 收集 ClientLib 文件
     */
    async collectClientLibFiles(clientLibPath, type) {
        const files = [];
        const dirPath = `${clientLibPath}/${type}`;
        
        try {
            // 尝试获取目录列表
            const dirContent = await this.aemClient.getJSON(`${dirPath}.json`);
            
            // 遍历目录内容
            for (const fileName in dirContent) {
                if (fileName.startsWith('jcr:') || fileName.startsWith('sling:')) {
                    continue;
                }
                
                const filePath = `${dirPath}/${fileName}`;
                try {
                    const content = await this.aemClient.getText(filePath);
                    files.push({
                        path: filePath,
                        name: fileName,
                        content: content,
                        size: content.length
                    });
                } catch (error) {
                    // 可能是目录，跳过
                }
            }
        } catch (error) {
            // 目录可能不存在
        }

        return files;
    }

    /**
     * 收集 Sling Models
     */
    async collectSlingModels() {
        console.log('6. 收集 Sling Models...');
        
        for (const modelClass of this.collectedData.dependencies.slingModels) {
            try {
                const javaFile = await this.findJavaFile(modelClass);
                if (javaFile) {
                    const content = await this.aemClient.getText(javaFile.path);
                    this.collectedData.files.java.push({
                        path: javaFile.path,
                        className: modelClass,
                        content: content,
                        size: content.length
                    });
                    console.log(`   ✓ ${modelClass}`);
                }
            } catch (error) {
                console.warn(`   ⚠ 无法找到 Sling Model: ${modelClass} - ${error.message}`);
            }
        }
    }

    /**
     * 查找 Java 文件
     */
    async findJavaFile(className) {
        // 将类名转换为文件路径
        // com.myapp.core.models.HeroModel -> 
        // /apps/myapp/core/src/main/java/com/myapp/core/models/HeroModel.java
        const parts = className.split('.');
        const fileName = parts.pop();
        const packagePath = parts.join('/');
        
        // 尝试多个可能的路径
        const possiblePaths = [
            `/apps/myapp/core/src/main/java/${packagePath}/${fileName}.java`,
            `/apps/myapp/bundle/src/main/java/${packagePath}/${fileName}.java`,
            `/apps/myapp/src/main/java/${packagePath}/${fileName}.java`
        ];

        for (const filePath of possiblePaths) {
            try {
                await this.aemClient.getText(filePath);
                return { path: filePath };
            } catch (error) {
                // 继续尝试下一个
            }
        }

        return null;
    }

    /**
     * 收集子组件（递归）
     */
    async collectChildComponents(maxDepth, currentDepth = 0) {
        if (currentDepth >= maxDepth) {
            return;
        }

        console.log(`7. 收集子组件 (深度 ${currentDepth + 1}/${maxDepth})...`);
        
        const childComponents = [...this.collectedData.dependencies.childComponents];
        
        for (const child of childComponents) {
            if (child.resourceType && !child.resourceType.startsWith('foundation/')) {
                try {
                    // 这里可以递归收集子组件
                    // 为了简化，这里只记录信息
                    console.log(`   ✓ ${child.resourceType}`);
                } catch (error) {
                    console.warn(`   ⚠ 无法收集子组件: ${child.resourceType}`);
                }
            }
        }
    }

    /**
     * 分析继承关系
     */
    async analyzeInheritance(componentPath) {
        console.log('8. 分析继承关系...');
        
        const inheritanceChain = [];
        let currentPath = componentPath;
        
        while (currentPath) {
            try {
                const contentXml = await this.aemClient.getJSON(`${currentPath}.json`);
                inheritanceChain.push({
                    path: currentPath,
                    title: contentXml['jcr:title'],
                    resourceSuperType: contentXml['sling:resourceSuperType']
                });
                
                currentPath = contentXml['sling:resourceSuperType'];
                if (!currentPath || currentPath.startsWith('foundation/')) {
                    break;
                }
            } catch (error) {
                break;
            }
        }
        
        this.collectedData.dependencies.inheritance = inheritanceChain;
        console.log(`   ✓ 继承链: ${inheritanceChain.length} 层`);
    }

    /**
     * 收集静态资源
     */
    async collectAssets() {
        console.log('9. 收集静态资源...');
        
        // 从 HTL 模板中提取资源引用
        if (this.collectedData.files.html.length > 0) {
            const htlContent = this.collectedData.files.html[0].content;
            const imageRegex = /(?:src|data-src)=['"]([^'"]+\.(jpg|png|gif|svg|webp))['"]/gi;
            let match;
            const images = new Set();
            
            while ((match = imageRegex.exec(htlContent)) !== null) {
                images.add(match[1]);
            }
            
            // 下载资源文件（可选）
            // for (const imagePath of images) {
            //     try {
            //         const imageData = await this.aemClient.getBinary(imagePath);
            //         this.collectedData.files.assets.push({
            //             path: imagePath,
            //             type: 'image',
            //             size: imageData.length
            //         });
            //     } catch (error) {
            //         // 跳过无法下载的资源
            //     }
            // }
            
            console.log(`   ✓ 找到 ${images.size} 个资源引用`);
        }
    }

    /**
     * 生成 React 迁移信息
     */
    async generateReactMigrationInfo() {
        console.log('10. 生成 React 迁移信息...');
        
        const component = this.collectedData.component;
        const dialog = this.collectedData.configurations.dialog;
        
        // 提取 Props 定义
        const props = {};
        if (dialog) {
            props = this.extractPropsFromDialog(dialog);
        }
        
        // 生成组件路径
        const componentName = path.basename(component.path);
        const reactComponentPath = `src/components/${componentName}/${componentName}.tsx`;
        
        this.collectedData.reactMigration = {
            componentPath: reactComponentPath,
            props: props,
            styles: {
                cssPath: `src/components/${componentName}/${componentName}.module.css`,
                cssFiles: this.collectedData.files.css.map(f => f.path)
            },
            dependencies: {
                childComponents: this.collectedData.dependencies.childComponents.map(c => c.resourceType),
                models: this.collectedData.dependencies.slingModels
            }
        };
        
        console.log('   ✓ React 迁移信息已生成');
    }

    /**
     * 从对话框提取 Props
     */
    extractPropsFromDialog(dialog) {
        const props = {};
        
        function traverse(node, path = '') {
            if (node.name && node['sling:resourceType']) {
                // 这是一个字段节点
                const fieldName = node.name.replace('./', '');
                props[fieldName] = {
                    type: this.inferTypeFromFieldType(node['sling:resourceType']),
                    required: node.required === true,
                    description: node.fieldDescription || node.fieldLabel,
                    defaultValue: node.value || null
                };
            }
            
            // 递归遍历子节点
            if (node.items) {
                Object.keys(node.items).forEach(key => {
                    traverse(node.items[key], path ? `${path}.${key}` : key);
                });
            }
        }
        
        traverse(dialog);
        return props;
    }

    /**
     * 从字段类型推断 TypeScript 类型
     */
    inferTypeFromFieldType(fieldType) {
        const typeMap = {
            'textfield': 'string',
            'textarea': 'string',
            'numberfield': 'number',
            'checkbox': 'boolean',
            'pathfield': 'string',
            'select': 'string'
        };
        
        for (const [key, value] of Object.entries(typeMap)) {
            if (fieldType.includes(key)) {
                return value;
            }
        }
        
        return 'string'; // 默认类型
    }

    /**
     * 解析数组属性
     */
    parseArrayProperty(value) {
        if (!value) return [];
        if (Array.isArray(value)) return value;
        if (typeof value === 'string') {
            // 处理 "[item1, item2]" 格式
            const match = value.match(/\[(.*?)\]/);
            if (match) {
                return match[1].split(',').map(s => s.trim()).filter(s => s);
            }
            return [value];
        }
        return [];
    }
}

// 命令行接口
async function main() {
    const args = process.argv.slice(2);
    
    if (args.length === 0) {
        console.log(`
使用方法:
  node component-resource-collector.js <component-path> [options]

参数:
  component-path    组件路径，例如: /apps/myapp/components/hero

选项:
  --output <dir>          输出目录 (默认: ./collected)
  --format <format>       输出格式: json, react (默认: json)
  --recursive             递归收集子组件
  --depth <number>        递归深度 (默认: 3)
  --include-dependencies  包含依赖 (默认: true)
  --no-dependencies       不包含依赖

环境变量:
  AEM_HOST          AEM 主机地址 (默认: http://localhost:4502)
  AEM_USER          AEM 用户名 (默认: admin)
  AEM_PASS          AEM 密码 (默认: admin)

示例:
  node component-resource-collector.js /apps/myapp/components/hero --output ./collected/hero
  node component-resource-collector.js /apps/myapp/components/hero --format react --recursive
        `);
        process.exit(1);
    }

    const componentPath = args[0];
    const outputIndex = args.indexOf('--output');
    const formatIndex = args.indexOf('--format');
    const recursive = args.includes('--recursive');
    const depthIndex = args.indexOf('--depth');
    const includeDependencies = !args.includes('--no-dependencies');

    const outputDir = outputIndex >= 0 && args[outputIndex + 1] 
        ? args[outputIndex + 1] 
        : config.outputDir;
    const format = formatIndex >= 0 && args[formatIndex + 1] 
        ? args[formatIndex + 1] 
        : 'json';
    const depth = depthIndex >= 0 && parseInt(args[depthIndex + 1]) 
        ? parseInt(args[depthIndex + 1]) 
        : 3;

    console.log(`\n组件资源收集器`);
    console.log('='.repeat(60));
    console.log(`组件路径: ${componentPath}`);
    console.log(`输出目录: ${outputDir}`);
    console.log(`输出格式: ${format}`);
    console.log(`递归收集: ${recursive ? '是' : '否'}`);
    console.log(`包含依赖: ${includeDependencies ? '是' : '否'}`);

    // 确保输出目录存在
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }

    const collector = new ComponentResourceCollector(config);
    
    try {
        const collectedData = await collector.collect(componentPath, {
            format: format,
            recursive: recursive,
            depth: depth,
            includeDependencies: includeDependencies
        });
        
        // 保存结果
        const outputFile = path.join(outputDir, 'collected-data.json');
        fs.writeFileSync(outputFile, JSON.stringify(collectedData, null, 2));
        console.log(`\n结果已保存到: ${outputFile}`);
        
    } catch (error) {
        console.error('\n收集失败:', error);
        process.exit(1);
    }
}

// 如果直接运行此脚本
if (require.main === module) {
    main();
}

module.exports = { ComponentResourceCollector, AEMClient };

