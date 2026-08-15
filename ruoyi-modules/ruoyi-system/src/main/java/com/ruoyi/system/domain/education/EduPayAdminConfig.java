package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduPayAdminConfig
{
    private Boolean enabled;
    private String pid;
    private String gatewayUrl;
    private String notifyUrl;
    private boolean keyConfigured;
    private String keyMasked;
    private String key;
    private BigDecimal paperExportFee;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getPid() { return pid; }
    public void setPid(String pid) { this.pid = pid; }
    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }
    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    public boolean isKeyConfigured() { return keyConfigured; }
    public void setKeyConfigured(boolean keyConfigured) { this.keyConfigured = keyConfigured; }
    public String getKeyMasked() { return keyMasked; }
    public void setKeyMasked(String keyMasked) { this.keyMasked = keyMasked; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public BigDecimal getPaperExportFee() { return paperExportFee; }
    public void setPaperExportFee(BigDecimal paperExportFee) { this.paperExportFee = paperExportFee; }
}
