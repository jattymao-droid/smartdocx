package com.ruoyi.system.service.education;

import com.ruoyi.system.domain.education.EduLibraryAdminConfig;

public interface IEduLibraryAdminConfigService
{
    int resolvePreviewMaxPages();

    EduLibraryAdminConfig loadAdminConfig();

    void saveAdminConfig(EduLibraryAdminConfig config);
}
