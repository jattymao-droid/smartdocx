package com.ruoyi.system.domain.education;

public class EduQbTextbook
{
    private Long textbookId;
    private Long versionId;
    private String textbookName;
    private Integer orderNum;
    private String status;

    public Long getTextbookId()
    {
        return textbookId;
    }

    public void setTextbookId(Long textbookId)
    {
        this.textbookId = textbookId;
    }

    public Long getVersionId()
    {
        return versionId;
    }

    public void setVersionId(Long versionId)
    {
        this.versionId = versionId;
    }

    public String getTextbookName()
    {
        return textbookName;
    }

    public void setTextbookName(String textbookName)
    {
        this.textbookName = textbookName;
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
