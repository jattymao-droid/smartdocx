package com.ruoyi.system.service.education.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbComposeTemplate;
import com.ruoyi.system.mapper.education.EduQbComposeTemplateMapper;
import com.ruoyi.system.service.education.IEduQbComposeTemplateService;

@Service
public class EduQbComposeTemplateServiceImpl implements IEduQbComposeTemplateService
{
    @Autowired
    private EduQbComposeTemplateMapper composeTemplateMapper;

    @Override
    public List<EduQbComposeTemplate> selectAvailableTemplates(Long subjectId, String username)
    {
        ensureSystemTemplates();
        EduQbComposeTemplate query = new EduQbComposeTemplate();
        query.setSubjectId(subjectId);
        query.setCreateBy(username);
        return composeTemplateMapper.selectComposeTemplateList(query);
    }

    @Override
    public EduQbComposeTemplate selectTemplateById(Long templateId)
    {
        ensureSystemTemplates();
        return composeTemplateMapper.selectComposeTemplateById(templateId);
    }

    @Override
    public Long saveUserTemplate(EduQbComposeTemplate template, String username)
    {
        if (template == null)
        {
            throw new ServiceException("\u8bf7\u63d0\u4f9b\u6a21\u677f\u5185\u5bb9");
        }
        if (StringUtils.isEmpty(template.getTemplateName()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u6a21\u677f\u540d\u79f0");
        }
        if (StringUtils.isEmpty(template.getTypeRules()))
        {
            throw new ServiceException("\u8bf7\u8bbe\u7f6e\u9898\u578b\u914d\u7f6e");
        }
        template.setScope(EduQbComposeTemplate.SCOPE_USER);
        template.setStatus("0");
        template.setCreateBy(username);
        if (template.getTemplateId() != null)
        {
            EduQbComposeTemplate existing = composeTemplateMapper.selectComposeTemplateById(template.getTemplateId());
            if (existing == null || !EduQbComposeTemplate.SCOPE_USER.equals(existing.getScope())
                    || !username.equals(existing.getCreateBy()))
            {
                throw new ServiceException("\u4ec5\u53ef\u7f16\u8f91\u81ea\u5df1\u4fdd\u5b58\u7684\u6a21\u677f");
            }
            composeTemplateMapper.updateComposeTemplate(template);
            return template.getTemplateId();
        }
        if (StringUtils.isEmpty(template.getTemplateCode()))
        {
            template.setTemplateCode("custom_" + System.currentTimeMillis());
        }
        composeTemplateMapper.insertComposeTemplate(template);
        return template.getTemplateId();
    }

    @Override
    public int deleteUserTemplate(Long templateId, String username)
    {
        return composeTemplateMapper.deleteComposeTemplateById(templateId, username);
    }

    private void ensureSystemTemplates()
    {
        if (composeTemplateMapper.countSystemTemplates() > 0)
        {
            return;
        }
        insertSystem("unit", "\u5355\u5143\u6d4b\u9a8c", "\u5355\u5143\u6d4b\u9a8c\u5377",
                "0.30", "0.70", 20, 60, 20,
                "[{\"questionType\":\"single\",\"count\":5,\"scorePerQuestion\":3},"
                        + "{\"questionType\":\"multi\",\"count\":2,\"scorePerQuestion\":4},"
                        + "{\"questionType\":\"fill\",\"count\":3,\"scorePerQuestion\":4}]");
        insertSystem("midterm", "\u671f\u4e2d\u6d4b\u8bd5", "\u671f\u4e2d\u6d4b\u8bd5\u5377",
                "0.20", "0.80", 25, 50, 25,
                "[{\"questionType\":\"single\",\"count\":8,\"scorePerQuestion\":3},"
                        + "{\"questionType\":\"multi\",\"count\":4,\"scorePerQuestion\":4},"
                        + "{\"questionType\":\"fill\",\"count\":4,\"scorePerQuestion\":4},"
                        + "{\"questionType\":\"short\",\"count\":2,\"scorePerQuestion\":8}]");
        insertSystem("final", "\u671f\u672b\u6d4b\u8bd5", "\u671f\u672b\u6d4b\u8bd5\u5377",
                "0.20", "0.90", 20, 45, 35,
                "[{\"questionType\":\"single\",\"count\":10,\"scorePerQuestion\":3},"
                        + "{\"questionType\":\"multi\",\"count\":5,\"scorePerQuestion\":4},"
                        + "{\"questionType\":\"fill\",\"count\":5,\"scorePerQuestion\":4},"
                        + "{\"questionType\":\"short\",\"count\":3,\"scorePerQuestion\":10},"
                        + "{\"questionType\":\"comprehensive\",\"count\":1,\"scorePerQuestion\":12}]");
    }

    private void insertSystem(String code, String name, String title, String diffMin, String diffMax,
            int easy, int medium, int hard, String typeRules)
    {
        EduQbComposeTemplate row = new EduQbComposeTemplate();
        row.setTemplateCode(code);
        row.setTemplateName(name);
        row.setPaperTitle(title);
        row.setScope(EduQbComposeTemplate.SCOPE_SYSTEM);
        row.setDifficultyMin(new BigDecimal(diffMin));
        row.setDifficultyMax(new BigDecimal(diffMax));
        row.setEasyPercent(easy);
        row.setMediumPercent(medium);
        row.setHardPercent(hard);
        row.setTypeRules(typeRules);
        row.setStatus("0");
        row.setCreateBy("system");
        composeTemplateMapper.insertComposeTemplate(row);
    }
}
