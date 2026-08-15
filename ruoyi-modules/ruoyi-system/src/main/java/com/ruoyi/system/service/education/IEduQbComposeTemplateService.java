package com.ruoyi.system.service.education;

import java.util.List;
import com.ruoyi.system.domain.education.EduQbComposeTemplate;

public interface IEduQbComposeTemplateService
{
    List<EduQbComposeTemplate> selectAvailableTemplates(Long subjectId, String username);

    EduQbComposeTemplate selectTemplateById(Long templateId);

    Long saveUserTemplate(EduQbComposeTemplate template, String username);

    int deleteUserTemplate(Long templateId, String username);
}
