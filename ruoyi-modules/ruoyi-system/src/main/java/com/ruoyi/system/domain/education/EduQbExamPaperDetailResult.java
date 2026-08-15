package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import java.util.List;

public class EduQbExamPaperDetailResult
{
    private Long paperId;
    private String paperTitle;
    private Long subjectId;
    private String examCategory;
    private String examYear;
    private String region;
    private String grade;
    private BigDecimal totalScore;
    private Integer itemCount;
    private String createTime;
    private List<EduQbExamPaperQuestionView> questions;

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

    public BigDecimal getTotalScore()
    {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore)
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

    public List<EduQbExamPaperQuestionView> getQuestions()
    {
        return questions;
    }

    public void setQuestions(List<EduQbExamPaperQuestionView> questions)
    {
        this.questions = questions;
    }
}
