package com.ruoyi.system.service.education.impl;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.config.EduPayProperties;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.domain.education.EduPayAdminConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.education.IEduPayConfigService;

@Service
public class EduPayConfigServiceImpl implements IEduPayConfigService
{
    private static final String KEY_ENABLED = "edu.pay.zpay.enabled";
    private static final String KEY_PID = "edu.pay.zpay.pid";
    private static final String KEY_SECRET = "edu.pay.zpay.key";
    private static final String KEY_GATEWAY = "edu.pay.zpay.gateway-url";
    private static final String KEY_NOTIFY = "edu.pay.zpay.notify-url";
    private static final String KEY_PAPER_FEE = "edu.pay.paper-export-fee";
    private static final String MASK_PLACEHOLDER = "********";

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private EduPayProperties defaultProperties;

    @Override
    public EduPayProperties resolveRuntimeConfig()
    {
        EduPayProperties runtime = new EduPayProperties();
        EduPayProperties.ZPay zpay = new EduPayProperties.ZPay();
        zpay.setEnabled(parseBoolean(configService.selectConfigByKey(KEY_ENABLED), defaultProperties.getZpay().isEnabled()));
        zpay.setPid(firstNonBlank(configService.selectConfigByKey(KEY_PID), defaultProperties.getZpay().getPid()));
        zpay.setKey(firstNonBlank(configService.selectConfigByKey(KEY_SECRET), defaultProperties.getZpay().getKey()));
        zpay.setGatewayUrl(firstNonBlank(configService.selectConfigByKey(KEY_GATEWAY), defaultProperties.getZpay().getGatewayUrl()));
        zpay.setNotifyUrl(firstNonBlank(configService.selectConfigByKey(KEY_NOTIFY), defaultProperties.getZpay().getNotifyUrl()));
        runtime.setZpay(zpay);
        runtime.setPaperExportFee(parseAmount(configService.selectConfigByKey(KEY_PAPER_FEE), defaultProperties.getPaperExportFee()));
        return runtime;
    }

    @Override
    public EduPayAdminConfig loadAdminConfig()
    {
        EduPayAdminConfig config = new EduPayAdminConfig();
        config.setEnabled(parseBoolean(configService.selectConfigByKey(KEY_ENABLED), defaultProperties.getZpay().isEnabled()));
        config.setPid(firstNonBlank(configService.selectConfigByKey(KEY_PID), defaultProperties.getZpay().getPid()));
        config.setGatewayUrl(firstNonBlank(configService.selectConfigByKey(KEY_GATEWAY), defaultProperties.getZpay().getGatewayUrl()));
        config.setNotifyUrl(firstNonBlank(configService.selectConfigByKey(KEY_NOTIFY), defaultProperties.getZpay().getNotifyUrl()));
        config.setPaperExportFee(parseAmount(configService.selectConfigByKey(KEY_PAPER_FEE), defaultProperties.getPaperExportFee()));

        String secret = StringUtils.trim(configService.selectConfigByKey(KEY_SECRET));
        if (StringUtils.isEmpty(secret))
        {
            secret = StringUtils.trim(defaultProperties.getZpay().getKey());
        }
        config.setKeyConfigured(StringUtils.isNotEmpty(secret));
        config.setKeyMasked(maskSecret(secret));
        config.setKey("");
        return config;
    }

    @Override
    public void saveAdminConfig(EduPayAdminConfig config)
    {
        if (config == null)
        {
            throw new ServiceException("\u914d\u7f6e\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String username = SecurityUtils.getUsername();
        updateConfigValue(KEY_ENABLED, String.valueOf(Boolean.TRUE.equals(config.getEnabled())), username);
        updateConfigValue(KEY_PID, StringUtils.trim(config.getPid()), username);
        updateConfigValue(KEY_GATEWAY, StringUtils.trim(config.getGatewayUrl()), username);
        updateConfigValue(KEY_NOTIFY, StringUtils.trim(config.getNotifyUrl()), username);
        updateConfigValue(KEY_PAPER_FEE, normalizeAmount(config.getPaperExportFee()).toPlainString(), username);

        String incomingKey = StringUtils.trim(config.getKey());
        if (StringUtils.isNotEmpty(incomingKey) && !MASK_PLACEHOLDER.equals(incomingKey))
        {
            updateConfigValue(KEY_SECRET, incomingKey, username);
        }
    }

    private void updateConfigValue(String configKey, String configValue, String username)
    {
        SysConfig query = new SysConfig();
        query.setConfigKey(configKey);
        SysConfig existing = configMapper.selectConfig(query);
        if (existing == null)
        {
            throw new ServiceException("\u7f3a\u5c11\u53c2\u6570\u914d\u7f6e: " + configKey);
        }
        SysConfig update = new SysConfig();
        update.setConfigId(existing.getConfigId());
        update.setConfigKey(configKey);
        update.setConfigValue(StringUtils.nvl(configValue, ""));
        update.setUpdateBy(username);
        configService.updateConfig(update);
    }

    private static boolean parseBoolean(String raw, boolean fallback)
    {
        if (StringUtils.isEmpty(raw))
        {
            return fallback;
        }
        return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "Y".equalsIgnoreCase(raw);
    }

    private static String firstNonBlank(String primary, String fallback)
    {
        if (StringUtils.isNotEmpty(primary))
        {
            return StringUtils.trim(primary);
        }
        return StringUtils.trim(fallback);
    }

    private static BigDecimal parseAmount(String raw, BigDecimal fallback)
    {
        if (StringUtils.isEmpty(raw))
        {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
        try
        {
            return new BigDecimal(raw.trim());
        }
        catch (Exception ex)
        {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
    }

    private static BigDecimal normalizeAmount(BigDecimal amount)
    {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
        {
            return BigDecimal.ZERO;
        }
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private static String maskSecret(String secret)
    {
        if (StringUtils.isEmpty(secret))
        {
            return "";
        }
        if (secret.length() <= 4)
        {
            return MASK_PLACEHOLDER;
        }
        return secret.substring(0, 2) + MASK_PLACEHOLDER + secret.substring(secret.length() - 2);
    }
}
