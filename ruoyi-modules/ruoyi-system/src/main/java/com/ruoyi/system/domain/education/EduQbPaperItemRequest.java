package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduQbPaperItemRequest
{
    private Long questionId;
    private Integer orderNum;
    private BigDecimal scoreValue;
    private String sectionName;
    private Integer answerAreaLines;
    private String answerAreaStyle;

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public BigDecimal getScoreValue()
    {
        return scoreValue;
    }

    public void setScoreValue(BigDecimal scoreValue)
    {
        this.scoreValue = scoreValue;
    }

    public String getSectionName()
    {
        return sectionName;
    }

    public void setSectionName(String sectionName)
    {
        this.sectionName = sectionName;
    }

    public Integer getAnswerAreaLines()
    {
        return answerAreaLines;
    }

    public void setAnswerAreaLines(Integer answerAreaLines)
    {
        this.answerAreaLines = answerAreaLines;
    }

    public String getAnswerAreaStyle()
    {
        return answerAreaStyle;
    }

    public void setAnswerAreaStyle(String answerAreaStyle)
    {
        this.answerAreaStyle = answerAreaStyle;
    }
}
