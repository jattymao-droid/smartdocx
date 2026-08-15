package com.ruoyi.system.domain.education;

public class EduQbTextbookVersion
{
    private Long versionId;
    private Long subjectId;
    /** ??��????? / ???? */
    private String schoolStage;
    private String versionName;
    private Integer orderNum;
    private String status;

    public Long getVersionId()
    {
        return versionId;
    }

    public void setVersionId(Long versionId)
    {
        this.versionId = versionId;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getSchoolStage()
    {
        return schoolStage;
    }

    public void setSchoolStage(String schoolStage)
    {
        this.schoolStage = schoolStage;
    }

    public String getVersionName()
    {
        return versionName;
    }

    public void setVersionName(String versionName)
    {
        this.versionName = versionName;
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
