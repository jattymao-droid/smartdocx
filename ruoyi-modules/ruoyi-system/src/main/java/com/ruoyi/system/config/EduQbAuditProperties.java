package com.ruoyi.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Question bank audit workflow configuration.
 */
@Component
@ConfigurationProperties(prefix = "edu.qb.audit")
public class EduQbAuditProperties
{
    /** When true, teacher submissions enter pending review instead of auto-approved. */
    private boolean enabled = true;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
