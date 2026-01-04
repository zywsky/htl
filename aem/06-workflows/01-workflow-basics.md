# AEM 工作流：第一部分 - 工作流基础

## 📖 什么是 AEM 工作流?

AEM 工作流是自动化业务流程的机制，用于管理和自动化内容生命周期中的各种任务，如审批、发布、翻译等。

## 🏗️ 工作流核心概念

### 1. 工作流模型 (Workflow Model)
定义工作流的步骤和流程

### 2. 工作流步骤 (Workflow Step)
工作流中的单个任务单元

### 3. 工作流进程 (Workflow Process)
执行实际业务逻辑的 Java 类

### 4. 工作流启动器 (Workflow Launcher)
自动触发工作流的机制

## 💻 创建工作流进程步骤

### 示例 1：基础工作流进程

```java
package com.example.core.workflow;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Node;

/**
 * 基础工作流进程步骤
 * 
 * WorkflowProcess: 工作流进程接口
 * 实现 execute() 方法来定义进程逻辑
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=示例工作流进程"
    }
)
public class BasicWorkflowProcess implements WorkflowProcess {

    /**
     * 执行工作流进程
     * 
     * @param item 工作项（包含工作流数据和元数据）
     * @param session 工作流会话
     * @param args 进程参数（从工作流模型配置）
     */
    @Override
    public void execute(WorkItem item, WorkflowSession session, MetaDataMap args) {
        try {
            // 获取工作流数据
            String payload = item.getWorkflowData().getPayload().toString();
            
            // 获取资源解析器
            ResourceResolver resolver = session.adaptTo(ResourceResolver.class);
            
            if (resolver != null) {
                // 获取资源
                javax.jcr.Session jcrSession = resolver.adaptTo(javax.jcr.Session.class);
                Node node = jcrSession.getNode(payload);
                
                // 执行逻辑（例如：设置属性）
                node.setProperty("workflowProcessed", true);
                node.setProperty("workflowProcessedDate", 
                    new java.util.Date().toString());
                
                // 保存更改
                jcrSession.save();
                
                System.out.println("工作流进程已处理: " + payload);
            }
            
        } catch (Exception e) {
            // 记录错误
            System.err.println("工作流进程错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### 示例 2：内容审批工作流进程

```java
package com.example.core.workflow;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Node;

