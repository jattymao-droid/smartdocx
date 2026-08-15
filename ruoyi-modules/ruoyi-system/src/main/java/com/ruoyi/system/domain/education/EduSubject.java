package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduSubject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long subjectId;

    private String subjectName;

    private BigDecimal fullScore;

    private Integer orderNum;

    private String status;

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getSubjectName()
    {
        return subjectName;
    }

    public void setSubjectName(String subjectName)
    {
        this.subjectName = subjectName;
    }

    public BigDecimal getFullScore()
    {
        return fullScore;
    }

    public void setFullScore(BigDecimal fullScore)
    {
        this.fullScore = fullScore;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
