# HTL 安全最佳实践

## 安全概述

安全性是 HTL 组件开发中的重要考虑因素。本指南介绍如何确保 HTL 组件的安全性。

## 1. XSS 防护

### HTL 自动转义

HTL 默认会自动转义所有输出，防止 XSS 攻击：

```html
<!-- 安全：自动转义 -->
<div>${properties.userInput}</div>
<!-- 如果 userInput 包含 <script>alert('XSS')</script>，会被转义为文本 -->
```

### 谨慎使用 data-sly-unescape

```html
<!-- 危险：只在确定内容安全时使用 -->
<div data-sly-unescape="${properties.trustedHtmlContent}"></div>

<!-- 更好的方式：使用 Use API 清理 HTML -->
<sly data-sly-use.sanitizer="${'com.example.HtmlSanitizer' @ html=properties.htmlContent}"></sly>
<div data-sly-unescape="${sanitizer.sanitizedHtml}"></div>
```

## 2. 输入验证

### 在 HTL 中验证

```html
<sly data-sly-set.isValid="${properties.email && properties.email.contains('@')}"></sly>
<div data-sly-test="${isValid}">
    ${properties.email}
</div>
<div data-sly-test="${!isValid}" class="error">
    无效的邮箱地址
</div>
```

### 在 Sling Model 中验证

```java
@Model(adaptables = Resource.class)
public interface ValidatedModel {
    @Inject
    String getEmail();
    
    default boolean isValidEmail() {
        return getEmail() != null && 
               getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}
```

## 3. 路径遍历防护

### 验证路径

```html
<sly data-sly-use.validator="${'com.example.PathValidator' @ path=properties.filePath}"></sly>
<div data-sly-test="${validator.isValid}">
    <img src="${validator.safePath}">
</div>
```

## 4. SQL 注入防护

### 使用参数化查询

虽然 HTL 本身不直接涉及数据库，但在 Use API 中：

```java
// 正确：使用参数化查询
String sql = "SELECT * FROM users WHERE id = ?";
PreparedStatement stmt = connection.prepareStatement(sql);
stmt.setString(1, userId);

// 错误：字符串拼接
String sql = "SELECT * FROM users WHERE id = " + userId; // 危险！
```

## 5. CSRF 防护

### 使用 CSRF Token

```html
<form action="${properties.actionUrl}" method="post">
    <input type="hidden" 
           name=":cq_csrf_token" 
           value="${request.requestPathInfo.suffix}">
    <!-- 表单字段 -->
</form>
```

## 6. 敏感信息保护

### 不在 HTL 中暴露敏感信息

```html
<!-- 错误：暴露敏感信息 -->
<div>API Key: ${properties.apiKey}</div>

<!-- 正确：在服务器端处理 -->
<sly data-sly-use.service="${'com.example.ApiService' @ apiKey=properties.apiKey}"></sly>
<div>${service.getData()}</div>
```

## 7. 权限检查

### 检查用户权限

```html
<sly data-sly-use.auth="${'com.example.AuthHelper' @ request=request}"></sly>
<div data-sly-test="${auth.hasPermission('content/edit')}">
    <!-- 只有有权限的用户才能看到 -->
    <button>编辑</button>
</div>
```

## 8. 内容安全策略 (CSP)

### 设置 CSP 头

```html
<!-- 在页面级别设置 CSP -->
<meta http-equiv="Content-Security-Policy" 
      content="default-src 'self'; script-src 'self' 'unsafe-inline';">
```

## 9. HTTPS 强制

### 确保使用 HTTPS

```html
<!-- 在 Use API 中检查协议 -->
<sly data-sly-use.security="${'com.example.SecurityHelper' @ request=request}"></sly>
<div data-sly-test="${security.isSecure}">
    <!-- 安全内容 -->
</div>
```

## 10. 文件上传安全

### 验证文件类型和大小

```html
<sly data-sly-use.upload="${'com.example.FileUploadValidator' @ 
    file=properties.uploadedFile}"></sly>
<div data-sly-test="${upload.isValid}">
    <img src="${upload.safePath}">
</div>
<div data-sly-test="${!upload.isValid}" class="error">
    ${upload.errorMessage}
</div>
```

## 11. 会话管理

### 安全的会话处理

```html
<sly data-sly-use.session="${'com.example.SessionHelper' @ request=request}"></sly>
<div data-sly-test="${session.isValid}">
    <!-- 会话有效的内容 -->
</div>
```

## 12. 密码处理

### 永远不要在 HTL 中显示密码

```html
<!-- 错误：显示密码 -->
<div>密码: ${properties.password}</div>

<!-- 正确：在服务器端处理 -->
<sly data-sly-use.auth="${'com.example.AuthService' @ 
    username=properties.username,
    password=properties.password}"></sly>
<div data-sly-test="${auth.isAuthenticated}">
    登录成功
</div>
```

## 13. 错误处理

### 不暴露系统信息

```html
<!-- 错误：暴露详细错误信息 -->
<div class="error">${exception.getMessage()}</div>

<!-- 正确：显示用户友好的错误信息 -->
<div class="error">${model.userFriendlyErrorMessage}</div>
```

## 14. 安全配置

### 使用配置管理敏感设置

```html
<sly data-sly-use.config="${'com.example.SecurityConfig'}"></sly>
<div data-sly-test="${config.isFeatureEnabled('secure-feature')}">
    <!-- 功能内容 -->
</div>
```

## 15. 实际应用示例

### 安全的用户输入组件

```html
<sly data-sly-use.validator="${'com.example.InputValidator' @ 
    input=properties.userInput,
    type='text',
    maxLength=100}"></sly>

<div class="input-component">
    <input type="text"
           value="${validator.sanitizedInput}"
           data-sly-attribute.maxlength="${validator.maxLength}">
    <div data-sly-test="${validator.hasError}" class="error">
        ${validator.errorMessage}
    </div>
</div>
```

### 安全的链接组件

```html
<sly data-sly-use.link="${'com.example.LinkValidator' @ 
    url=properties.linkUrl}"></sly>

<a data-sly-test="${link.isValid}"
   href="${link.safeUrl}"
   data-sly-attribute="${{
       'target': link.isExternal ? '_blank' : '_self',
       'rel': link.isExternal ? 'noopener noreferrer' : ''
   }}">
    ${properties.linkText}
</a>
```

## 安全检查清单

- [ ] 所有用户输入都已验证
- [ ] 使用 HTL 自动转义（避免不必要的 unescape）
- [ ] 敏感信息不在 HTL 中暴露
- [ ] 路径已验证，防止路径遍历
- [ ] 权限已检查
- [ ] CSRF 保护已实现
- [ ] 错误消息不暴露系统信息
- [ ] 文件上传已验证
- [ ] 使用 HTTPS
- [ ] 会话管理安全
- [ ] 密码正确处理
- [ ] 安全配置已设置

## 安全测试

### 1. 输入验证测试

测试各种恶意输入：
- SQL 注入尝试
- XSS 尝试
- 路径遍历尝试
- 特殊字符

### 2. 权限测试

- 测试未授权访问
- 测试权限提升
- 测试会话劫持

### 3. 渗透测试

- 使用工具如 OWASP ZAP
- 手动安全测试
- 代码审查

## 安全资源

- OWASP Top 10
- AEM 安全文档
- HTL 安全最佳实践
- Sling 安全指南

## 总结

1. **信任但验证**: 验证所有用户输入
2. **最小权限**: 只授予必要的权限
3. **深度防御**: 多层安全措施
4. **保持更新**: 及时更新依赖和补丁
5. **安全审查**: 定期进行安全审查

