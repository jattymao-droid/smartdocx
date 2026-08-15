package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduPayCheckResult
{
    private boolean enabled;
    private boolean needPay;
    private boolean purchased;
    private BigDecimal price;
    private String productName;
    private String bizType;
    private Long bizId;
    private String bizRef;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isNeedPay() { return needPay; }
    public void setNeedPay(boolean needPay) { this.needPay = needPay; }
    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getBizRef() { return bizRef; }
    public void setBizRef(String bizRef) { this.bizRef = bizRef; }
}
