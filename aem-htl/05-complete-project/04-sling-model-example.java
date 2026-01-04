/**
 * Sling Model 示例
 * 
 * 这是一个完整的 Sling Model 示例，展示如何与 HTL 配合使用
 */

package com.myapp.components.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.osgi.annotation.versioning.ProviderType;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * 卡片组件的 Sling Model
 * 
 * 对应 HTL 模板: card.html
 */
@Model(
    adaptables = {SlingHttpServletRequest.class, Resource.class},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@ProviderType
public interface CardModel {
    
    // ========== 基础属性 ==========
    
    /**
     * 卡片标题
     */
    @ValueMapValue
    String getTitle();
    
    /**
     * 卡片副标题
     */
    @ValueMapValue
    String getSubtitle();
    
    /**
     * 卡片内容（HTML）
     */
    @ValueMapValue
    String getContent();
    
    /**
     * 卡片类型
     */
    @ValueMapValue
    @Named("cardType")
    String getCardType();
    
    // ========== 图片相关 ==========
    
    /**
     * 是否有图片
     */
    boolean hasImage();
    
    /**
     * 图片 URL
     */
    String getImageUrl();
    
    /**
     * 图片 Alt 文本
     */
    String getImageAlt();
    
    /**
     * 响应式图片 srcset
     */
    String getImageSrcset();
    
    /**
     * 响应式图片 sizes
     */
    String getImageSizes();
    
    /**
     * 是否懒加载图片
     */
    boolean isLazyLoadImage();
    
    // ========== 链接相关 ==========
    
    /**
     * 链接 URL
     */
    @ValueMapValue
    @Named("linkUrl")
    String getLinkUrl();
    
    /**
     * 链接文本
     */
    @ValueMapValue
    @Named("linkText")
    String getLinkText();
    
    /**
     * 链接 ARIA 标签
     */
    String getLinkAriaLabel();
    
    /**
     * 是否在新标签页打开
     */
    @ValueMapValue
    @Named("openInNewTab")
    boolean isOpenInNewTab();
    
    /**
     * 是否显示链接图标
     */
    @ValueMapValue
    @Named("showLinkIcon")
    boolean isShowLinkIcon();
    
    // ========== 徽章相关 ==========
    
    /**
     * 徽章文本
     */
    @ValueMapValue
    String getBadge();
    
    /**
     * 徽章类型
     */
    @ValueMapValue
    @Named("badgeType")
    String getBadgeType();
    
    // ========== 元数据相关 ==========
    
    /**
     * 是否显示元数据
     */
    @ValueMapValue
    @Named("showMeta")
    boolean isShowMeta();
    
    /**
     * 作者
     */
    @ValueMapValue
    String getAuthor();
    
    /**
     * 发布日期
     */
    @ValueMapValue
    @Named("publishDate")
    String getPublishDate();
    
    /**
     * 格式化的发布日期
     */
    String getFormattedPublishDate();
    
    /**
     * ISO 格式的发布日期
     */
    String getPublishDateISO();
    
    /**
     * 分类
     */
    @ValueMapValue
    String getCategory();
    
    /**
     * 分类 URL
     */
    String getCategoryUrl();
    
    // ========== 标签相关 ==========
    
    /**
     * 标签列表
     */
    List<Tag> getTags();
    
    // ========== 操作相关 ==========
    
    /**
     * 操作列表
     */
    List<Action> getActions();
    
    // ========== 其他 ==========
    
    /**
     * 是否为推荐卡片
     */
    boolean isFeatured();
    
    /**
     * 是否已配置
     */
    boolean isConfigured();
    
    /**
     * 内容 HTML（已处理的）
     */
    String getContentHtml();
    
    // ========== 内部类 ==========
    
    /**
     * 标签接口
     */
    interface Tag {
        String getLabel();
        String getUrl();
        String getType();
    }
    
    /**
     * 操作接口
     */
    interface Action {
        String getLabel();
        String getUrl();
        String getType();
        String getAriaLabel();
    }
}

/**
 * 实现类示例（简化版）
 * 
 * 注意：实际实现应该包含完整的业务逻辑
 */
/*
@Model(adaptables = Resource.class)
public class CardModelImpl implements CardModel {
    
    @Inject
    private Resource resource;
    
    @Inject
    private ValueMap properties;
    
    private static final String DEFAULT_CARD_TYPE = "default";
    
    @PostConstruct
    protected void init() {
        // 初始化逻辑
    }
    
    @Override
    public String getTitle() {
        return properties.get("title", String.class);
    }
    
    @Override
    public boolean hasImage() {
        String imagePath = properties.get("imagePath", String.class);
        return imagePath != null && !imagePath.isEmpty();
    }
    
    @Override
    public String getImageUrl() {
        String imagePath = properties.get("imagePath", String.class);
        if (imagePath == null) {
            return null;
        }
        // 转换为 URL（使用 DamUrlProvider 等）
        return imagePath;
    }
    
    @Override
    public boolean isConfigured() {
        return getTitle() != null || getContent() != null || hasImage();
    }
    
    // 其他方法实现...
}
*/

