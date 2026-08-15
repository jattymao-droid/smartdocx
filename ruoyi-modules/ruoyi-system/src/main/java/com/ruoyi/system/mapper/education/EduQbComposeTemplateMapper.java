package com.ruoyi.system.mapper.education;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduQbComposeTemplate;

public interface EduQbComposeTemplateMapper
{
    List<EduQbComposeTemplate> selectComposeTemplateList(EduQbComposeTemplate query);

    EduQbComposeTemplate selectComposeTemplateById(@Param("templateId") Long templateId);

    int insertComposeTemplate(EduQbComposeTemplate row);

    int updateComposeTemplate(EduQbComposeTemplate row);

    int deleteComposeTemplateById(@Param("templateId") Long templateId,
            @Param("createBy") String createBy);

    int countSystemTemplates();
}
