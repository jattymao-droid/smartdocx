package com.ruoyi.system.service.education.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import com.ruoyi.common.core.utils.StringUtils;

public final class ZPaySignUtils
{
    private ZPaySignUtils()
    {
    }

    public static String sign(Map<String, String> params, String merchantKey)
    {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("sign".equals(key) || "sign_type".equals(key))
            {
                continue;
            }
            if (StringUtils.isEmpty(value))
            {
                continue;
            }
            sorted.put(key, value);
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet())
        {
            if (!first)
            {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        sb.append(merchantKey);
        return md5(sb.toString());
    }

    public static boolean verify(Map<String, String> params, String merchantKey, String sign)
    {
        if (StringUtils.isEmpty(sign))
        {
            return false;
        }
        return sign.equalsIgnoreCase(sign(params, merchantKey));
    }

    private static String md5(String raw)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes)
            {
                String part = Integer.toHexString(b & 0xff);
                if (part.length() == 1)
                {
                    hex.append('0');
                }
                hex.append(part);
            }
            return hex.toString();
        }
        catch (Exception ex)
        {
            throw new RuntimeException("MD5 sign failed", ex);
        }
    }
}
