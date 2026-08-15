package com.ruoyi.system.domain.education;

import java.util.List;

public class EduQbWrongComposeRequest
{
    private Long subjectId;
    private String paperTitle;
    private List<Long> questionIds;
    private Integer limit;

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getPaperTitle()
    {
        return paperTitle;
    }

    public void setPaperTitle(String paperTitle)
    {
        this.paperTitle = paperTitle;
    }

    public List<Long> getQuestionIds()
    {
        return questionIds;
    }

    public void setQuestionIds(List<Long> questionIds)
    {
        this.questionIds = questionIds;
    }

    public Integer getLimit()
    {
        return limit;
    }

    public void setLimit(Integer limit)
    {
        this.limit = limit;
    }
}
