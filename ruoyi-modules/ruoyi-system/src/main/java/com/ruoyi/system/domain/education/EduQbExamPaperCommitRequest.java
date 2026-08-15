package com.ruoyi.system.domain.education;

import java.util.List;

public class EduQbExamPaperCommitRequest
{
    private Long paperId;
    private String paperTitle;
    private Long subjectId;
    private String examCategory;
    private String examYear;
    private String region;
    private String grade;
    private String sourceFile;
    private String publishStatus;
    private List<EduQbExamPaperMarkItem> items;

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

    public String getSourceFile()
    {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile)
    {
        this.sourceFile = sourceFile;
    }

    public String getPublishStatus()
    {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus)
    {
        this.publishStatus = publishStatus;
    }

    public List<EduQbExamPaperMarkItem> getItems()
    {
        return items;
    }

    public void setItems(List<EduQbExamPaperMarkItem> items)
    {
        this.items = items;
    }
}
