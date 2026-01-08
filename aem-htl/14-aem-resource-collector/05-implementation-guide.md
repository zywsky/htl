# 实现指南：如何更好地收集 AEM 组件信息

## 目录
1. [收集策略深度分析](#收集策略深度分析)
2. [关键技术实现](#关键技术实现)
3. [优化方案](#优化方案)
4. [最佳实践](#最佳实践)
5. [常见问题解决](#常见问题解决)

---

## 收集策略深度分析

### 策略 1: 多路径收集（推荐）

**问题**: AEM 中同一个资源可能有多个访问路径

**解决方案**: 尝试多个可能的路径

```javascript
async function collectWithMultiplePaths(componentPath) {
    const possiblePaths = [
        // 标准路径
        componentPath,
        // 相对路径
        componentPath.replace('/apps/', ''),
        // 资源类型路径
        componentPath.replace('/components/', '/components/')
    ];
    
    for (const path of possiblePaths) {
        try {
            const data = await aemClient.getJSON(`${path}.json`);
            return { path: path, data: data };
        } catch (error) {
            // 继续尝试下一个路径
        }
    }
    
    throw new Error('无法找到组件');
}
```

### 策略 2: 缓存机制

**问题**: 重复收集相同资源浪费时间和资源

**解决方案**: 实现缓存机制

```javascript
class ResourceCache {
    constructor() {
        this.cache = new Map();
        this.maxSize = 1000; // 最大缓存数量
    }
    
    get(key) {
        return this.cache.get(key);
    }
    
    set(key, value) {
        if (this.cache.size >= this.maxSize) {
            // 删除最旧的缓存
            const firstKey = this.cache.keys().next().value;
            this.cache.delete(firstKey);
        }
        this.cache.set(key, value);
    }
    
    async getOrFetch(key, fetchFn) {
        if (this.cache.has(key)) {
            return this.cache.get(key);
        }
        
        const value = await fetchFn();
        this.set(key, value);
        return value;
    }
}

// 使用缓存
const cache = new ResourceCache();

async function collectComponent(componentPath) {
    return await cache.getOrFetch(componentPath, async () => {
        return await aemClient.getJSON(`${componentPath}.json`);
    });
}
```

### 策略 3: 并行收集

**问题**: 顺序收集速度慢

**解决方案**: 并行收集多个资源

```javascript
async function collectInParallel(componentPath) {
    const tasks = [
        collectComponentInfo(componentPath),
        collectTemplateFiles(componentPath),
        collectConfigurations(componentPath),
        collectClientLibs(componentPath)
    ];
    
    const results = await Promise.allSettled(tasks);
    
    return {
        componentInfo: results[0].status === 'fulfilled' ? results[0].value : null,
        templates: results[1].status === 'fulfilled' ? results[1].value : [],
        configs: results[2].status === 'fulfilled' ? results[2].value : {},
        clientlibs: results[3].status === 'fulfilled' ? results[3].value : []
    };
}
```

### 策略 4: 增量收集

**问题**: 重新收集已收集的组件浪费时间

**解决方案**: 支持增量收集

```javascript
class IncrementalCollector {
    constructor(outputDir) {
        this.outputDir = outputDir;
        this.collectedComponents = new Set();
        this.loadCollectedComponents();
    }
    
    loadCollectedComponents() {
        const indexFile = path.join(this.outputDir, 'collected-index.json');
        if (fs.existsSync(indexFile)) {
            const index = JSON.parse(fs.readFileSync(indexFile, 'utf8'));
            index.components.forEach(c => this.collectedComponents.add(c.path));
        }
    }
    
    async collect(componentPath, options = {}) {
        // 检查是否已收集
        if (!options.force && this.collectedComponents.has(componentPath)) {
            console.log(`跳过已收集的组件: ${componentPath}`);
            return this.loadCollectedData(componentPath);
        }
        
        // 收集组件
        const data = await this.doCollect(componentPath);
        
        // 保存数据
        await this.saveCollectedData(componentPath, data);
        
        // 更新索引
        this.collectedComponents.add(componentPath);
        await this.saveIndex();
        
        return data;
    }
    
    loadCollectedData(componentPath) {
        const componentName = path.basename(componentPath);
        const dataFile = path.join(this.outputDir, componentName, 'collected-data.json');
        return JSON.parse(fs.readFileSync(dataFile, 'utf8'));
    }
}
```

---

## 关键技术实现

### 1. JCR 查询

**用途**: 查找 ClientLibs、组件等资源

```javascript
class JCRQuery {
    constructor(aemClient) {
        this.aemClient = aemClient;
    }
    
    /**
     * 查询 ClientLibs
     */
    async findClientLibs(category) {
        // 方法 1: 使用 JCR SQL2 查询
        const query = `
            SELECT * FROM [cq:ClientLibraryFolder] 
            WHERE [categories] = '${category}'
        `;
        
        try {
            const results = await this.aemClient.query(query);
            return results;
        } catch (error) {
            // 方法 2: 尝试常见路径
            return await this.findClientLibByPath(category);
        }
    }
    
    /**
     * 通过路径查找 ClientLib
     */
    async findClientLibByPath(category) {
        const possiblePaths = [
            `/apps/myapp/clientlibs/${category.replace(/\./g, '/')}`,
            `/apps/myapp/clientlibs/components/${category.split('.').pop()}`,
            `/etc/clientlibs/${category.replace(/\./g, '/')}`
        ];
        
        for (const path of possiblePaths) {
            try {
                const data = await this.aemClient.getJSON(`${path}.json`);
                if (data['jcr:primaryType'] === 'cq:ClientLibraryFolder') {
                    return [{ path: path, ...data }];
                }
            } catch (error) {
                // 继续尝试下一个路径
            }
        }
        
        return [];
    }
    
    /**
     * 查询组件
     */
    async findComponents(resourceType) {
        const query = `
            SELECT * FROM [cq:Component] 
            WHERE [sling:resourceType] = '${resourceType}'
        `;
        
        return await this.aemClient.query(query);
    }
}
```

### 2. HTL 模板解析

**用途**: 提取模板中的依赖关系

```javascript
class HTLParser {
    /**
     * 解析 HTL 模板
     */
    parse(htlContent) {
        return {
            clientlibs: this.extractClientLibs(htlContent),
            slingModels: this.extractSlingModels(htlContent),
            childComponents: this.extractChildComponents(htlContent),
            resources: this.extractResources(htlContent),
            templates: this.extractTemplates(htlContent)
        };
    }
    
    /**
     * 提取 ClientLibs
     */
    extractClientLibs(content) {
        const clientlibs = [];
        
        // 匹配 clientlib.css/js/all @ categories='...'
        const regex = /clientlib\.(css|js|all)\s*@\s*categories=['"]([^'"]+)['"]/g;
        let match;
        
        while ((match = regex.exec(content)) !== null) {
            const type = match[1];
            const categories = match[2].split(',').map(c => c.trim());
            
            categories.forEach(category => {
                clientlibs.push({
                    type: type,
                    category: category,
                    line: this.getLineNumber(content, match.index)
                });
            });
        }
        
        return clientlibs;
    }
    
    /**
     * 提取 Sling Models
     */
    extractSlingModels(content) {
        const models = [];
        
        // 匹配 data-sly-use.xxx="com.xxx.Model"
        const regex = /data-sly-use\.\w+\s*=\s*['"]([^'"]+)['"]/g;
        let match;
        
        while ((match = regex.exec(content)) !== null) {
            models.push({
                className: match[1],
                line: this.getLineNumber(content, match.index)
            });
        }
        
        return models;
    }
    
    /**
     * 提取子组件
     */
    extractChildComponents(content) {
        const components = [];
        
        // 匹配 data-sly-resource="..." resourceType="..."
        const regex = /data-sly-resource\s*=\s*['"]([^'"]+)['"][^>]*resourceType=['"]([^'"]+)['"]/g;
        let match;
        
        while ((match = regex.exec(content)) !== null) {
            components.push({
                path: match[1],
                resourceType: match[2],
                line: this.getLineNumber(content, match.index)
            });
        }
        
        return components;
    }
    
    /**
     * 提取资源引用
     */
    extractResources(content) {
        const resources = [];
        
        // 匹配图片
        const imageRegex = /(?:src|data-src)=['"]([^'"]+\.(jpg|png|gif|svg|webp))['"]/gi;
        let match;
        while ((match = imageRegex.exec(content)) !== null) {
            resources.push({
                type: 'image',
                path: match[1],
                line: this.getLineNumber(content, match.index)
            });
        }
        
        // 匹配其他资源
        const resourceRegex = /(?:href|src)=['"]([^'"]+\.(css|js|woff|woff2|ttf|otf))['"]/gi;
        while ((match = resourceRegex.exec(content)) !== null) {
            resources.push({
                type: match[2],
                path: match[1],
                line: this.getLineNumber(content, match.index)
            });
        }
        
        return resources;
    }
    
    /**
     * 提取模板调用
     */
    extractTemplates(content) {
        const templates = [];
        
        // 匹配 data-sly-call="${xxx.yyy @ ...}"
        const regex = /data-sly-call\s*=\s*\${\s*(\w+)\.(\w+)\s*@/g;
        let match;
        
        while ((match = regex.exec(content)) !== null) {
            templates.push({
                object: match[1],
                method: match[2],
                line: this.getLineNumber(content, match.index)
            });
        }
        
        return templates;
    }
    
    /**
     * 获取行号
     */
    getLineNumber(content, index) {
        return content.substring(0, index).split('\n').length;
    }
}
```

### 3. 对话框配置解析

**用途**: 提取组件 Props 定义

```javascript
class DialogParser {
    /**
     * 解析对话框配置
     */
    parse(dialogConfig) {
        const fields = [];
        this.traverse(dialogConfig, fields);
        return fields;
    }
    
    /**
     * 遍历对话框节点
     */
    traverse(node, fields, path = '') {
        // 检查是否是字段节点
        if (node.name && node['sling:resourceType']) {
            const field = {
                name: node.name.replace('./', ''),
                label: node.fieldLabel || node.name,
                description: node.fieldDescription || '',
                type: this.inferFieldType(node['sling:resourceType']),
                required: node.required === true,
                defaultValue: node.value || null,
                options: this.extractOptions(node),
                validation: this.extractValidation(node),
                path: path
            };
            
            fields.push(field);
        }
        
        // 递归遍历子节点
        if (node.items) {
            Object.keys(node.items).forEach(key => {
                this.traverse(
                    node.items[key],
                    fields,
                    path ? `${path}.${key}` : key
                );
            });
        }
    }
    
    /**
     * 推断字段类型
     */
    inferFieldType(resourceType) {
        const typeMap = {
            'textfield': 'string',
            'textarea': 'string',
            'numberfield': 'number',
            'checkbox': 'boolean',
            'pathfield': 'string',
            'select': 'string',
            'datepicker': 'string',
            'colorfield': 'string',
            'richtext': 'string'
        };
        
        for (const [key, value] of Object.entries(typeMap)) {
            if (resourceType.includes(key)) {
                return value;
            }
        }
        
        return 'string';
    }
    
    /**
     * 提取选项（用于 select 字段）
     */
    extractOptions(node) {
        if (node.options) {
            return node.options.map(opt => ({
                text: opt.text || opt.value,
                value: opt.value
            }));
        }
        return null;
    }
    
    /**
     * 提取验证规则
     */
    extractValidation(node) {
        const validation = {};
        
        if (node.required === true) {
            validation.required = true;
        }
        
        if (node.validation) {
            validation.rules = node.validation;
        }
        
        return Object.keys(validation).length > 0 ? validation : null;
    }
}
```

### 4. Java 类解析

**用途**: 提取 Sling Model 的结构

```javascript
class JavaParser {
    /**
     * 解析 Java 类
     */
    parse(javaContent) {
        return {
            package: this.extractPackage(javaContent),
            className: this.extractClassName(javaContent),
            imports: this.extractImports(javaContent),
            annotations: this.extractAnnotations(javaContent),
            fields: this.extractFields(javaContent),
            methods: this.extractMethods(javaContent),
            interfaces: this.extractInterfaces(javaContent)
        };
    }
    
    /**
     * 提取包名
     */
    extractPackage(content) {
        const match = content.match(/package\s+([\w.]+);/);
        return match ? match[1] : null;
    }
    
    /**
     * 提取类名
     */
    extractClassName(content) {
        const match = content.match(/(?:public\s+)?(?:class|interface)\s+(\w+)/);
        return match ? match[1] : null;
    }
    
    /**
     * 提取导入语句
     */
    extractImports(content) {
        const imports = [];
        const regex = /import\s+([\w.]+);/g;
        let match;
        
        while ((match = regex.exec(content)) !== null) {
            imports.push(match[1]);
        }
        
        return imports;
    }
    
    /**
     * 提取注解
     */
    extractAnnotations(content) {
        const annotations = [];
        const regex = /@(\w+)(?:\([^)]*\))?/g;
        let match;
        
        while ((match = regex.exec(content)) !== null) {
            annotations.push({
                name: match[1],
                full: match[0]
            });
        }
        
        return annotations;
    }
    
    /**
     * 提取字段
     */
    extractFields(content) {
        const fields = [];
        const regex = /(?:@[\w.()\s,]*\n\s*)?(?:private|protected|public)\s+([\w.<>]+)\s+(\w+);/g;
        let match;
        
        while ((match = regex.exec(content)) !== null) {
            fields.push({
                type: match[1],
                name: match[2]
            });
        }
        
        return fields;
    }
    
    /**
     * 提取方法
     */
    extractMethods(content) {
        const methods = [];
        const regex = /(?:@[\w.()\s,]*\n\s*)?(?:public|private|protected)\s+([\w.<>]+)\s+(\w+)\s*\([^)]*\)/g;
        let match;
        
        while ((match = regex.exec(content)) !== null) {
            methods.push({
                returnType: match[1],
                name: match[2]
            });
        }
        
        return methods;
    }
    
    /**
     * 提取接口
     */
    extractInterfaces(content) {
        const interfaces = [];
        const regex = /implements\s+([\w.\s,]+)/;
        const match = content.match(regex);
        
        if (match) {
            interfaces.push(...match[1].split(',').map(i => i.trim()));
        }
        
        return interfaces;
    }
}
```

---

## 优化方案

### 1. 错误处理和重试

```javascript
class RetryableCollector {
    constructor(aemClient, maxRetries = 3) {
        this.aemClient = aemClient;
        this.maxRetries = maxRetries;
    }
    
    async collectWithRetry(componentPath) {
        let lastError;
        
        for (let i = 0; i < this.maxRetries; i++) {
            try {
                return await this.aemClient.getJSON(`${componentPath}.json`);
            } catch (error) {
                lastError = error;
                
                // 等待后重试
                if (i < this.maxRetries - 1) {
                    await this.sleep(1000 * (i + 1)); // 指数退避
                }
            }
        }
        
        throw lastError;
    }
    
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}
```

### 2. 进度跟踪

```javascript
class ProgressTracker {
    constructor(total) {
        this.total = total;
        this.current = 0;
        this.startTime = Date.now();
    }
    
    update(current) {
        this.current = current;
        this.log();
    }
    
    increment() {
        this.current++;
        this.log();
    }
    
    log() {
        const percentage = ((this.current / this.total) * 100).toFixed(1);
        const elapsed = ((Date.now() - this.startTime) / 1000).toFixed(1);
        const estimated = this.current > 0 
            ? ((Date.now() - this.startTime) / this.current * (this.total - this.current) / 1000).toFixed(1)
            : '?';
        
        process.stdout.write(`\r进度: ${this.current}/${this.total} (${percentage}%) | 已用: ${elapsed}s | 预计: ${estimated}s`);
    }
    
    complete() {
        console.log('\n完成！');
    }
}
```

### 3. 数据验证

```javascript
class DataValidator {
    validateComponentData(data) {
        const errors = [];
        
        // 验证必需字段
        if (!data.component || !data.component.path) {
            errors.push('缺少组件路径');
        }
        
        // 验证文件完整性
        if (data.files.html.length === 0) {
            errors.push('缺少 HTL 模板文件');
        }
        
        // 验证配置完整性
        if (!data.configurations.contentXml) {
            errors.push('缺少组件定义文件');
        }
        
        return {
            valid: errors.length === 0,
            errors: errors
        };
    }
}
```

---

## 最佳实践

### 1. 收集顺序

**推荐顺序**:
1. 组件基本信息（.content.xml）
2. HTL 模板文件
3. 配置文件（dialog、editConfig 等）
4. 依赖分析（ClientLibs、Sling Models）
5. 静态资源

**原因**: 后面的步骤依赖前面的信息

### 2. 错误处理

**策略**:
- 使用 try-catch 捕获错误
- 记录错误但不中断整个流程
- 提供详细的错误信息

### 3. 性能优化

**策略**:
- 使用缓存避免重复请求
- 并行收集独立资源
- 增量收集已收集的组件

### 4. 数据存储

**策略**:
- 使用 JSON 格式存储
- 按组件组织目录结构
- 保存原始数据和解析后的数据

---

## 常见问题解决

### 问题 1: 无法找到组件

**原因**: 路径不正确或组件不存在

**解决方案**:
```javascript
async function findComponent(componentPath) {
    const possiblePaths = [
        componentPath,
        componentPath.replace('/apps/', ''),
        `/apps/${componentPath}`
    ];
    
    for (const path of possiblePaths) {
        try {
            const data = await aemClient.getJSON(`${path}.json`);
            return { path: path, data: data };
        } catch (error) {
            // 继续尝试
        }
    }
    
    throw new Error(`无法找到组件: ${componentPath}`);
}
```

### 问题 2: ClientLibs 找不到

**原因**: Categories 命名不一致或路径不同

**解决方案**:
```javascript
async function findClientLib(category) {
    // 尝试多种路径模式
    const patterns = [
        category.replace(/\./g, '/'),
        category.split('.').pop(),
        category.replace(/\./g, '-')
    ];
    
    for (const pattern of patterns) {
        const paths = [
            `/apps/myapp/clientlibs/${pattern}`,
            `/apps/myapp/clientlibs/components/${pattern}`,
            `/etc/clientlibs/${pattern}`
        ];
        
        for (const path of paths) {
            try {
                const data = await aemClient.getJSON(`${path}.json`);
                if (data['jcr:primaryType'] === 'cq:ClientLibraryFolder') {
                    return { path: path, data: data };
                }
            } catch (error) {
                // 继续尝试
            }
        }
    }
    
    return null;
}
```

### 问题 3: Java 文件找不到

**原因**: 包路径或项目结构不同

**解决方案**:
```javascript
async function findJavaFile(className) {
    const parts = className.split('.');
    const fileName = parts.pop();
    const packagePath = parts.join('/');
    
    // 尝试多种项目结构
    const projectStructures = [
        'core/src/main/java',
        'bundle/src/main/java',
        'src/main/java',
        'main/java'
    ];
    
    for (const structure of projectStructures) {
        const filePath = `/apps/myapp/${structure}/${packagePath}/${fileName}.java`;
        try {
            await aemClient.getText(filePath);
            return filePath;
        } catch (error) {
            // 继续尝试
        }
    }
    
    return null;
}
```

---

## 总结

### 核心要点

1. **多路径收集**: 尝试多个可能的路径
2. **缓存机制**: 避免重复收集
3. **并行处理**: 提高收集速度
4. **错误处理**: 优雅处理错误
5. **数据验证**: 确保数据完整性

### 实现建议

1. **先实现基础功能**: 先实现基本的收集功能
2. **逐步优化**: 根据使用情况逐步优化
3. **添加日志**: 记录收集过程便于调试
4. **测试验证**: 使用实际组件测试

### 下一步

- 使用 [资源收集器工具](../tools/component-resource-collector.js) 开始收集
- 根据项目需求定制收集逻辑
- 参考 [组件结构分析](./02-component-structure-analysis.md) 了解组件结构
- 参考 [收集策略详解](./03-collection-strategies.md) 了解收集策略

