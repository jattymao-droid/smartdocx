package com.ruoyi.system.domain.education;

import java.util.Date;

public class EduPayEntitlement
{
    private Long entitlementId;
    private String username;
    private String bizType;
    private Long bizId;
    private String bizRef;
    private String orderNo;
    private Date createTime;

    public Long getEntitlementId() { return entitlementId; }
    public void setEntitlementId(Long entitlementId) { this.entitlementId = entitlementId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getBizRef() { return bizRef; }
    public void setBizRef(String bizRef) { this.bizRef = bizRef; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
