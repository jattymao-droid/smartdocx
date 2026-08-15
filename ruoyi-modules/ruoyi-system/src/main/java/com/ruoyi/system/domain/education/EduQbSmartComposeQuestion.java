package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduQbSmartComposeQuestion
{
    private Long questionId;
    private String questionCode;
    private String content;
    private String questionType;
    private BigDecimal difficulty;
    private String options;
    private String images;
    private BigDecimal scoreValue;
    private Integer orderNum;

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }

    public String getQuestionCode()
    {
        return questionCode;
    }

    public void setQuestionCode(String questionCode)
    {
        this.questionCode = questionCode;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getQuestionType()
    {
        return questionType;
    }

    public void setQuestionType(String questionType)
    {
        this.questionType = questionType;
    }

    public BigDecimal getDifficulty()
    {
        return difficulty;
    }

    public void setDifficulty(BigDecimal difficulty)
    {
        this.difficulty = difficulty;
    }

    public String getOptions()
    {
        return options;
    }

    public void setOptions(String options)
    {
        this.options = options;
    }

    public String getImages()
    {
        return images;
    }

    public void setImages(String images)
    {
        this.images = images;
    }

    public BigDecimal getScoreValue()
    {
        return scoreValue;
    }

    public void setScoreValue(BigDecimal scoreValue)
    {
        this.scoreValue = scoreValue;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }
}
