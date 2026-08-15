package com.ruoyi.system.domain.education;

import java.io.Serializable;

public class EduQbAiTutorAdminConfig implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Boolean enabled;
    private String baseUrl;
    private String apiKey;
    private Boolean apiKeyConfigured;
    private String apiKeyMasked;
    private String model;
    private Double temperature;

    public Boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled(Boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public Boolean getApiKeyConfigured()
    {
        return apiKeyConfigured;
    }

    public void setApiKeyConfigured(Boolean apiKeyConfigured)
    {
        this.apiKeyConfigured = apiKeyConfigured;
    }

    public String getApiKeyMasked()
    {
        return apiKeyMasked;
    }

    public void setApiKeyMasked(String apiKeyMasked)
    {
        this.apiKeyMasked = apiKeyMasked;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public Double getTemperature()
    {
        return temperature;
    }

    public void setTemperature(Double temperature)
    {
        this.temperature = temperature;
    }
}
