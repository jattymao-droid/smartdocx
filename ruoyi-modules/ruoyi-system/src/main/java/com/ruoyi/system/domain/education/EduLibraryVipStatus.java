package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

public class EduLibraryVipStatus
{
    private boolean enabled;
    private boolean active;
    private String planCode;
    private String defaultPlanCode;
    private List<EduLibraryVipPlan> plans;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;
    private Integer remainDays;
    private BigDecimal price;
    private Integer durationDays;
    private boolean freeDownload;
    private Integer previewPages;
    private boolean payEnabled;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getDefaultPlanCode() { return defaultPlanCode; }
    public void setDefaultPlanCode(String defaultPlanCode) { this.defaultPlanCode = defaultPlanCode; }
    public List<EduLibraryVipPlan> getPlans() { return plans; }
    public void setPlans(List<EduLibraryVipPlan> plans) { this.plans = plans; }
    public Date getExpireTime() { return expireTime; }
    public void setExpireTime(Date expireTime) { this.expireTime = expireTime; }
    public Integer getRemainDays() { return remainDays; }
    public void setRemainDays(Integer remainDays) { this.remainDays = remainDays; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public boolean isFreeDownload() { return freeDownload; }
    public void setFreeDownload(boolean freeDownload) { this.freeDownload = freeDownload; }
    public Integer getPreviewPages() { return previewPages; }
    public void setPreviewPages(Integer previewPages) { this.previewPages = previewPages; }
    public boolean isPayEnabled() { return payEnabled; }
    public void setPayEnabled(boolean payEnabled) { this.payEnabled = payEnabled; }
}
