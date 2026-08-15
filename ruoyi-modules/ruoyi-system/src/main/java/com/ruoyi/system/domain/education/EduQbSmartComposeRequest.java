package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import java.util.List;

public class EduQbSmartComposeRequest
{
    private Long subjectId;
    private Long chapterId;
    private String chapterText;
    private String paperTitle;
    /** unit | midterm | final | custom */
    private String templateCode;
    private BigDecimal difficultyMin;
    private BigDecimal difficultyMax;
    private Integer easyPercent;
    private Integer mediumPercent;
    private Integer hardPercent;
    private List<EduQbSmartComposeTypeRule> typeRules;
    private List<Long> excludeQuestionIds;
    private List<Long> chapterIds;

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public Long getChapterId()
    {
        return chapterId;
    }

    public void setChapterId(Long chapterId)
    {
        this.chapterId = chapterId;
    }

    public String getChapterText()
    {
        return chapterText;
    }

    public void setChapterText(String chapterText)
    {
        this.chapterText = chapterText;
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

    public BigDecimal getDifficultyMin()
    {
        return difficultyMin;
    }

    public void setDifficultyMin(BigDecimal difficultyMin)
    {
        this.difficultyMin = difficultyMin;
    }

    public BigDecimal getDifficultyMax()
    {
        return difficultyMax;
    }

    public void setDifficultyMax(BigDecimal difficultyMax)
    {
        this.difficultyMax = difficultyMax;
    }

    public Integer getEasyPercent()
    {
        return easyPercent;
    }

    public void setEasyPercent(Integer easyPercent)
    {
        this.easyPercent = easyPercent;
    }

    public Integer getMediumPercent()
    {
        return mediumPercent;
    }

    public void setMediumPercent(Integer mediumPercent)
    {
        this.mediumPercent = mediumPercent;
    }

    public Integer getHardPercent()
    {
        return hardPercent;
    }

    public void setHardPercent(Integer hardPercent)
    {
        this.hardPercent = hardPercent;
    }

    public List<EduQbSmartComposeTypeRule> getTypeRules()
    {
        return typeRules;
    }

    public void setTypeRules(List<EduQbSmartComposeTypeRule> typeRules)
    {
        this.typeRules = typeRules;
    }

    public List<Long> getExcludeQuestionIds()
    {
        return excludeQuestionIds;
    }

    public void setExcludeQuestionIds(List<Long> excludeQuestionIds)
    {
        this.excludeQuestionIds = excludeQuestionIds;
    }

    public List<Long> getChapterIds()
    {
        return chapterIds;
    }

    public void setChapterIds(List<Long> chapterIds)
    {
        this.chapterIds = chapterIds;
    }
}
