package com.ruoyi.system.config;

import java.io.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ruoyi.common.core.constant.Constants;

/**
 * Serve question-bank uploaded files under /profile/** from file.path.
 */
@Configuration
public class EduQbResourcesConfig implements WebMvcConfigurer
{
    @Value("${file.path:D:/ruoyi/uploadPath}")
    private String localFilePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
                .addResourceLocations("file:" + localFilePath + File.separator);
    }
}
