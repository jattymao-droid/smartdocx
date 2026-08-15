package com.ruoyi.system.domain.education;

public class EduQbDuplicateCheckBody
{
    private Long subjectId;

    private String content;

    /** Exclude self when editing an existing question. */
    private Long questionId;

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }
}
