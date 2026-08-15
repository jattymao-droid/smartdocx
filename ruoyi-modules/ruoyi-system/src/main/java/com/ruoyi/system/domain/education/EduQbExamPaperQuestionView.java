package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduQbExamPaperQuestionView extends EduQbQuestion
{
    private static final long serialVersionUID = 1L;

    private Long itemId;
    private Integer orderNum;
    private BigDecimal scoreValue;
    private String sectionName;

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
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
}
