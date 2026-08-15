package com.ruoyi.system.service.education.support;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.config.EduQbOcrProperties;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.system.domain.education.EduQbOcrLine;

@Component("paddleOcrProvider")
public class PaddleOcrProvider implements OcrProvider
{
    private static final Logger log = LoggerFactory.getLogger(PaddleOcrProvider.class);

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private static final int READ_TIMEOUT_MS = 120000;

    private static final long HEALTH_CACHE_MS = 30000L;

    @Autowired
    private EduQbOcrProperties ocrProperties;

    private volatile Boolean healthCache;
    private volatile long healthCheckedAt;

    @Override
    public List<EduQbOcrLine> recognize(byte[] imageBytes)
    {
        return recognizeDetailed(imageBytes).getLines();
    }

    public OcrSidecarResult recognizeDetailed(byte[] imageBytes)
    {
        EduQbOcrProperties.PaddleConfig cfg = ocrProperties.getPaddle();
        if (!cfg.isEnabled())
        {
            throw new ServiceException("PaddleOCR \u672a\u542f\u7528");
        }
        if (!isReachable())
        {
            throw new ServiceException("PaddleOCR \u670d\u52a1\u4e0d\u53ef\u7528\uff0c\u8bf7\u542f\u52a8 paddleocr-service\uff08\u9ed8\u8ba4 http://127.0.0.1:8867\uff09");
        }
        String ocrUrl = cfg.resolveOcrUrl();
        Map<String, String> body = new HashMap<>();
        body.put("image", Base64.getEncoder().encodeToString(imageBytes));
        body.put("mode", cfg.getMode() != null ? cfg.getMode() : "text");
        String response = postJson(ocrUrl, JSON.toJSONString(body), cfg.getReadTimeoutMs());
        return parseDetailedResponse(response);
    }

    public boolean isReachable()
    {
        EduQbOcrProperties.PaddleConfig cfg = ocrProperties.getPaddle();
        if (!cfg.isEnabled() || cfg.getBaseUrl() == null || cfg.getBaseUrl().isBlank())
        {
            return false;
        }
        long now = System.currentTimeMillis();
        if (healthCache != null && now - healthCheckedAt < HEALTH_CACHE_MS)
        {
            return healthCache;
        }
        boolean ok = pingHealth(cfg.resolveHealthUrl(), cfg.getConnectTimeoutMs());
        healthCache = ok;
        healthCheckedAt = now;
        return ok;
    }

    private boolean pingHealth(String healthUrl, int connectTimeoutMs)
    {
        HttpURLConnection conn = null;
        try
        {
            conn = openConnection(healthUrl, "GET", connectTimeoutMs, 5000);
            int code = conn.getResponseCode();
            if (code != 200)
            {
                return false;
            }
            String body = readStream(conn);
            JSONObject root = JSON.parseObject(body);
            return root != null && "ok".equalsIgnoreCase(root.getString("status"));
        }
        catch (Exception ex)
        {
            log.debug("PaddleOCR health check failed: {}", ex.getMessage());
            return false;
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }
    }

    private OcrSidecarResult parseDetailedResponse(String response)
    {
        JSONObject root = JSON.parseObject(response);
        if (root == null)
        {
            throw new ServiceException("PaddleOCR \u54cd\u5e94\u89e3\u6790\u5931\u8d25");
        }
        if (root.containsKey("detail"))
        {
            throw new ServiceException("PaddleOCR \u8c03\u7528\u5931\u8d25\uff1a" + root.getString("detail"));
        }

        OcrSidecarResult result = new OcrSidecarResult();
        result.setProvider(root.getString("provider") != null ? root.getString("provider") : "paddleocr");
        result.setMode(root.getString("mode") != null ? root.getString("mode") : "text");

        JSONArray warnings = root.getJSONArray("warnings");
        if (warnings != null)
        {
            List<String> warnList = new ArrayList<>();
            for (int i = 0; i < warnings.size(); i++)
            {
                String w = warnings.getString(i);
                if (w != null && !w.isBlank())
                {
                    warnList.add(w);
                }
            }
            result.setWarnings(warnList);
        }

        JSONArray lines = root.getJSONArray("lines");
        List<EduQbOcrLine> ocrLines = new ArrayList<>();
        if (lines != null)
        {
            for (int i = 0; i < lines.size(); i++)
            {
                JSONObject item = lines.getJSONObject(i);
                if (item == null)
                {
                    continue;
                }
                String text = item.getString("text");
                if (text == null || text.isBlank())
                {
                    continue;
                }
                BigDecimal confidence = new BigDecimal("0.8500");
                Double score = item.getDouble("confidence");
                if (score != null)
                {
                    confidence = BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
                }
                ocrLines.add(new EduQbOcrLine(text.trim(), confidence));
            }
        }
        result.setLines(ocrLines);
        return result;
    }

    private String postJson(String url, String jsonBody, int readTimeoutMs)
    {
        HttpURLConnection conn = null;
        try
        {
            conn = openConnection(url, "POST", CONNECT_TIMEOUT_MS, readTimeoutMs);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setDoOutput(true);
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream out = conn.getOutputStream())
            {
                out.write(bytes);
            }
            int code = conn.getResponseCode();
            String body = readStream(conn);
            if (code < 200 || code >= 300)
            {
                log.error("PaddleOCR HTTP {}: {}", code, body);
                throw new ServiceException("PaddleOCR HTTP \u9519\u8bef: " + code);
            }
            return body;
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            log.error("PaddleOCR request failed", ex);
            throw new ServiceException("PaddleOCR \u8bf7\u6c42\u5931\u8d25: " + ex.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }
    }

    private HttpURLConnection openConnection(String url, String method, int connectTimeout, int readTimeout)
            throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Connection", "Keep-Alive");
        return conn;
    }

    private String readStream(HttpURLConnection conn) throws Exception
    {
        int code = conn.getResponseCode();
        java.io.InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null)
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
