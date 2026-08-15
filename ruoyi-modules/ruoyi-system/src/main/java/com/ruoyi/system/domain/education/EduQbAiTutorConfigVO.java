package com.ruoyi.system.domain.education;

public class EduQbAiTutorConfigVO
{
    private Boolean enabled;
    private Boolean aiPowered;
    private String model;

    public Boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled(Boolean enabled)
    {
        this.enabled = enabled;
    }

    public Boolean getAiPowered()
    {
        return aiPowered;
    }

    public void setAiPowered(Boolean aiPowered)
    {
        this.aiPowered = aiPowered;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }
}
