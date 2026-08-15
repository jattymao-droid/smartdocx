package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduQbPracticeStats
{
    private Integer sessionCount;
    private Integer totalQuestions;
    private Integer totalCorrect;
    private Integer totalChoice;
    private BigDecimal avgChoiceRate;

    public Integer getSessionCount()
    {
        return sessionCount;
    }

    public void setSessionCount(Integer sessionCount)
    {
        this.sessionCount = sessionCount;
    }

    public Integer getTotalQuestions()
    {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions)
    {
        this.totalQuestions = totalQuestions;
    }

    public Integer getTotalCorrect()
    {
        return totalCorrect;
    }

    public void setTotalCorrect(Integer totalCorrect)
    {
        this.totalCorrect = totalCorrect;
    }

    public Integer getTotalChoice()
    {
        return totalChoice;
    }

    public void setTotalChoice(Integer totalChoice)
    {
        this.totalChoice = totalChoice;
    }

    public BigDecimal getAvgChoiceRate()
    {
        return avgChoiceRate;
    }

    public void setAvgChoiceRate(BigDecimal avgChoiceRate)
    {
        this.avgChoiceRate = avgChoiceRate;
    }
}
