package com.ruoyi.system.service.education.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.config.EduQbOcrProperties;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.system.service.education.support.EduQbHttpUtils;
import com.ruoyi.system.domain.education.EduQbOcrLine;

@Component("baiduOcrProvider")
public class BaiduOcrProvider implements OcrProvider
{
    private static final Logger log = LoggerFactory.getLogger(BaiduOcrProvider.class);

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String OCR_GENERAL_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";
    private static final String OCR_ACCURATE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic";

    @Autowired
    private EduQbOcrProperties ocrProperties;

    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    @Override
    public List<EduQbOcrLine> recognize(byte[] imageBytes)
    {
        if (!ocrProperties.getBaidu().isConfigured())
        {
            throw new ServiceException("\u767e\u5ea6 OCR \u672a\u914d\u7f6e API Key");
        }
        String token = getAccessToken();
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        String param;
        try
        {
            param = "image=" + URLEncoder.encode(imageBase64, StandardCharsets.UTF_8.name())
                    + "&detect_direction=true&probability=true";
        }
        catch (Exception ex)
        {
            throw new ServiceException("\u56fe\u7247\u7f16\u7801\u5931\u8d25");
        }
        String ocrUrl = ocrProperties.getBaidu().isAccurateMode() ? OCR_ACCURATE_URL : OCR_GENERAL_URL;
        String url = ocrUrl + "?access_token=" + token;
        String response = EduQbHttpUtils.sendPost(url, param);
        return parseResponse(response);
    }

    private List<EduQbOcrLine> parseResponse(String response)
    {
        JSONObject root = JSON.parseObject(response);
        if (root == null)
        {
            throw new ServiceException("OCR \u54cd\u5e94\u89e3\u6790\u5931\u8d25");
        }
        if (root.containsKey("error_code"))
        {
            throw new ServiceException("OCR \u8c03\u7528\u5931\u8d25\uff1a" + root.getString("error_msg"));
        }
        JSONArray wordsResult = root.getJSONArray("words_result");
        List<EduQbOcrLine> lines = new ArrayList<>();
        if (wordsResult == null)
        {
            return lines;
        }
        for (int i = 0; i < wordsResult.size(); i++)
        {
            JSONObject item = wordsResult.getJSONObject(i);
            if (item == null)
            {
                continue;
            }
            String words = item.getString("words");
            if (words == null || words.isBlank())
            {
                continue;
            }
            BigDecimal confidence = new BigDecimal("0.9000");
            JSONObject probability = item.getJSONObject("probability");
            if (probability != null && probability.getDouble("average") != null)
            {
                confidence = BigDecimal.valueOf(probability.getDouble("average")).setScale(4, RoundingMode.HALF_UP);
            }
            lines.add(new EduQbOcrLine(words.trim(), confidence));
        }
        return lines;
    }

    private synchronized String getAccessToken()
    {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireAt)
        {
            return cachedToken;
        }
        String apiKey = ocrProperties.getBaidu().getApiKey();
        String secretKey = ocrProperties.getBaidu().getSecretKey();
        String param = "grant_type=client_credentials&client_id=" + apiKey + "&client_secret=" + secretKey;
        String response = EduQbHttpUtils.sendPost(TOKEN_URL, param);
        JSONObject root = JSON.parseObject(response);
        if (root == null || root.getString("access_token") == null)
        {
            log.error("Baidu OCR token failed: {}", response);
            throw new ServiceException("\u83b7\u53d6 OCR Token \u5931\u8d25");
        }
        cachedToken = root.getString("access_token");
        int expiresIn = root.getIntValue("expires_in", 2592000);
        tokenExpireAt = System.currentTimeMillis() + (expiresIn - 600L) * 1000L;
        return cachedToken;
    }
}
