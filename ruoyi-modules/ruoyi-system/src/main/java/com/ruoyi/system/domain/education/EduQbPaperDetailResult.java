package com.ruoyi.system.domain.education;

import java.util.Map;

public class EduQbPaperDetailResult
{
    private Long paperId;
    private String paperTitle;
    private String templateCode;
    private String sortMode;
    private String exportMode;
    private String answerLayout;
    private Map<String, String> header;
    private Map<String, Object> exportConfig;
    private String groupTab;
    private String orderRadio;
    private String paperTemplate;
    private String exportFormat;
    private String pageLayout;
    private Map<String, Object> answerAreas;
    private Object basketItems;
    private java.math.BigDecimal totalScore;
    private Integer itemCount;
    private String createTime;

    public Long getPaperId()
    {
        return paperId;
    }

    public void setPaperId(Long paperId)
    {
        this.paperId = paperId;
    }

    public String getPaperTitle()
    {
        return paperTitle;
    }

    public void setPaperTitle(String paperTitle)
    {
        this.paperTitle = paperTitle;
    }

    public String getTemplateCode()
    {
        return templateCode;
    }

    public void setTemplateCode(String templateCode)
    {
        this.templateCode = templateCode;
    }

    public String getSortMode()
    {
        return sortMode;
    }

    public void setSortMode(String sortMode)
    {
        this.sortMode = sortMode;
    }

    public String getExportMode()
    {
        return exportMode;
    }

    public void setExportMode(String exportMode)
    {
        this.exportMode = exportMode;
    }

    public String getAnswerLayout()
    {
        return answerLayout;
    }

    public void setAnswerLayout(String answerLayout)
    {
        this.answerLayout = answerLayout;
    }

    public Map<String, String> getHeader()
    {
        return header;
    }

    public void setHeader(Map<String, String> header)
    {
        this.header = header;
    }

    public Map<String, Object> getExportConfig()
    {
        return exportConfig;
    }

    public void setExportConfig(Map<String, Object> exportConfig)
    {
        this.exportConfig = exportConfig;
    }

    public String getGroupTab()
    {
        return groupTab;
    }

    public void setGroupTab(String groupTab)
    {
        this.groupTab = groupTab;
    }

    public String getOrderRadio()
    {
        return orderRadio;
    }

    public void setOrderRadio(String orderRadio)
    {
        this.orderRadio = orderRadio;
    }

    public String getPaperTemplate()
    {
        return paperTemplate;
    }

    public void setPaperTemplate(String paperTemplate)
    {
        this.paperTemplate = paperTemplate;
    }

    public String getExportFormat()
    {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat)
    {
        this.exportFormat = exportFormat;
    }

    public String getPageLayout()
    {
        return pageLayout;
    }

    public void setPageLayout(String pageLayout)
    {
        this.pageLayout = pageLayout;
    }

    public Map<String, Object> getAnswerAreas()
    {
        return answerAreas;
    }

    public void setAnswerAreas(Map<String, Object> answerAreas)
    {
        this.answerAreas = answerAreas;
    }

    public Object getBasketItems()
    {
        return basketItems;
    }

    public void setBasketItems(Object basketItems)
    {
        this.basketItems = basketItems;
    }

    public java.math.BigDecimal getTotalScore()
    {
        return totalScore;
    }

    public void setTotalScore(java.math.BigDecimal totalScore)
    {
        this.totalScore = totalScore;
    }

    public Integer getItemCount()
    {
        return itemCount;
    }

    public void setItemCount(Integer itemCount)
    {
        this.itemCount = itemCount;
    }

    public String getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(String createTime)
    {
        this.createTime = createTime;
    }
}
