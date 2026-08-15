package com.ruoyi.system.domain.education;

public class EduLibraryStatusBody
{
    private Long[] documentIds;
    /** 0=online 1=shelved */
    private String status;

    public Long[] getDocumentIds()
    {
        return documentIds;
    }

    public void setDocumentIds(Long[] documentIds)
    {
        this.documentIds = documentIds;
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
