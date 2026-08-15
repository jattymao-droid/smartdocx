package com.ruoyi.system.domain.education;

import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduQbQuestionType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long typeId;

    private String typeCode;

    private String typeName;

    /** choice | multi | judge | fill | subjective */
    private String answerMode;

    private Integer contentMaxLen;

    private Integer orderNum;

    /** 0=normal 1=disabled */
    private String status;

    /** 1=builtin cannot delete */
    private String builtin;

    public Long getTypeId()
    {
        return typeId;
    }

    public void setTypeId(Long typeId)
    {
        this.typeId = typeId;
    }

    public String getTypeCode()
    {
        return typeCode;
    }

    public void setTypeCode(String typeCode)
    {
        this.typeCode = typeCode;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }

    public String getAnswerMode()
    {
        return answerMode;
    }

    public void setAnswerMode(String answerMode)
    {
        this.answerMode = answerMode;
    }

    public Integer getContentMaxLen()
    {
        return contentMaxLen;
    }

    public void setContentMaxLen(Integer contentMaxLen)
    {
        this.contentMaxLen = contentMaxLen;
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

    public String getBuiltin()
    {
        return builtin;
    }

    public void setBuiltin(String builtin)
    {
        this.builtin = builtin;
    }
}
