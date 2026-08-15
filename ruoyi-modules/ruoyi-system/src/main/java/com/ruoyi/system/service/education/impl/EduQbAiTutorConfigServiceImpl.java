package com.ruoyi.system.service.education.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.config.EduQbAiTutorProperties;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.domain.education.EduQbAiTutorAdminConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.education.IEduQbAiTutorConfigService;
import com.ruoyi.system.service.ISysConfigService;

@Service
public class EduQbAiTutorConfigServiceImpl implements IEduQbAiTutorConfigService
{
    private static final String KEY_ENABLED = "edu.qb.ai.enabled";
    private static final String KEY_BASE_URL = "edu.qb.ai.base-url";
    private static final String KEY_API_KEY = "edu.qb.ai.api-key";
    private static final String KEY_MODEL = "edu.qb.ai.model";
    private static final String KEY_TEMPERATURE = "edu.qb.ai.temperature";

    private static final String MASK_PLACEHOLDER = "********";

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private EduQbAiTutorProperties defaultProperties;

    @Override
    public EduQbAiTutorProperties resolveRuntimeConfig()
    {
        EduQbAiTutorProperties runtime = new EduQbAiTutorProperties();
        runtime.setEnabled(parseBoolean(configService.selectConfigByKey(KEY_ENABLED), defaultProperties.isEnabled()));
        runtime.setBaseUrl(firstNonBlank(configService.selectConfigByKey(KEY_BASE_URL), defaultProperties.getBaseUrl()));
        runtime.setApiKey(firstNonBlank(configService.selectConfigByKey(KEY_API_KEY), defaultProperties.getApiKey()));
        runtime.setModel(firstNonBlank(configService.selectConfigByKey(KEY_MODEL), defaultProperties.getModel()));
        runtime.setTemperature(parseTemperature(configService.selectConfigByKey(KEY_TEMPERATURE),
                defaultProperties.getTemperature()));
        runtime.setConnectTimeoutMs(defaultProperties.getConnectTimeoutMs());
        runtime.setReadTimeoutMs(defaultProperties.getReadTimeoutMs());
        return runtime;
    }

    @Override
    public EduQbAiTutorAdminConfig loadAdminConfig()
    {
        EduQbAiTutorAdminConfig config = new EduQbAiTutorAdminConfig();
        config.setEnabled(parseBoolean(configService.selectConfigByKey(KEY_ENABLED), defaultProperties.isEnabled()));
        config.setBaseUrl(firstNonBlank(configService.selectConfigByKey(KEY_BASE_URL), defaultProperties.getBaseUrl()));
        config.setModel(firstNonBlank(configService.selectConfigByKey(KEY_MODEL), defaultProperties.getModel()));
        config.setTemperature(parseTemperature(configService.selectConfigByKey(KEY_TEMPERATURE),
                defaultProperties.getTemperature()));

        String apiKey = StringUtils.trim(configService.selectConfigByKey(KEY_API_KEY));
        if (StringUtils.isEmpty(apiKey))
        {
            apiKey = StringUtils.trim(defaultProperties.getApiKey());
        }
        config.setApiKeyConfigured(StringUtils.isNotEmpty(apiKey));
        config.setApiKeyMasked(maskApiKey(apiKey));
        config.setApiKey("");
        return config;
    }

    @Override
    public void saveAdminConfig(EduQbAiTutorAdminConfig config)
    {
        if (config == null)
        {
            throw new ServiceException("\u914d\u7f6e\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (StringUtils.isEmpty(config.getBaseUrl()))
        {
            throw new ServiceException("\u8bf7\u586b\u5199 API \u5730\u5740");
        }
        if (StringUtils.isEmpty(config.getModel()))
        {
            throw new ServiceException("\u8bf7\u586b\u5199\u6a21\u578b\u540d\u79f0");
        }

        String username = SecurityUtils.getUsername();
        updateConfigValue(KEY_ENABLED, String.valueOf(Boolean.TRUE.equals(config.getEnabled())), username);
        updateConfigValue(KEY_BASE_URL, StringUtils.trim(config.getBaseUrl()), username);
        updateConfigValue(KEY_MODEL, StringUtils.trim(config.getModel()), username);
        updateConfigValue(KEY_TEMPERATURE, String.valueOf(normalizeTemperature(config.getTemperature())), username);

        String incomingKey = StringUtils.trim(config.getApiKey());
        if (StringUtils.isNotEmpty(incomingKey) && !MASK_PLACEHOLDER.equals(incomingKey))
        {
            updateConfigValue(KEY_API_KEY, incomingKey, username);
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
        return "true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim());
    }

    private static double parseTemperature(String raw, double fallback)
    {
        if (StringUtils.isEmpty(raw))
        {
            return fallback;
        }
        try
        {
            return normalizeTemperature(Double.parseDouble(raw.trim()));
        }
        catch (NumberFormatException ex)
        {
            return fallback;
        }
    }

    private static double normalizeTemperature(Double raw)
    {
        if (raw == null)
        {
            return 0.7D;
        }
        double value = raw;
        if (value < 0D)
        {
            return 0D;
        }
        if (value > 1D)
        {
            return 1D;
        }
        return value;
    }

    private static String firstNonBlank(String primary, String fallback)
    {
        if (StringUtils.isNotEmpty(primary))
        {
            return primary.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private static String maskApiKey(String apiKey)
    {
        if (StringUtils.isEmpty(apiKey))
        {
            return "";
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 8)
        {
            return MASK_PLACEHOLDER;
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }
}
