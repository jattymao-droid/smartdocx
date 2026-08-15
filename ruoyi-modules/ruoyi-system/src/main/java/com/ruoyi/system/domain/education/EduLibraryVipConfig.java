package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduLibraryVipConfig
{
    private boolean enabled;
    private BigDecimal price;
    private Integer durationDays;
    private boolean freeDownload;
    private Integer previewPages;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public boolean isFreeDownload() { return freeDownload; }
    public void setFreeDownload(boolean freeDownload) { this.freeDownload = freeDownload; }
    public Integer getPreviewPages() { return previewPages; }
    public void setPreviewPages(Integer previewPages) { this.previewPages = previewPages; }
}
