package com.ruoyi.system.domain.education;

public class EduLibraryAuditBody
{
    private Long[] documentIds;
    private String auditStatus;
    private String auditRemark;

    public Long[] getDocumentIds() { return documentIds; }
    public void setDocumentIds(Long[] documentIds) { this.documentIds = documentIds; }
    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(String auditStatus) { this.auditStatus = auditStatus; }
    public String getAuditRemark() { return auditRemark; }
    public void setAuditRemark(String auditRemark) { this.auditRemark = auditRemark; }
}
