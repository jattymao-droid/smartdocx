package com.ruoyi.system.service.education.support;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal HTTP helper for OCR providers.
 */
public final class EduQbHttpUtils
{
    private EduQbHttpUtils()
    {
    }

    public static String sendPost(String url, String param)
    {
        StringBuilder result = new StringBuilder();
        HttpURLConnection conn = null;
        try
        {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(120000);
            try (OutputStream os = conn.getOutputStream())
            {
                os.write(param.getBytes(StandardCharsets.UTF_8));
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    result.append(line);
                }
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException("HTTP POST failed: " + url, ex);
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }
        return result.toString();
    }
}
