package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduQbComposeTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String SCOPE_SYSTEM = "system";
    public static final String SCOPE_USER = "user";

    private Long templateId;
    private String templateName;
    private String templateCode;
    private String scope;
    private Long subjectId;
    private String paperTitle;
    private BigDecimal difficultyMin;
    private BigDecimal difficultyMax;
    private Integer easyPercent;
    private Integer mediumPercent;
    private Integer hardPercent;
    /** JSON array of type rules */
    private String typeRules;
    private String status;

    public Long getTemplateId()
    {
        return templateId;
    }

    public void setTemplateId(Long templateId)
    {
        this.templateId = templateId;
    }

    public String getTemplateName()
    {
        return templateName;
    }

    public void setTemplateName(String templateName)
    {
        this.templateName = templateName;
    }

    public String getTemplateCode()
    {
        return templateCode;
    }

    public void setTemplateCode(String templateCode)
    {
        this.templateCode = templateCode;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getPaperTitle()
    {
        return paperTitle;
    }

    public void setPaperTitle(String paperTitle)
    {
        this.paperTitle = paperTitle;
    }

    public BigDecimal getDifficultyMin()
    {
        return difficultyMin;
    }

    public void setDifficultyMin(BigDecimal difficultyMin)
    {
        this.difficultyMin = difficultyMin;
    }

    public BigDecimal getDifficultyMax()
    {
        return difficultyMax;
    }

    public void setDifficultyMax(BigDecimal difficultyMax)
    {
        this.difficultyMax = difficultyMax;
    }

    public Integer getEasyPercent()
    {
        return easyPercent;
    }

    public void setEasyPercent(Integer easyPercent)
    {
        this.easyPercent = easyPercent;
    }

    public Integer getMediumPercent()
    {
        return mediumPercent;
    }

    public void setMediumPercent(Integer mediumPercent)
    {
        this.mediumPercent = mediumPercent;
    }

    public Integer getHardPercent()
    {
        return hardPercent;
    }

    public void setHardPercent(Integer hardPercent)
    {
        this.hardPercent = hardPercent;
    }

    public String getTypeRules()
    {
        return typeRules;
    }

    public void setTypeRules(String typeRules)
    {
        this.typeRules = typeRules;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
