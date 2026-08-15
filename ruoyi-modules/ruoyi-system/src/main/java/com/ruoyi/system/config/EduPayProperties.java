package com.ruoyi.system.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edu.pay")
public class EduPayProperties
{
    private ZPay zpay = new ZPay();
    private BigDecimal paperExportFee = BigDecimal.ZERO;

    public ZPay getZpay()
    {
        return zpay;
    }

    public void setZpay(ZPay zpay)
    {
        this.zpay = zpay;
    }

    public BigDecimal getPaperExportFee()
    {
        return paperExportFee;
    }

    public void setPaperExportFee(BigDecimal paperExportFee)
    {
        this.paperExportFee = paperExportFee;
    }

    public static class ZPay
    {
        private boolean enabled;
        private String pid;
        private String key;
        private String gatewayUrl = "https://zpayz.cn";
        private String notifyUrl;

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        public String getPid()
        {
            return pid;
        }

        public void setPid(String pid)
        {
            this.pid = pid;
        }

        public String getKey()
        {
            return key;
        }

        public void setKey(String key)
        {
            this.key = key;
        }

        public String getGatewayUrl()
        {
            return gatewayUrl;
        }

        public void setGatewayUrl(String gatewayUrl)
        {
            this.gatewayUrl = gatewayUrl;
        }

        public String getNotifyUrl()
        {
            return notifyUrl;
        }

        public void setNotifyUrl(String notifyUrl)
        {
            this.notifyUrl = notifyUrl;
        }
    }
}
