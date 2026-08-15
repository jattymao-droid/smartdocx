package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduLibraryTopic extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long topicId;
    private String title;
    private String summary;
    private String coverUrl;
    private BigDecimal bundlePrice;
    private Integer downloadCount;
    private Integer orderNum;
    private String status;
    private String delFlag;
    private Integer docCount;
    /** Zip document shown in library lists; opens this topic page when clicked */
    private Long bundleDocumentId;
    private Long[] documentIds;
    private List<EduLibraryDocument> documents;

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public BigDecimal getBundlePrice() { return bundlePrice; }
    public void setBundlePrice(BigDecimal bundlePrice) { this.bundlePrice = bundlePrice; }
    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public Integer getDocCount() { return docCount; }
    public void setDocCount(Integer docCount) { this.docCount = docCount; }
    public Long getBundleDocumentId() { return bundleDocumentId; }
    public void setBundleDocumentId(Long bundleDocumentId) { this.bundleDocumentId = bundleDocumentId; }
    public Long[] getDocumentIds() { return documentIds; }
    public void setDocumentIds(Long[] documentIds) { this.documentIds = documentIds; }
    public List<EduLibraryDocument> getDocuments() { return documents; }
    public void setDocuments(List<EduLibraryDocument> documents) { this.documents = documents; }
}
