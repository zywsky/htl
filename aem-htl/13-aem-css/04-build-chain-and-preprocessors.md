# AEM CSS 构建链与预处理器集成

## 目录
1. [概述：为什么需要构建链](#概述为什么需要构建链)
2. [AEM 项目结构：ui.frontend 模块](#aem-项目结构uifrontend-模块)
3. [预处理器集成：Sass / Less / PostCSS](#预处理器集成sass--less--postcss)
4. [构建工具配置：Webpack / Vite](#构建工具配置webpack--vite)
5. [编译输出到 ClientLibs](#编译输出到-clientlibs)
6. [Categories 命名一致性保证](#categories-命名一致性保证)
7. [实际项目配置示例](#实际项目配置示例)
8. [最佳实践与常见问题](#最佳实践与常见问题)
9. [CI/CD 集成](#cicd-集成)
10. [调试与验证](#调试与验证)

---

## 概述：为什么需要构建链

### 现实情况

**问题**: 在实际项目中，开发者很少直接写纯 CSS 文件。

**原因**:
1. **预处理器优势**: Sass/SCSS、Less 提供变量、嵌套、混入、函数等高级特性
2. **模块化开发**: 需要将样式拆分成多个文件，按需导入
3. **自动优化**: 需要自动压缩、自动添加浏览器前缀、自动处理兼容性
4. **开发体验**: 需要热重载、Source Maps、错误提示等开发工具

### 传统方式 vs 构建链方式

#### 传统方式（不推荐）

```
直接编写 CSS 文件
  ↓
手动复制到 /apps/myapp/clientlibs/.../css/
  ↓
手动配置 .content.xml
  ↓
手动压缩和优化
```

**问题**:
- ❌ 无法使用预处理器特性
- ❌ 手动操作容易出错
- ❌ 难以维护大型项目
- ❌ 没有开发工具支持

#### 构建链方式（推荐）

```
编写 SCSS 文件（ui.frontend/src/main/webpack/...）
  ↓
Webpack/Vite 编译（自动处理依赖、压缩、优化）
  ↓
自动输出到 /apps/myapp/clientlibs/.../css/
  ↓
自动生成或更新 .content.xml
  ↓
自动处理 Source Maps、压缩、浏览器前缀
```

**优势**:
- ✅ 使用预处理器高级特性
- ✅ 自动化流程，减少错误
- ✅ 支持模块化开发
- ✅ 完整的开发工具链

---

## AEM 项目结构：ui.frontend 模块

### AEM Archetype 项目结构

AEM 项目通常使用 Maven Archetype 生成，标准结构如下：

```
myapp/
├── ui.apps/                    # 组件模板、配置（部署到 /apps/myapp/）
│   ├── src/
│   │   └── main/
│   │       └── content/
│   │           └── jcr_root/
│   │               └── apps/
│   │                   └── myapp/
│   │                       ├── components/
│   │                       └── clientlibs/    # 编译后的 CSS/JS 输出目录
│   └── pom.xml
│
├── ui.frontend/                # 前端源代码和构建配置（重点）
│   ├── src/
│   │   └── main/
│   │       └── webpack/
│   │           ├── components/                # 组件 SCSS 文件
│   │           │   ├── hero/
│   │           │   │   └── hero.scss
│   │           │   └── card/
│   │           │       └── card.scss
│   │           ├── base/                     # 基础样式
│   │           │   ├── _variables.scss
│   │           │   ├── _mixins.scss
│   │           │   └── base.scss
│   │           └── theme/                    # 主题样式
│   │               └── theme.scss
│   ├── webpack.config.js                      # Webpack 配置
│   ├── package.json                           # NPM 依赖
│   └── pom.xml                                # Maven 集成
│
├── core/                       # Java 代码（Sling Models）
├── it.tests/                   # 集成测试
└── pom.xml                     # 父 POM
```

### ui.frontend 模块的作用

**ui.frontend 模块**是前端开发的"工作区"：

1. **源代码目录**: 存放 SCSS、TypeScript/JavaScript 源代码
2. **构建配置**: Webpack/Vite 配置文件
3. **依赖管理**: package.json 管理 NPM 依赖
4. **编译输出**: 编译后的文件输出到 `ui.apps` 模块的 `clientlibs` 目录

### 工作流程

```
开发者编写 SCSS
  ↓
ui.frontend/src/main/webpack/components/hero/hero.scss
  ↓
Webpack 编译（npm run build 或 mvn clean install）
  ↓
输出到 ui.apps/src/main/content/jcr_root/apps/myapp/clientlibs/components/hero/css/hero.css
  ↓
Maven 打包
  ↓
部署到 AEM 实例
  ↓
AEM 通过 ClientLibs 机制加载
```

---

## 预处理器集成：Sass / Less / PostCSS

### Sass / SCSS 集成

#### 为什么选择 Sass/SCSS？

**Sass（Syntactically Awesome Style Sheets）**是最流行的 CSS 预处理器：

1. **变量**: 统一管理颜色、字体、间距等
2. **嵌套**: 更直观的层级结构
3. **混入（Mixins）**: 复用样式代码块
4. **函数**: 计算、转换、处理数据
5. **导入**: 模块化组织代码
6. **继承**: 样式继承和扩展

#### SCSS 文件结构示例

**基础变量文件** (`ui.frontend/src/main/webpack/base/_variables.scss`):

```scss
// 颜色变量
$color-primary: #007bff;
$color-secondary: #6c757d;
$color-success: #28a745;
$color-danger: #dc3545;

// 字体变量
$font-family-base: 'Helvetica Neue', Arial, sans-serif;
$font-size-base: 16px;
$font-weight-normal: 400;
$font-weight-bold: 700;

// 间距变量
$spacing-xs: 4px;
$spacing-sm: 8px;
$spacing-md: 16px;
$spacing-lg: 24px;
$spacing-xl: 32px;

// 断点变量
$breakpoint-sm: 576px;
$breakpoint-md: 768px;
$breakpoint-lg: 992px;
$breakpoint-xl: 1200px;
```

**混入文件** (`ui.frontend/src/main/webpack/base/_mixins.scss`):

```scss
// 响应式断点混入
@mixin respond-to($breakpoint) {
  @if $breakpoint == 'sm' {
    @media (min-width: $breakpoint-sm) {
      @content;
    }
  } @else if $breakpoint == 'md' {
    @media (min-width: $breakpoint-md) {
      @content;
    }
  } @else if $breakpoint == 'lg' {
    @media (min-width: $breakpoint-lg) {
      @content;
    }
  } @else if $breakpoint == 'xl' {
    @media (min-width: $breakpoint-xl) {
      @content;
    }
  }
}

// Flexbox 居中混入
@mixin flex-center {
  display: flex;
  justify-content: center;
  align-items: center;
}

// 清除浮动混入
@mixin clearfix {
  &::after {
    content: '';
    display: table;
    clear: both;
  }
}
```

**组件 SCSS 文件** (`ui.frontend/src/main/webpack/components/hero/hero.scss`):

```scss
// 导入基础样式
@import '../../base/variables';
@import '../../base/mixins';

// Hero 组件样式
.hero {
  position: relative;
  padding: $spacing-xl 0;
  background-color: $color-primary;
  color: white;
  
  &__title {
    font-size: 2.5rem;
    font-weight: $font-weight-bold;
    margin-bottom: $spacing-md;
    
    @include respond-to('md') {
      font-size: 3.5rem;
    }
  }
  
  &__subtitle {
    font-size: 1.25rem;
    margin-bottom: $spacing-lg;
    opacity: 0.9;
  }
  
  &__button {
    display: inline-block;
    padding: $spacing-sm $spacing-lg;
    background-color: white;
    color: $color-primary;
    text-decoration: none;
    border-radius: 4px;
    transition: transform 0.2s;
    
    &:hover {
      transform: translateY(-2px);
    }
  }
  
  // 响应式调整
  @include respond-to('lg') {
    padding: $spacing-xl * 2 0;
  }
}
```

### Less 集成

#### Less 配置

Less 是另一个流行的 CSS 预处理器，语法类似但有一些差异：

**package.json 依赖**:

```json
{
  "devDependencies": {
    "less": "^4.1.3",
    "less-loader": "^11.1.3"
  }
}
```

**Webpack 配置**:

```javascript
module.exports = {
  module: {
    rules: [
      {
        test: /\.less$/,
        use: [
          'style-loader',
          'css-loader',
          'less-loader'
        ]
      }
    ]
  }
};
```

**Less 文件示例** (`ui.frontend/src/main/webpack/components/card/card.less`):

```less
@color-primary: #007bff;
@spacing-md: 16px;

.card {
  padding: @spacing-md;
  border: 1px solid #ddd;
  border-radius: 4px;
  
  &__header {
    font-weight: bold;
    margin-bottom: @spacing-md;
  }
  
  &__body {
    color: #666;
  }
}
```

### PostCSS 集成

#### PostCSS 的作用

**PostCSS** 是一个用 JavaScript 转换 CSS 的工具，通常与 Sass/Less 一起使用：

1. **自动添加浏览器前缀**: 使用 Autoprefixer
2. **CSS 压缩**: 使用 cssnano
3. **未来 CSS 特性**: 使用 postcss-preset-env
4. **CSS 模块化**: 使用 postcss-modules

#### PostCSS 配置

**postcss.config.js**:

```javascript
module.exports = {
  plugins: [
    require('autoprefixer')({
      overrideBrowserslist: [
        'last 2 versions',
        '> 1%',
        'not dead'
      ]
    }),
    require('cssnano')({
      preset: 'default'
    }),
    require('postcss-preset-env')({
      stage: 2
    })
  ]
};
```

**package.json 依赖**:

```json
{
  "devDependencies": {
    "postcss": "^8.4.21",
    "postcss-loader": "^7.0.2",
    "autoprefixer": "^10.4.14",
    "cssnano": "^5.1.15",
    "postcss-preset-env": "^8.0.1"
  }
}
```

---

## 构建工具配置：Webpack / Vite

### Webpack 配置

#### 完整 Webpack 配置示例

**webpack.config.js** (`ui.frontend/webpack.config.js`):

```javascript
const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');

// 定义输出目录（对应 AEM clientlibs 目录结构）
const CLIENTLIB_ROOT = path.resolve(__dirname, '../ui.apps/src/main/content/jcr_root/apps/myapp/clientlibs');

module.exports = (env, argv) => {
  const isProduction = argv.mode === 'production';

  return {
    entry: {
      // 基础样式
      'base/base': './src/main/webpack/base/base.scss',
      
      // 组件样式
      'components/hero/hero': './src/main/webpack/components/hero/hero.scss',
      'components/card/card': './src/main/webpack/components/card/card.scss',
      
      // 主题样式
      'theme/theme': './src/main/webpack/theme/theme.scss',
    },
    
    output: {
      path: CLIENTLIB_ROOT,
      filename: '[name].js', // JavaScript 输出（如果有）
    },
    
    module: {
      rules: [
        // SCSS 文件处理
        {
          test: /\.scss$/,
          use: [
            // 提取 CSS 到独立文件
            {
              loader: MiniCssExtractPlugin.loader,
            },
            // 处理 CSS
            {
              loader: 'css-loader',
              options: {
                sourceMap: !isProduction,
                importLoaders: 2, // 在 css-loader 之前运行 2 个 loader（postcss-loader, sass-loader）
              },
            },
            // PostCSS 处理（自动添加前缀、压缩等）
            {
              loader: 'postcss-loader',
              options: {
                sourceMap: !isProduction,
              },
            },
            // Sass 编译
            {
              loader: 'sass-loader',
              options: {
                sourceMap: !isProduction,
                sassOptions: {
                  outputStyle: isProduction ? 'compressed' : 'expanded',
                },
              },
            },
          ],
        },
        // 图片资源处理
        {
          test: /\.(png|jpg|jpeg|gif|svg)$/,
          type: 'asset/resource',
          generator: {
            filename: '[path][name][ext]',
          },
        },
        // 字体文件处理
        {
          test: /\.(woff|woff2|eot|ttf|otf)$/,
          type: 'asset/resource',
          generator: {
            filename: '[path][name][ext]',
          },
        },
      ],
    },
    
    plugins: [
      // 清理输出目录
      new CleanWebpackPlugin({
        cleanOnceBeforeBuildPatterns: [
          path.join(CLIENTLIB_ROOT, '**/*'),
          `!${path.join(CLIENTLIB_ROOT, '.content.xml')}`, // 保留配置文件
        ],
      }),
      
      // 提取 CSS 到独立文件
      new MiniCssExtractPlugin({
        filename: '[name]/css/[name].css',
        chunkFilename: '[id].css',
      }),
      
      // 复制 .content.xml 配置文件
      new CopyWebpackPlugin({
        patterns: [
          {
            from: path.resolve(__dirname, 'clientlibs-config'),
            to: CLIENTLIB_ROOT,
            globOptions: {
              ignore: ['**/.DS_Store'],
            },
          },
        ],
      }),
    ],
    
    // Source Maps（开发环境）
    devtool: isProduction ? false : 'source-map',
    
    // 优化配置
    optimization: {
      minimize: isProduction,
    },
    
    // 性能提示
    performance: {
      hints: isProduction ? 'warning' : false,
    },
  };
};
```

#### package.json 脚本

**package.json** (`ui.frontend/package.json`):

```json
{
  "name": "myapp-ui.frontend",
  "version": "1.0.0",
  "scripts": {
    "build": "webpack --mode production",
    "build:dev": "webpack --mode development",
    "watch": "webpack --mode development --watch",
    "clean": "rimraf ../ui.apps/src/main/content/jcr_root/apps/myapp/clientlibs/**/*.{css,js,map}"
  },
  "devDependencies": {
    "webpack": "^5.88.0",
    "webpack-cli": "^5.1.4",
    "sass": "^1.64.1",
    "sass-loader": "^13.3.2",
    "css-loader": "^6.8.1",
    "postcss": "^8.4.21",
    "postcss-loader": "^7.3.3",
    "autoprefixer": "^10.4.14",
    "cssnano": "^5.1.15",
    "mini-css-extract-plugin": "^2.7.6",
    "clean-webpack-plugin": "^4.0.0",
    "copy-webpack-plugin": "^11.0.0"
  }
}
```

### Vite 配置（现代替代方案）

#### Vite 的优势

**Vite** 是新一代前端构建工具，相比 Webpack 有以下优势：

1. **更快的开发服务器**: 基于 ES modules，无需打包
2. **更快的构建**: 使用 Rollup，构建速度更快
3. **更好的开发体验**: 热更新更快，错误提示更清晰
4. **更简单的配置**: 配置更简洁，开箱即用

#### Vite 配置示例

**vite.config.js** (`ui.frontend/vite.config.js`):

```javascript
import { defineConfig } from 'vite';
import { resolve } from 'path';
import { copyFileSync, mkdirSync, existsSync } from 'fs';

const CLIENTLIB_ROOT = resolve(__dirname, '../ui.apps/src/main/content/jcr_root/apps/myapp/clientlibs');

// 自定义插件：复制 .content.xml 文件
function copyContentXml() {
  return {
    name: 'copy-content-xml',
    buildStart() {
      // 确保目录存在
      if (!existsSync(CLIENTLIB_ROOT)) {
        mkdirSync(CLIENTLIB_ROOT, { recursive: true });
      }
      
      // 复制配置文件
      const configSource = resolve(__dirname, 'clientlibs-config');
      // 这里可以添加复制逻辑
    },
  };
}

export default defineConfig({
  root: 'src/main/webpack',
  
  build: {
    outDir: CLIENTLIB_ROOT,
    emptyOutDir: false,
    rollupOptions: {
      input: {
        'base/base': resolve(__dirname, 'src/main/webpack/base/base.scss'),
        'components/hero/hero': resolve(__dirname, 'src/main/webpack/components/hero/hero.scss'),
        'components/card/card': resolve(__dirname, 'src/main/webpack/components/card/card.scss'),
      },
      output: {
        assetFileNames: (assetInfo) => {
          // CSS 文件输出到对应目录的 css 子目录
          if (assetInfo.name.endsWith('.css')) {
            const name = assetInfo.name.replace('.css', '');
            return `${name}/css/${name}.css`;
          }
          return '[name][extname]';
        },
      },
    },
    cssCodeSplit: false, // 不拆分 CSS（每个入口一个文件）
    cssMinify: true,
    sourcemap: true,
  },
  
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@import "./base/_variables.scss";`, // 全局导入变量
      },
    },
    postcss: {
      plugins: [
        require('autoprefixer'),
        require('cssnano')({
          preset: 'default',
        }),
      ],
    },
  },
  
  plugins: [
    copyContentXml(),
  ],
});
```

**package.json** (使用 Vite):

```json
{
  "name": "myapp-ui.frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "build": "vite build",
    "dev": "vite",
    "preview": "vite preview"
  },
  "devDependencies": {
    "vite": "^4.4.5",
    "sass": "^1.64.1",
    "autoprefixer": "^10.4.14",
    "cssnano": "^5.1.15",
    "postcss": "^8.4.21"
  }
}
```

---

## 编译输出到 ClientLibs

### 输出目录结构

编译后的文件必须输出到正确的 AEM ClientLibs 目录结构：

```
ui.apps/src/main/content/jcr_root/apps/myapp/clientlibs/
├── base/
│   ├── .content.xml                    # ClientLib 配置（手动创建或自动生成）
│   └── css/
│       └── base.css                    # 编译后的 CSS（Webpack/Vite 输出）
│
├── components/
│   ├── hero/
│   │   ├── .content.xml
│   │   └── css/
│   │       └── hero.css
│   └── card/
│       ├── .content.xml
│       └── css/
│           └── card.css
│
└── theme/
    ├── .content.xml
    └── css/
        └── theme.css
```

### 输出路径映射规则

**关键规则**: 源代码路径 → 输出路径的映射必须与 AEM ClientLibs 的 categories 命名保持一致。

#### 映射示例

| 源代码路径 | 输出路径 | Categories |
|-----------|---------|------------|
| `ui.frontend/src/main/webpack/base/base.scss` | `ui.apps/.../clientlibs/base/css/base.css` | `myapp.base` |
| `ui.frontend/src/main/webpack/components/hero/hero.scss` | `ui.apps/.../clientlibs/components/hero/css/hero.css` | `myapp.components.hero` |
| `ui.frontend/src/main/webpack/components/card/card.scss` | `ui.apps/.../clientlibs/components/card/css/card.css` | `myapp.components.card` |

### Webpack 输出配置详解

#### 1. Entry 配置（定义入口）

```javascript
entry: {
  // 格式: '输出路径（相对于 CLIENTLIB_ROOT）': '源代码路径'
  'base/base': './src/main/webpack/base/base.scss',
  'components/hero/hero': './src/main/webpack/components/hero/hero.scss',
}
```

**说明**:
- Key (`'base/base'`) 决定了输出目录结构
- Value (`'./src/main/webpack/base/base.scss'`) 是源代码文件路径

#### 2. MiniCssExtractPlugin 配置（CSS 输出）

```javascript
new MiniCssExtractPlugin({
  filename: '[name]/css/[name].css',
  // [name] 会被替换为 entry 的 key
  // 例如: 'base/base' → 'base/base/css/base/base.css'
  // 但通常我们希望: 'base/base' → 'base/css/base.css'
})
```

**优化配置**:

```javascript
new MiniCssExtractPlugin({
  filename: (pathData) => {
    // pathData.chunk.name = 'base/base'
    const parts = pathData.chunk.name.split('/');
    const dir = parts[0]; // 'base'
    const file = parts[parts.length - 1]; // 'base'
    return `${dir}/css/${file}.css`;
  },
})
```

#### 3. 完整输出配置示例

```javascript
const path = require('path');

// 辅助函数：从 entry name 提取目录和文件名
function getOutputPath(entryName) {
  const parts = entryName.split('/');
  const dir = parts[0];
  const file = parts[parts.length - 1];
  return {
    dir,
    file,
    cssPath: `${dir}/css/${file}.css`,
  };
}

module.exports = {
  entry: {
    'base/base': './src/main/webpack/base/base.scss',
    'components/hero/hero': './src/main/webpack/components/hero/hero.scss',
  },
  
  output: {
    path: CLIENTLIB_ROOT,
    // JavaScript 输出（如果有 JS 入口）
    filename: (pathData) => {
      const { dir, file } = getOutputPath(pathData.chunk.name);
      return `${dir}/js/${file}.js`;
    },
  },
  
  plugins: [
    new MiniCssExtractPlugin({
      filename: (pathData) => {
        const { cssPath } = getOutputPath(pathData.chunk.name);
        return cssPath;
      },
    }),
  ],
};
```

---

## Categories 命名一致性保证

### 问题：为什么需要一致性？

**核心问题**: 如何确保编译输出的目录结构与 AEM ClientLibs 的 categories 命名保持一致？

**不一致的后果**:
- ❌ 组件无法找到对应的 CSS
- ❌ 依赖关系混乱
- ❌ 调试困难
- ❌ 维护成本高

### 一致性规则

#### 规则 1: 目录结构 = Categories 层次

```
源代码目录结构:
ui.frontend/src/main/webpack/components/hero/hero.scss
  ↓
输出目录结构:
ui.apps/.../clientlibs/components/hero/css/hero.css
  ↓
Categories 命名:
myapp.components.hero
```

**映射关系**:
- `components/hero` → `myapp.components.hero`
- `base` → `myapp.base`
- `theme` → `myapp.theme`

#### 规则 2: 命名约定

**约定**:
1. **小写字母**: 目录和文件名使用小写
2. **连字符分隔**: 多个单词使用连字符（`-`），不用下划线
3. **点分隔 categories**: Categories 使用点（`.`）分隔层次
4. **项目前缀**: Categories 必须以项目名开头（`myapp.`）

**示例**:

| 目录结构 | Categories | 说明 |
|---------|-----------|------|
| `components/hero-banner/hero-banner.scss` | `myapp.components.hero-banner` | 组件名使用连字符 |
| `components/product-list/product-list.scss` | `myapp.components.product-list` | 多个单词用连字符 |
| `base/base.scss` | `myapp.base` | 基础库 |
| `theme/brand-a/theme.scss` | `myapp.theme.brand-a` | 主题子分类 |

### 自动化一致性保证方案

#### 方案 1: 配置文件驱动（推荐）

**创建配置文件** (`ui.frontend/clientlibs.config.js`):

```javascript
/**
 * ClientLibs 配置映射表
 * 确保源代码路径、输出路径、categories 命名的一致性
 */
module.exports = {
  // 基础样式库
  base: {
    source: 'src/main/webpack/base/base.scss',
    output: 'base/css/base.css',
    category: 'myapp.base',
    dependencies: [],
  },
  
  // Hero 组件
  'components.hero': {
    source: 'src/main/webpack/components/hero/hero.scss',
    output: 'components/hero/css/hero.css',
    category: 'myapp.components.hero',
    dependencies: ['myapp.base'],
  },
  
  // Card 组件
  'components.card': {
    source: 'src/main/webpack/components/card/card.scss',
    output: 'components/card/css/card.css',
    category: 'myapp.components.card',
    dependencies: ['myapp.base'],
  },
  
  // 主题样式
  theme: {
    source: 'src/main/webpack/theme/theme.scss',
    output: 'theme/css/theme.css',
    category: 'myapp.theme',
    dependencies: ['myapp.base'],
  },
};
```

**Webpack 配置使用配置文件**:

```javascript
const clientlibsConfig = require('./clientlibs.config.js');

// 从配置生成 entry
const entry = {};
Object.keys(clientlibsConfig).forEach((key) => {
  const config = clientlibsConfig[key];
  entry[key] = `./${config.source}`;
});

module.exports = {
  entry,
  
  plugins: [
    new MiniCssExtractPlugin({
      filename: (pathData) => {
        const config = clientlibsConfig[pathData.chunk.name];
        return config.output;
      },
    }),
    
    // 自动生成 .content.xml 文件
    new GenerateContentXmlPlugin({
      config: clientlibsConfig,
      outputPath: CLIENTLIB_ROOT,
    }),
  ],
};
```

#### 方案 2: 自动生成 .content.xml

**自定义 Webpack 插件** (`ui.frontend/plugins/generate-content-xml-plugin.js`):

```javascript
const fs = require('fs');
const path = require('path');

class GenerateContentXmlPlugin {
  constructor(options) {
    this.config = options.config;
    this.outputPath = options.outputPath;
  }
  
  apply(compiler) {
    compiler.hooks.emit.tapAsync('GenerateContentXmlPlugin', (compilation, callback) => {
      // 为每个 ClientLib 生成 .content.xml
      Object.keys(this.config).forEach((key) => {
        const config = this.config[key];
        const dir = path.join(this.outputPath, config.output.split('/css/')[0]);
        
        // 确保目录存在
        if (!fs.existsSync(dir)) {
          fs.mkdirSync(dir, { recursive: true });
        }
        
        // 生成 .content.xml 内容
        const xmlContent = this.generateContentXml(config);
        const xmlPath = path.join(dir, '.content.xml');
        
        fs.writeFileSync(xmlPath, xmlContent, 'utf8');
      });
      
      callback();
    });
  }
  
  generateContentXml(config) {
    const dependencies = config.dependencies || [];
    const dependenciesAttr = dependencies.length > 0
      ? `dependencies="[${dependencies.map(d => `"${d}"`).join(',')}]"`
      : '';
    
    return `<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" 
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          categories="[${config.category}]"
          ${dependenciesAttr}
          allowProxy="{Boolean}true"/>`;
  }
}

module.exports = GenerateContentXmlPlugin;
```

#### 方案 3: 验证脚本

**创建验证脚本** (`ui.frontend/scripts/validate-clientlibs.js`):

```javascript
const fs = require('fs');
const path = require('path');
const clientlibsConfig = require('../clientlibs.config.js');

const CLIENTLIB_ROOT = path.resolve(__dirname, '../../ui.apps/src/main/content/jcr_root/apps/myapp/clientlibs');

function validateClientlibs() {
  console.log('🔍 验证 ClientLibs 一致性...\n');
  
  let hasError = false;
  
  Object.keys(clientlibsConfig).forEach((key) => {
    const config = clientlibsConfig[key];
    const outputDir = path.join(CLIENTLIB_ROOT, config.output.split('/css/')[0]);
    const cssFile = path.join(CLIENTLIB_ROOT, config.output);
    const xmlFile = path.join(outputDir, '.content.xml');
    
    // 检查 CSS 文件是否存在
    if (!fs.existsSync(cssFile)) {
      console.error(`❌ CSS 文件不存在: ${cssFile}`);
      hasError = true;
    }
    
    // 检查 .content.xml 是否存在
    if (!fs.existsSync(xmlFile)) {
      console.error(`❌ 配置文件不存在: ${xmlFile}`);
      hasError = true;
    } else {
      // 验证 .content.xml 中的 categories
      const xmlContent = fs.readFileSync(xmlFile, 'utf8');
      if (!xmlContent.includes(`categories="[${config.category}]"`)) {
        console.error(`❌ Categories 不匹配: ${config.category} (期望) vs ${xmlFile} (实际)`);
        hasError = true;
      }
    }
    
    // 验证依赖关系
    if (config.dependencies && config.dependencies.length > 0) {
      config.dependencies.forEach((dep) => {
        const depConfig = Object.values(clientlibsConfig).find(c => c.category === dep);
        if (!depConfig) {
          console.warn(`⚠️  依赖的 ClientLib 未定义: ${dep}`);
        }
      });
    }
  });
  
  if (hasError) {
    console.error('\n❌ 验证失败！请检查上述错误。');
    process.exit(1);
  } else {
    console.log('✅ 所有 ClientLibs 验证通过！');
  }
}

validateClientlibs();
```

**package.json 添加验证脚本**:

```json
{
  "scripts": {
    "build": "webpack --mode production",
    "validate": "node scripts/validate-clientlibs.js",
    "build:validate": "npm run build && npm run validate"
  }
}
```

---

## 实际项目配置示例

### 完整项目结构示例

```
myapp/
├── ui.frontend/
│   ├── src/main/webpack/
│   │   ├── base/
│   │   │   ├── _variables.scss
│   │   │   ├── _mixins.scss
│   │   │   └── base.scss
│   │   ├── components/
│   │   │   ├── hero/
│   │   │   │   └── hero.scss
│   │   │   └── card/
│   │   │       └── card.scss
│   │   └── theme/
│   │       └── theme.scss
│   ├── clientlibs.config.js          # 配置映射表
│   ├── webpack.config.js             # Webpack 配置
│   ├── package.json
│   └── plugins/
│       └── generate-content-xml-plugin.js
│
└── ui.apps/
    └── src/main/content/jcr_root/apps/myapp/
        └── clientlibs/                # 编译输出目录
            ├── base/
            │   ├── .content.xml       # 自动生成
            │   └── css/
            │       └── base.css       # Webpack 输出
            ├── components/
            │   ├── hero/
            │   │   ├── .content.xml
            │   │   └── css/
            │   │       └── hero.css
            │   └── card/
            │       ├── .content.xml
            │       └── css/
            │           └── card.css
            └── theme/
                ├── .content.xml
                └── css/
                    └── theme.css
```

### 完整 Webpack 配置示例

**webpack.config.js**:

```javascript
const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const GenerateContentXmlPlugin = require('./plugins/generate-content-xml-plugin');
const clientlibsConfig = require('./clientlibs.config.js');

const CLIENTLIB_ROOT = path.resolve(__dirname, '../ui.apps/src/main/content/jcr_root/apps/myapp/clientlibs');

// 从配置生成 entry
const entry = {};
Object.keys(clientlibsConfig).forEach((key) => {
  entry[key] = `./${clientlibsConfig[key].source}`;
});

module.exports = (env, argv) => {
  const isProduction = argv.mode === 'production';

  return {
    entry,
    
    output: {
      path: CLIENTLIB_ROOT,
    },
    
    module: {
      rules: [
        {
          test: /\.scss$/,
          use: [
            {
              loader: MiniCssExtractPlugin.loader,
            },
            {
              loader: 'css-loader',
              options: {
                sourceMap: !isProduction,
                importLoaders: 2,
              },
            },
            {
              loader: 'postcss-loader',
              options: {
                sourceMap: !isProduction,
              },
            },
            {
              loader: 'sass-loader',
              options: {
                sourceMap: !isProduction,
                sassOptions: {
                  outputStyle: isProduction ? 'compressed' : 'expanded',
                },
              },
            },
          ],
        },
        {
          test: /\.(png|jpg|jpeg|gif|svg)$/,
          type: 'asset/resource',
          generator: {
            filename: '[path][name][ext]',
          },
        },
      ],
    },
    
    plugins: [
      new CleanWebpackPlugin({
        cleanOnceBeforeBuildPatterns: [
          path.join(CLIENTLIB_ROOT, '**/*'),
          `!${path.join(CLIENTLIB_ROOT, '.content.xml')}`,
        ],
      }),
      
      new MiniCssExtractPlugin({
        filename: (pathData) => {
          const config = clientlibsConfig[pathData.chunk.name];
          return config.output;
        },
      }),
      
      new GenerateContentXmlPlugin({
        config: clientlibsConfig,
        outputPath: CLIENTLIB_ROOT,
      }),
    ],
    
    devtool: isProduction ? false : 'source-map',
  };
};
```

### Maven 集成

**ui.frontend/pom.xml**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  
  <parent>
    <groupId>com.myapp</groupId>
    <artifactId>myapp</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>
  
  <artifactId>myapp.ui.frontend</artifactId>
  <packaging>pom</packaging>
  
  <build>
    <plugins>
      <!-- Frontend Maven Plugin: 运行 NPM 脚本 -->
      <plugin>
        <groupId>com.github.eirslett</groupId>
        <artifactId>frontend-maven-plugin</artifactId>
        <version>1.15.0</version>
        <configuration>
          <workingDirectory>${project.basedir}</workingDirectory>
        </configuration>
        <executions>
          <!-- 安装 Node.js 和 NPM -->
          <execution>
            <id>install node and npm</id>
            <goals>
              <goal>install-node-and-npm</goal>
            </goals>
            <configuration>
              <nodeVersion>v18.17.0</nodeVersion>
              <npmVersion>9.8.1</npmVersion>
            </configuration>
          </execution>
          
          <!-- 安装 NPM 依赖 -->
          <execution>
            <id>npm install</id>
            <goals>
              <goal>npm</goal>
            </goals>
            <configuration>
              <arguments>install</arguments>
            </configuration>
          </execution>
          
          <!-- 运行 Webpack 构建 -->
          <execution>
            <id>npm run build</id>
            <goals>
              <goal>npm</goal>
            </goals>
            <configuration>
              <arguments>run build</arguments>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

**父 POM 配置** (`pom.xml`):

```xml
<modules>
  <module>ui.frontend</module>
  <module>ui.apps</module>
  <module>core</module>
</modules>

<build>
  <plugins>
    <!-- 确保 ui.frontend 在 ui.apps 之前构建 -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-dependency-plugin</artifactId>
      <executions>
        <execution>
          <id>unpack frontend build</id>
          <phase>generate-resources</phase>
          <goals>
            <goal>unpack-dependencies</goal>
          </goals>
          <configuration>
            <includeGroupIds>com.myapp</includeGroupIds>
            <includeArtifactIds>myapp.ui.frontend</includeArtifactIds>
            <outputDirectory>${project.build.directory}/classes</outputDirectory>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

---

## 最佳实践与常见问题

### 最佳实践

#### 1. 目录结构规范

**✅ 推荐结构**:

```
ui.frontend/src/main/webpack/
├── base/                    # 基础样式（变量、混入、重置样式）
│   ├── _variables.scss
│   ├── _mixins.scss
│   └── base.scss
├── components/              # 组件样式（按组件组织）
│   ├── hero/
│   │   └── hero.scss
│   └── card/
│       └── card.scss
├── layout/                  # 布局样式（页面布局、网格系统）
│   └── grid.scss
├── theme/                   # 主题样式（品牌色、主题变量）
│   └── theme.scss
└── vendor/                  # 第三方库样式
    └── bootstrap.scss
```

#### 2. 命名规范

**✅ Categories 命名**:
- 使用点分隔的层次结构: `myapp.components.hero`
- 使用小写字母和连字符: `myapp.components.hero-banner`
- 保持与目录结构一致: `components/hero` → `myapp.components.hero`

**✅ 文件命名**:
- 组件文件: `hero.scss`（与组件名一致）
- 部分文件: `_variables.scss`（以下划线开头）
- 输出文件: `hero.css`（与源文件同名）

#### 3. 依赖管理

**✅ 合理声明依赖**:

```javascript
// clientlibs.config.js
{
  'components.hero': {
    dependencies: ['myapp.base'], // 只声明直接依赖
  },
  'components.card': {
    dependencies: ['myapp.base'], // 不依赖 hero，即使它们都依赖 base
  },
}
```

**❌ 避免循环依赖**:

```javascript
// ❌ 错误示例
{
  'components.hero': {
    dependencies: ['myapp.components.card'],
  },
  'components.card': {
    dependencies: ['myapp.components.hero'], // 循环依赖！
  },
}
```

#### 4. 性能优化

**✅ 按需加载**:
- 每个组件独立的 ClientLib
- 页面只加载需要的 CSS

**✅ 压缩和优化**:
- 生产环境启用 CSS 压缩
- 使用 PostCSS 自动添加浏览器前缀
- 移除未使用的 CSS（使用 PurgeCSS）

**✅ 缓存策略**:
- 使用版本号或 hash 文件名
- 长期缓存基础库，短期缓存组件样式

### 常见问题

#### 问题 1: CSS 文件找不到

**症状**: 组件样式不生效，浏览器控制台显示 404 错误。

**原因**:
1. 输出路径配置错误
2. .content.xml 中的 categories 不匹配
3. 编译未成功执行

**解决方案**:
1. 检查 Webpack 输出路径配置
2. 验证 .content.xml 中的 categories
3. 运行验证脚本: `npm run validate`

#### 问题 2: 样式冲突

**症状**: 不同组件的样式互相影响。

**原因**:
1. CSS 选择器过于宽泛
2. 没有使用 BEM 命名规范
3. 全局样式污染

**解决方案**:
1. 使用 BEM 命名: `.hero__title`, `.hero__button`
2. 使用 CSS 模块化（PostCSS Modules）
3. 限制全局样式的范围

#### 问题 3: 构建速度慢

**症状**: Webpack 构建时间过长。

**原因**:
1. 文件过多
2. 未启用缓存
3. Source Maps 未优化

**解决方案**:
1. 使用 Webpack 缓存: `cache: { type: 'filesystem' }`
2. 生产环境禁用 Source Maps
3. 使用 Vite 替代 Webpack（更快的构建速度）

#### 问题 4: 热更新不工作

**症状**: 修改 SCSS 文件后，页面不自动刷新。

**原因**:
1. Webpack watch 模式未启用
2. AEM Dispatcher 缓存
3. 浏览器缓存

**解决方案**:
1. 使用 `npm run watch` 启动 watch 模式
2. 清除 Dispatcher 缓存
3. 使用浏览器硬刷新（Cmd+Shift+R）

---

## CI/CD 集成

### 构建流程

**CI/CD 流程**:

```
1. Git Push
   ↓
2. CI 服务器触发构建
   ↓
3. 安装 NPM 依赖 (npm install)
   ↓
4. 运行 Webpack 构建 (npm run build)
   ↓
5. 运行验证脚本 (npm run validate)
   ↓
6. 运行测试 (npm test)
   ↓
7. Maven 打包 (mvn clean install)
   ↓
8. 部署到 AEM 实例
```

### GitHub Actions 示例

**.github/workflows/build.yml**:

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: ui.frontend/package-lock.json
    
    - name: Install dependencies
      working-directory: ui.frontend
      run: npm ci
    
    - name: Build frontend
      working-directory: ui.frontend
      run: npm run build
    
    - name: Validate ClientLibs
      working-directory: ui.frontend
      run: npm run validate
    
    - name: Setup Java
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    
    - name: Build with Maven
      run: mvn clean install -DskipTests
    
    - name: Deploy to AEM
      if: github.ref == 'refs/heads/main'
      run: |
        # 部署逻辑
        echo "Deploying to AEM..."
```

### Jenkins Pipeline 示例

**Jenkinsfile**:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Frontend') {
            steps {
                dir('ui.frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                    sh 'npm run validate'
                }
            }
        }
        
        stage('Build Maven') {
            steps {
                sh 'mvn clean install -DskipTests'
            }
        }
        
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                echo 'Deploying to AEM...'
                // 部署逻辑
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}
```

---

## 调试与验证

### 开发环境调试

#### 1. Source Maps

**启用 Source Maps**:

```javascript
// webpack.config.js
module.exports = {
  devtool: 'source-map', // 开发环境
  // 或
  devtool: 'eval-source-map', // 更快的开发体验
};
```

**浏览器中使用**:
1. 打开浏览器开发者工具
2. 在 Sources 标签中找到原始 SCSS 文件
3. 设置断点，调试样式

#### 2. Webpack Bundle Analyzer

**安装**:

```bash
npm install --save-dev webpack-bundle-analyzer
```

**使用**:

```javascript
// webpack.config.js
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

module.exports = {
  plugins: [
    new BundleAnalyzerPlugin({
      analyzerMode: 'static',
      openAnalyzer: false,
    }),
  ],
};
```

**运行**:

```bash
npm run build
# 自动生成报告: dist/report.html
```

#### 3. 验证脚本

**运行验证**:

```bash
# 验证 ClientLibs 一致性
npm run validate

# 验证并构建
npm run build:validate
```

### 生产环境验证

#### 1. 检查输出文件

```bash
# 检查 CSS 文件是否存在
ls -la ui.apps/src/main/content/jcr_root/apps/myapp/clientlibs/components/hero/css/

# 检查 .content.xml 是否存在
cat ui.apps/src/main/content/jcr_root/apps/myapp/clientlibs/components/hero/.content.xml
```

#### 2. AEM 中验证

**使用 AEM ClientLibs 工具**:

```
URL: http://localhost:4502/libs/granite/ui/content/dumplibs.html
```

**检查项目**:
1. 查找 categories: `myapp.components.hero`
2. 检查 CSS 文件路径
3. 检查依赖关系

#### 3. 浏览器中验证

**检查网络请求**:
1. 打开浏览器开发者工具
2. 切换到 Network 标签
3. 刷新页面
4. 检查 CSS 文件是否加载
5. 检查文件路径是否正确: `/etc.clientlibs/myapp/clientlibs/components/hero.css`

**检查样式应用**:
1. 使用 Elements 标签检查元素
2. 验证 CSS 类名是否正确应用
3. 检查样式是否生效

---

## 总结

### 核心要点

1. **构建链的必要性**: 实际项目中需要使用预处理器（Sass/SCSS）和构建工具（Webpack/Vite）
2. **ui.frontend 模块**: 前端源代码和构建配置的独立模块
3. **输出路径映射**: 源代码路径 → 输出路径的映射必须与 AEM ClientLibs 结构一致
4. **Categories 一致性**: 通过配置文件、自动生成、验证脚本确保命名一致性
5. **自动化流程**: 使用 Webpack/Vite 插件自动生成 .content.xml，减少手动操作

### 工作流程

```
编写 SCSS 源代码
  ↓
Webpack/Vite 编译
  ↓
输出到 ui.apps/clientlibs/
  ↓
自动生成 .content.xml
  ↓
验证一致性
  ↓
Maven 打包
  ↓
部署到 AEM
```

### 关键文件

- **源代码**: `ui.frontend/src/main/webpack/**/*.scss`
- **配置文件**: `ui.frontend/clientlibs.config.js`
- **构建配置**: `ui.frontend/webpack.config.js`
- **输出目录**: `ui.apps/src/main/content/jcr_root/apps/myapp/clientlibs/`
- **ClientLib 配置**: `ui.apps/.../clientlibs/**/.content.xml`

### 最佳实践清单

- ✅ 使用配置文件驱动的方式管理 ClientLibs
- ✅ 自动生成 .content.xml 文件
- ✅ 使用验证脚本确保一致性
- ✅ 合理组织目录结构
- ✅ 遵循命名规范
- ✅ 优化构建性能
- ✅ 集成 CI/CD 流程

---

## 相关文档

- [CSS 管理概述](./01-css-management-overview.md)
- [ClientLibs 配置详解](./02-clientlibs-configuration.md)
- [CSS 查找机制](./03-css-lookup-mechanism.md)
- [组件 HTL 模板](../02-components/05-component-htl-template.html)

