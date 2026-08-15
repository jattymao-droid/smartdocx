package com.ruoyi.system.config;

import java.io.File;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Question bank OCR provider configuration.
 */
@Component
@ConfigurationProperties(prefix = "edu.qb.ocr")
public class EduQbOcrProperties
{
    /** auto | stub | baidu | paddle | tesseract */
    private String provider = "auto";

    private final Baidu baidu = new Baidu();

    private final PaddleConfig paddle = new PaddleConfig();

    private final TesseractConfig tesseract = new TesseractConfig();

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public Baidu getBaidu()
    {
        return baidu;
    }

    public PaddleConfig getPaddle()
    {
        return paddle;
    }

    public TesseractConfig getTesseract()
    {
        return tesseract;
    }

    public static class Baidu
    {
        /** accurate (default) | general */
        private String mode = "accurate";

        private String apiKey = "";

        private String secretKey = "";

        public String getMode()
        {
            return mode;
        }

        public void setMode(String mode)
        {
            this.mode = mode;
        }

        public String getApiKey()
        {
            return apiKey;
        }

        public void setApiKey(String apiKey)
        {
            this.apiKey = apiKey;
        }

        public String getSecretKey()
        {
            return secretKey;
        }

        public void setSecretKey(String secretKey)
        {
            this.secretKey = secretKey;
        }

        public boolean isConfigured()
        {
            return apiKey != null && !apiKey.isBlank() && secretKey != null && !secretKey.isBlank();
        }

        public boolean isAccurateMode()
        {
            return mode == null || mode.isBlank() || "accurate".equalsIgnoreCase(mode);
        }
    }

    public static class PaddleConfig
    {
        private boolean enabled = true;

        /** e.g. http://127.0.0.1:8867 */
        private String baseUrl = "http://127.0.0.1:8867";

        private int connectTimeoutMs = 5000;

        private int readTimeoutMs = 120000;

        /** text | mixed | auto — text uses PaddleOCR (Chinese exam); mixed uses Pix2Text for formulas */
        private String mode = "text";

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

        public String getMode()
        {
            return mode;
        }

        public void setMode(String mode)
        {
            this.mode = mode;
        }

        public String resolveHealthUrl()
        {
            return normalizeBaseUrl() + "/health";
        }

        public String resolveOcrUrl()
        {
            return normalizeBaseUrl() + "/ocr";
        }

        private String normalizeBaseUrl()
        {
            String url = baseUrl == null ? "" : baseUrl.trim();
            while (url.endsWith("/"))
            {
                url = url.substring(0, url.length() - 1);
            }
            return url;
        }
    }

    public static class TesseractConfig
    {
        private boolean enabled = true;

        /** tessdata directory; empty = auto-detect TESSDATA_PREFIX / common install paths */
        private String datapath = "";

        private String language = "chi_sim";

        private int dpi = 300;

        /** Tesseract page segmentation mode, 4 = single column (exam-friendly) */
        private int pageSegMode = 4;

        private boolean preprocessEnabled = true;

        private int minWidth = 2000;

        /** 0~1, split left/right columns for reading order (stem then options) */
        private double columnSplitRatio = 0.58;

        public boolean isPreprocessEnabled()
        {
            return preprocessEnabled;
        }

        public void setPreprocessEnabled(boolean preprocessEnabled)
        {
            this.preprocessEnabled = preprocessEnabled;
        }

        public int getMinWidth()
        {
            return minWidth;
        }

        public void setMinWidth(int minWidth)
        {
            this.minWidth = minWidth;
        }

        public double getColumnSplitRatio()
        {
            return columnSplitRatio;
        }

        public void setColumnSplitRatio(double columnSplitRatio)
        {
            this.columnSplitRatio = columnSplitRatio;
        }

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        public String getDatapath()
        {
            return datapath;
        }

        public void setDatapath(String datapath)
        {
            this.datapath = datapath;
        }

        public String getLanguage()
        {
            return language;
        }

        public void setLanguage(String language)
        {
            this.language = language;
        }

        public int getDpi()
        {
            return dpi;
        }

        public void setDpi(int dpi)
        {
            this.dpi = dpi;
        }

        public int getPageSegMode()
        {
            return pageSegMode;
        }

        public void setPageSegMode(int pageSegMode)
        {
            this.pageSegMode = pageSegMode;
        }

        public boolean isConfigured()
        {
            if (!enabled)
            {
                return false;
            }
            String path = resolveDatapath();
            if (path == null || path.isBlank())
            {
                return false;
            }
            File dir = new File(path);
            if (!dir.isDirectory())
            {
                return false;
            }
            String langs = language == null ? "" : language.trim();
            if (langs.isEmpty())
            {
                return false;
            }
            for (String lang : langs.split("\\+"))
            {
                String name = lang.trim();
                if (name.isEmpty())
                {
                    continue;
                }
                if (!new File(dir, name + ".traineddata").exists())
                {
                    return false;
                }
            }
            return true;
        }

        public String resolveDatapath()
        {
            if (datapath != null && !datapath.isBlank())
            {
                return datapath.trim();
            }
            String env = System.getenv("TESSDATA_PREFIX");
            if (env != null && !env.isBlank())
            {
                return env.trim();
            }
            String[] candidates = {
                "C:/Program Files/Tesseract-OCR/tessdata",
                "C:/Program Files (x86)/Tesseract-OCR/tessdata",
                "/usr/share/tesseract-ocr/5/tessdata",
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/local/share/tessdata"
            };
            for (String candidate : candidates)
            {
                if (new File(candidate).isDirectory())
                {
                    return candidate;
                }
            }
            return "";
        }
    }
}
