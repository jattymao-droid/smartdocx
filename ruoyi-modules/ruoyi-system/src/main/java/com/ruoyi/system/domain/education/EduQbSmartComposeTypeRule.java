package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduQbSmartComposeTypeRule
{
    private String questionType;
    private Integer count;
    private BigDecimal scorePerQuestion;

    public String getQuestionType()
    {
        return questionType;
    }

    public void setQuestionType(String questionType)
    {
        this.questionType = questionType;
    }

    public Integer getCount()
    {
        return count;
    }

    public void setCount(Integer count)
    {
        this.count = count;
    }

    public BigDecimal getScorePerQuestion()
    {
        return scorePerQuestion;
    }

    public void setScorePerQuestion(BigDecimal scorePerQuestion)
    {
        this.scorePerQuestion = scorePerQuestion;
    }
}