/**
 * 内容审批工作流进程
 * 
 * 检查内容是否符合发布标准
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=内容审批进程"
    }
)
public class ContentApprovalProcess implements WorkflowProcess {

    @Override
    public void execute(WorkItem item, WorkflowSession session, MetaDataMap args) {
        try {
            String payload = item.getWorkflowData().getPayload().toString();
            ResourceResolver resolver = session.adaptTo(ResourceResolver.class);
            
            if (resolver != null) {
                javax.jcr.Session jcrSession = resolver.adaptTo(javax.jcr.Session.class);
                Node node = jcrSession.getNode(payload);
                
                // 获取内容属性
                String title = node.hasProperty("jcr:title") ? 
                    node.getProperty("jcr:title").getString() : "";
                String description = node.hasProperty("jcr:description") ? 
                    node.getProperty("jcr:description").getString() : "";
                
                // 审批逻辑
                boolean approved = false;
                String approvalStatus = "待审批";
                
                // 检查内容是否完整
                if (title != null && !title.trim().isEmpty() &&
                    description != null && !description.trim().isEmpty()) {
                    approved = true;
                    approvalStatus = "已批准";
                }
                
                // 保存审批状态
                node.setProperty("approvalStatus", approvalStatus);
                node.setProperty("approved", approved);
                node.setProperty("approvalDate", 
                    new java.util.Date().toString());
                
                jcrSession.save();
                
                // 将审批结果保存到工作流元数据
                MetaDataMap metaData = item.getWorkflowData().getMetaDataMap();
                metaData.put("approved", approved);
                metaData.put("approvalStatus", approvalStatus);
            }
            
        } catch (Exception e) {
            System.err.println("审批进程错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### 示例 3：发送通知的工作流进程

```java
package com.example.core.workflow;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * 发送通知的工作流进程
 * 
 * 在工作流执行时发送通知
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=发送通知进程"
    }
)
public class NotificationProcess implements WorkflowProcess {

    // 可以注入其他服务（如邮件服务）
    // @Reference
    // private EmailService emailService;

    @Override
    public void execute(WorkItem item, WorkflowSession session, MetaDataMap args) {
        try {
            // 获取工作流参数（从工作流模型配置）
            String recipient = args.get("recipient", "");
            String subject = args.get("subject", "工作流通知");
            String message = args.get("message", "");
            
            // 获取工作流数据
            String payload = item.getWorkflowData().getPayload().toString();
            
            // 构建通知消息
            String notificationMessage = buildNotificationMessage(
                subject, message, payload);
            
            // 发送通知（这里只是打印，实际应该调用邮件服务等）
            System.out.println("发送通知到: " + recipient);
            System.out.println("主题: " + subject);
            System.out.println("消息: " + notificationMessage);
            
            // 实际实现可能是：
            // if (emailService != null) {
            //     emailService.sendEmail(recipient, subject, notificationMessage);
            // }
            
        } catch (Exception e) {
            System.err.println("通知进程错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 构建通知消息
     */
    private String buildNotificationMessage(String subject, String message, String payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("工作流通知\n");
        sb.append("==================\n");
        sb.append("主题: ").append(subject).append("\n");
        sb.append("消息: ").append(message).append("\n");
        sb.append("资源路径: ").append(payload).append("\n");
        sb.append("时间: ").append(new java.util.Date().toString());
        return sb.toString();
    }
}
```

## 📋 工作流模型定义（XML）

工作流模型通常在 `/conf/global/settings/workflow/models` 下定义，也可以通过代码创建：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
          jcr:primaryType="cq:WorkflowModel"
          jcr:title="内容审批工作流"
          description="自动审批和发布内容">
    
    <nodes jcr:primaryType="nt:unstructured">
        <!-- 开始节点 -->
        <start jcr:primaryType="cq:WorkflowNode"
               title="开始"
               type="START"/>
        
        <!-- 审批进程步骤 -->
        <approval jcr:primaryType="cq:WorkflowNode"
                  title="内容审批"
                  type="PROCESS"
                  description="检查内容是否符合发布标准">
            <metaData jcr:primaryType="nt:unstructured">
                <PROCESS jcr:primaryType="nt:unstructured"
                         jcr:title="内容审批进程"
                         process="com.example.core.workflow.ContentApprovalProcess"/>
            </metaData>
        </approval>
        
        <!-- 参与者步骤（需要人工审批） -->
        <participant jcr:primaryType="cq:WorkflowNode"
                     title="管理员审批"
                     type="PARTICIPANT"
                     description="等待管理员审批">
            <metaData jcr:primaryType="nt:unstructured">
                <PARTICIPANT jcr:primaryType="nt:unstructured"
                             jcr:title="管理员"
                             participantId="admin"/>
            </metaData>
        </participant>
        
        <!-- 通知进程步骤 -->
        <notification jcr:primaryType="cq:WorkflowNode"
                      title="发送通知"
                      type="PROCESS">
            <metaData jcr:primaryType="nt:unstructured">
                <PROCESS jcr:primaryType="nt:unstructured"
                         jcr:title="发送通知进程"
                         process="com.example.core.workflow.NotificationProcess">
                    <args jcr:primaryType="nt:unstructured">
                        <recipient>admin@example.com</recipient>
                        <subject>内容已审批</subject>
                        <message>内容已通过审批流程</message>
                    </args>
                </PROCESS>
            </metaData>
        </notification>
        
        <!-- 结束节点 -->
        <end jcr:primaryType="cq:WorkflowNode"
             title="结束"
             type="END"/>
    </nodes>
    
    <!-- 定义连接（步骤之间的流程） -->
    <transitions jcr:primaryType="nt:unstructured">
        <start-approval jcr:primaryType="cq:WorkflowTransition"
                       from="start"
                       to="approval"/>
        <approval-participant jcr:primaryType="cq:WorkflowTransition"
                             from="approval"
                             to="participant"/>
        <participant-notification jcr:primaryType="cq:WorkflowTransition"
                                 from="participant"
                                 to="notification"/>
        <notification-end jcr:primaryType="cq:WorkflowTransition"
                         from="notification"
                         to="end"/>
    </transitions>
</jcr:root>
```

## 🔑 关键要点

1. **WorkflowProcess 接口**：实现 execute() 方法定义进程逻辑
2. **WorkItem**：包含工作流数据和元数据
3. **WorkflowSession**：提供访问 JCR 的能力
4. **MetaDataMap**：用于传递参数和保存结果
5. **工作流模型**：定义工作流的步骤和流程

## ➡️ 下一步

继续学习 AEM 的其他高级主题，如内容服务、REST API 等。

