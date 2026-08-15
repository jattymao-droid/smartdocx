package com.ruoyi.system.service.education;

import com.ruoyi.system.config.EduPayProperties;
import com.ruoyi.system.domain.education.EduPayAdminConfig;

public interface IEduPayConfigService
{
    EduPayProperties resolveRuntimeConfig();

    EduPayAdminConfig loadAdminConfig();

    void saveAdminConfig(EduPayAdminConfig config);
}
