package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduQbPaper extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long paperId;
    private String paperTitle;
    private String templateCode;
    private BigDecimal totalScore;
    private String sortRule;
    private String exportConfig;
    private Integer itemCount;
    private String paperType;
    private Long subjectId;
    private String examCategory;
    private String examYear;
    private String region;
    private String grade;
    private String publishStatus;
    private String sourceFile;

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

    public BigDecimal getTotalScore()
    {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore)
    {
        this.totalScore = totalScore;
    }

    public String getSortRule()
    {
        return sortRule;
    }

    public void setSortRule(String sortRule)
    {
        this.sortRule = sortRule;
    }

    public String getExportConfig()
    {
        return exportConfig;
    }

    public void setExportConfig(String exportConfig)
    {
        this.exportConfig = exportConfig;
    }

    public Integer getItemCount()
    {
        return itemCount;
    }

    public void setItemCount(Integer itemCount)
    {
        this.itemCount = itemCount;
    }

    public String getPaperType()
    {
        return paperType;
    }

    public void setPaperType(String paperType)
    {
        this.paperType = paperType;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getExamCategory()
    {
        return examCategory;
    }

    public void setExamCategory(String examCategory)
    {
        this.examCategory = examCategory;
    }

    public String getExamYear()
    {
        return examYear;
    }

    public void setExamYear(String examYear)
    {
        this.examYear = examYear;
    }

    public String getRegion()
    {
        return region;
    }

    public void setRegion(String region)
    {
        this.region = region;
    }

    public String getGrade()
    {
        return grade;
    }

    public void setGrade(String grade)
    {
        this.grade = grade;
    }

    public String getPublishStatus()
    {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus)
    {
        this.publishStatus = publishStatus;
    }

    public String getSourceFile()
    {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile)
    {
        this.sourceFile = sourceFile;
    }
}
