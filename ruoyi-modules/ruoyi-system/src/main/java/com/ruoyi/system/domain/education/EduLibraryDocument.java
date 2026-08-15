package com.ruoyi.system.domain.education;

import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduLibraryDocument extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long documentId;
    private String documentCode;
    private String title;
    private String summary;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String fileUrl;
    private String fileStorage;
    private String coverUrl;
    private Integer pageCount;
    private Long subjectId;
    private String subjectName;
    private String schoolStage;
    private Long versionId;
    private String versionName;
    private Long textbookId;
    private String textbookName;
    private Long chapterId;
    private String chapterText;
    private Long categoryId;
    private String categoryName;
    private String tagNames;
    private String visibility;
    private String allowDownload;
    private java.math.BigDecimal downloadPrice;
    private Integer downloadCount;
    private Integer viewCount;
    private Integer favoriteCount;
    private String convertStatus;
    private String previewType;
    private String previewUrl;
    private String previewError;
    private String auditStatus;
    private String auditBy;
    private java.util.Date auditTime;
    private String auditRemark;
    private String recommendFlag;
    private Integer recommendOrder;
    private String status;
    private String delFlag;

    /** query */
    private String keyword;
    private String fileExtFilter;
    private Boolean favorited;
    private java.math.BigDecimal readProgress;
    /** When set, this archive document opens the linked hot topic in portal lists */
    private Long topicId;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getDocumentCode() { return documentCode; }
    public void setDocumentCode(String documentCode) { this.documentCode = documentCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileExt() { return fileExt; }
    public void setFileExt(String fileExt) { this.fileExt = fileExt; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileStorage() { return fileStorage; }
    public void setFileStorage(String fileStorage) { this.fileStorage = fileStorage; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getSchoolStage() { return schoolStage; }
    public void setSchoolStage(String schoolStage) { this.schoolStage = schoolStage; }
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }
    public Long getTextbookId() { return textbookId; }
    public void setTextbookId(Long textbookId) { this.textbookId = textbookId; }
    public String getTextbookName() { return textbookName; }
    public void setTextbookName(String textbookName) { this.textbookName = textbookName; }
    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    public String getChapterText() { return chapterText; }
    public void setChapterText(String chapterText) { this.chapterText = chapterText; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getTagNames() { return tagNames; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getAllowDownload() { return allowDownload; }
    public void setAllowDownload(String allowDownload) { this.allowDownload = allowDownload; }
    public java.math.BigDecimal getDownloadPrice() { return downloadPrice; }
    public void setDownloadPrice(java.math.BigDecimal downloadPrice) { this.downloadPrice = downloadPrice; }
    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Integer getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Integer favoriteCount) { this.favoriteCount = favoriteCount; }
    public String getConvertStatus() { return convertStatus; }
    public void setConvertStatus(String convertStatus) { this.convertStatus = convertStatus; }
    public String getPreviewType() { return previewType; }
    public void setPreviewType(String previewType) { this.previewType = previewType; }
    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    public String getPreviewError() { return previewError; }
    public void setPreviewError(String previewError) { this.previewError = previewError; }
    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(String auditStatus) { this.auditStatus = auditStatus; }
    public String getAuditBy() { return auditBy; }
    public void setAuditBy(String auditBy) { this.auditBy = auditBy; }
    public java.util.Date getAuditTime() { return auditTime; }
    public void setAuditTime(java.util.Date auditTime) { this.auditTime = auditTime; }
    public String getAuditRemark() { return auditRemark; }
    public void setAuditRemark(String auditRemark) { this.auditRemark = auditRemark; }
    public String getRecommendFlag() { return recommendFlag; }
    public void setRecommendFlag(String recommendFlag) { this.recommendFlag = recommendFlag; }
    public Integer getRecommendOrder() { return recommendOrder; }
    public void setRecommendOrder(Integer recommendOrder) { this.recommendOrder = recommendOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getFileExtFilter() { return fileExtFilter; }
    public void setFileExtFilter(String fileExtFilter) { this.fileExtFilter = fileExtFilter; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
    public java.math.BigDecimal getReadProgress() { return readProgress; }
    public void setReadProgress(java.math.BigDecimal readProgress) { this.readProgress = readProgress; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
}
