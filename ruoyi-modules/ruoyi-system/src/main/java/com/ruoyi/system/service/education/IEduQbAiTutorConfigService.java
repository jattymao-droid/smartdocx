package com.ruoyi.system.service.education;

import com.ruoyi.system.config.EduQbAiTutorProperties;
import com.ruoyi.system.domain.education.EduQbAiTutorAdminConfig;

public interface IEduQbAiTutorConfigService
{
    EduQbAiTutorProperties resolveRuntimeConfig();

    EduQbAiTutorAdminConfig loadAdminConfig();

    void saveAdminConfig(EduQbAiTutorAdminConfig config);
}
