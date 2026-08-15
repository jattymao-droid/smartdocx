package com.ruoyi.system.service.education.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.config.EduLibraryProperties;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.domain.education.EduLibraryAdminConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.education.IEduLibraryAdminConfigService;

@Service
public class EduLibraryAdminConfigServiceImpl implements IEduLibraryAdminConfigService
{
    private static final String KEY_PREVIEW_MAX_PAGES = "edu.library.preview.max-pages";
    private static final int MIN_PREVIEW_PAGES = 1;
    private static final int MAX_PREVIEW_PAGES = 100;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private EduLibraryProperties defaultProperties;

    @Override
    public int resolvePreviewMaxPages()
    {
        return parsePreviewMaxPages(configService.selectConfigByKey(KEY_PREVIEW_MAX_PAGES),
                defaultProperties.getPreview().getMaxPreviewPages());
    }

    @Override
    public EduLibraryAdminConfig loadAdminConfig()
    {
        EduLibraryAdminConfig config = new EduLibraryAdminConfig();
        config.setPreviewMaxPages(resolvePreviewMaxPages());
        return config;
    }

    @Override
    public void saveAdminConfig(EduLibraryAdminConfig config)
    {
        if (config == null || config.getPreviewMaxPages() == null)
        {
            throw new ServiceException("\u8bf7\u8bbe\u7f6e\u9884\u89c8\u9875\u6570");
        }
        int pages = normalizePreviewMaxPages(config.getPreviewMaxPages());
        String username = SecurityUtils.getUsername();
        updateConfigValue(KEY_PREVIEW_MAX_PAGES, String.valueOf(pages), username);
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

    private static int parsePreviewMaxPages(String raw, int fallback)
    {
        if (StringUtils.isEmpty(raw))
        {
            return normalizePreviewMaxPages(fallback);
        }
        try
        {
            return normalizePreviewMaxPages(Integer.parseInt(raw.trim()));
        }
        catch (NumberFormatException ex)
        {
            return normalizePreviewMaxPages(fallback);
        }
    }

    private static int normalizePreviewMaxPages(int value)
    {
        if (value < MIN_PREVIEW_PAGES)
        {
            return MIN_PREVIEW_PAGES;
        }
        if (value > MAX_PREVIEW_PAGES)
        {
            return MAX_PREVIEW_PAGES;
        }
        return value;
    }
}
