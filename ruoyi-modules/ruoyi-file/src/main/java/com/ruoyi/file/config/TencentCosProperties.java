package com.ruoyi.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cos")
public class TencentCosProperties
{
    /** SecretId */
    private String secretId;

    /** SecretKey */
    private String secretKey;

    /** Region, e.g. ap-guangzhou */
    private String region;

    /** Bucket name, e.g. bucket-1250000000 */
    private String bucketName;

    /** Optional CDN/custom domain without trailing slash */
    private String domain;

    /** Object key prefix, e.g. library */
    private String prefix = "library";

    public String getSecretId() { return secretId; }
    public void setSecretId(String secretId) { this.secretId = secretId; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String buildObjectUrl(String key)
    {
        String normalizedKey = key.startsWith("/") ? key.substring(1) : key;
        if (domain != null && !domain.trim().isEmpty())
        {
            return domain.replaceAll("/$", "") + "/" + normalizedKey;
        }
        return "https://" + bucketName + ".cos." + region + ".myqcloud.com/" + normalizedKey;
    }
}
