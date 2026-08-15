package com.ruoyi.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edu.qb.ai")
public class EduQbAiTutorProperties
{
    private boolean enabled = true;

    /** OpenAI-compatible API base, e.g. https://api.openai.com */
    private String baseUrl = "https://api.openai.com";

    private String apiKey = "";

    private String model = "gpt-4o-mini";

    private int connectTimeoutMs = 10000;

    private int readTimeoutMs = 120000;

    private double temperature = 0.7;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
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

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public int getConnectTimeoutMs()
    {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs)
    {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs()
    {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs)
    {
        this.readTimeoutMs = readTimeoutMs;
    }

    public double getTemperature()
    {
        return temperature;
    }

    public void setTemperature(double temperature)
    {
        this.temperature = temperature;
    }

    public boolean isRemoteConfigured()
    {
        return enabled && apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }

    public String resolveChatUrl()
    {
        String url = baseUrl == null ? "" : baseUrl.trim();
        while (url.endsWith("/"))
        {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/v1"))
        {
            return url + "/chat/completions";
        }
        return url + "/v1/chat/completions";
    }
}
