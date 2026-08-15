package com.ruoyi.system.domain.education;

public class EduLibraryRecommendBody
{
    private Long[] documentIds;
    private String recommendFlag;
    private Integer recommendOrder;

    public Long[] getDocumentIds() { return documentIds; }
    public void setDocumentIds(Long[] documentIds) { this.documentIds = documentIds; }
    public String getRecommendFlag() { return recommendFlag; }
    public void setRecommendFlag(String recommendFlag) { this.recommendFlag = recommendFlag; }
    public Integer getRecommendOrder() { return recommendOrder; }
    public void setRecommendOrder(Integer recommendOrder) { this.recommendOrder = recommendOrder; }
}
