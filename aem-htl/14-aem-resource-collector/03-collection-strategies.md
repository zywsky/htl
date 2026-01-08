# 收集策略详解

## 目录
1. [收集策略概述](#收集策略概述)
2. [广度优先策略](#广度优先策略)
3. [深度优先策略](#深度优先策略)
4. [按需收集策略](#按需收集策略)
5. [混合策略](#混合策略)
6. [策略选择指南](#策略选择指南)

---

## 收集策略概述

### 为什么需要不同的收集策略？

不同的场景需要不同的收集策略：

- **广度优先**: 快速了解组件结构，适合初步分析
- **深度优先**: 完整收集依赖链，适合迁移准备
- **按需收集**: 只收集需要的信息，适合特定场景
- **混合策略**: 结合多种策略，适合复杂场景

---

## 广度优先策略

### 特点

- ✅ 快速收集所有直接相关的资源
- ✅ 不深入依赖链
- ✅ 适合初步分析

### 实现

```javascript
async function breadthFirstCollect(componentPath) {
    const queue = [componentPath];
    const collected = new Set();
    const results = [];
    
    while (queue.length > 0) {
        const current = queue.shift();
        
        if (collected.has(current)) continue;
        collected.add(current);
        
        // 收集当前组件
        const componentData = await collectComponent(current);
        results.push(componentData);
        
        // 添加直接子组件到队列（不递归）
        for (const child of componentData.directChildren) {
            if (!collected.has(child.resourceType)) {
                queue.push(child.resourceType);
            }
        }
    }
    
    return results;
}
```

### 适用场景

- 快速了解组件结构
- 初步分析组件依赖
- 组件清单生成

---

## 深度优先策略

### 特点

- ✅ 完整收集依赖链
- ✅ 深入所有子组件
- ✅ 适合迁移准备

### 实现

```javascript
async function depthFirstCollect(componentPath, depth = 0, maxDepth = 5) {
    if (depth > maxDepth) {
        return null;
    }
    
    // 收集当前组件
    const componentData = await collectComponent(componentPath);
    
    // 递归收集子组件
    componentData.children = [];
    for (const child of componentData.childComponents) {
        const childData = await depthFirstCollect(
            child.resourceType,
            depth + 1,
            maxDepth
        );
        if (childData) {
            componentData.children.push(childData);
        }
    }
    
    return componentData;
}
```

### 适用场景

- 完整组件迁移
- 依赖关系分析
- 文档生成

---

## 按需收集策略

### 特点

- ✅ 只收集指定的信息类型
- ✅ 灵活配置
- ✅ 节省时间和资源

### 实现

```javascript
async function selectiveCollect(componentPath, options = {}) {
    const result = {
        component: null,
        files: {
            html: [],
            java: [],
            css: [],
            js: []
        },
        configurations: {},
        dependencies: {}
    };
    
    // 根据选项收集
    if (options.includeComponentInfo !== false) {
        result.component = await collectComponentInfo(componentPath);
    }
    
    if (options.includeFiles !== false) {
        result.files = await collectFiles(componentPath, options.fileTypes);
    }
    
    if (options.includeConfigs !== false) {
        result.configurations = await collectConfigurations(
            componentPath,
            options.configTypes
        );
    }
    
    if (options.includeDependencies !== false) {
        result.dependencies = await collectDependencies(
            componentPath,
            options.dependencyTypes
        );
    }
    
    return result;
}
```

### 使用示例

```javascript
// 只收集 HTL 模板和 CSS
const data = await selectiveCollect('/apps/myapp/components/hero', {
    includeComponentInfo: true,
    includeFiles: true,
    fileTypes: ['html', 'css'],
    includeConfigs: false,
    includeDependencies: false
});
```

### 适用场景

- 只关注特定类型的信息
- 快速原型开发
- 部分迁移

---

## 混合策略

### 特点

- ✅ 结合多种策略的优势
- ✅ 灵活适应不同场景
- ✅ 平衡速度和完整性

### 实现

```javascript
async function hybridCollect(componentPath, options = {}) {
    const strategy = options.strategy || 'breadth-first';
    const maxDepth = options.maxDepth || 3;
    
    // 第一阶段: 广度优先收集直接相关资源
    const directResources = await breadthFirstCollect(componentPath);
    
    // 第二阶段: 根据策略深入收集
    if (strategy === 'depth-first') {
        // 深度优先收集关键依赖
        for (const resource of directResources) {
            if (resource.isCritical) {
                resource.deepDependencies = await depthFirstCollect(
                    resource.path,
                    0,
                    maxDepth
                );
            }
        }
    } else if (strategy === 'selective') {
        // 按需收集特定依赖
        for (const resource of directResources) {
            if (options.shouldDeepCollect(resource)) {
                resource.deepDependencies = await selectiveCollect(
                    resource.path,
                    options.selectiveOptions
                );
            }
        }
    }
    
    return directResources;
}
```

### 适用场景

- 复杂组件迁移
- 平衡速度和完整性
- 灵活的场景需求

---

## 策略选择指南

### 根据项目规模选择

| 项目规模 | 推荐策略 | 原因 |
|---------|---------|------|
| 小型（< 10 组件） | 深度优先 | 组件少，可以完整收集 |
| 中型（10-50 组件） | 混合策略 | 平衡速度和完整性 |
| 大型（> 50 组件） | 广度优先 + 按需 | 快速收集，按需深入 |

### 根据迁移复杂度选择

| 复杂度 | 推荐策略 | 原因 |
|-------|---------|------|
| 简单组件 | 广度优先 | 依赖少，快速收集即可 |
| 中等复杂度 | 深度优先 | 需要完整依赖链 |
| 复杂组件 | 混合策略 | 需要灵活处理 |

### 根据时间限制选择

| 时间限制 | 推荐策略 | 原因 |
|---------|---------|------|
| 紧急 | 按需收集 | 只收集必要信息 |
| 正常 | 深度优先 | 完整收集 |
| 充足 | 混合策略 | 全面收集和分析 |

---

## 总结

### 核心要点

1. **广度优先**: 快速了解结构，适合初步分析
2. **深度优先**: 完整收集依赖，适合迁移准备
3. **按需收集**: 灵活配置，适合特定场景
4. **混合策略**: 结合优势，适合复杂场景

### 选择建议

1. **评估需求**: 先评估项目规模和复杂度
2. **选择策略**: 根据需求选择合适的策略
3. **灵活调整**: 根据实际情况调整策略
4. **持续优化**: 根据使用情况优化策略

