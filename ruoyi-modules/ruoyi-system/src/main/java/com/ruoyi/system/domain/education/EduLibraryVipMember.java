package com.ruoyi.system.domain.education;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduLibraryVipMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String STATUS_ACTIVE = "0";
    public static final String STATUS_EXPIRED = "1";
    public static final String STATUS_DISABLED = "2";

    public static final String SOURCE_PAY = "pay";
    public static final String SOURCE_ADMIN = "admin";

    private Long vipId;
    private String username;
    private String planCode;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;
    private String source;
    private String orderNo;

    /** query: keyword for username */
    private String keyword;

    public Long getVipId() { return vipId; }
    public void setVipId(Long vipId) { this.vipId = vipId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getExpireTime() { return expireTime; }
    public void setExpireTime(Date expireTime) { this.expireTime = expireTime; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
