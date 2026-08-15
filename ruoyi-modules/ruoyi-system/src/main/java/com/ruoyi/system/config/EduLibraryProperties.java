package com.ruoyi.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edu.library")
public class EduLibraryProperties
{
    private final Audit audit = new Audit();
    private final Preview preview = new Preview();
    private final Upload upload = new Upload();
    private final Watermark watermark = new Watermark();
    private final Storage storage = new Storage();

    public Audit getAudit()
    {
        return audit;
    }

    public Preview getPreview()
    {
        return preview;
    }

    public Upload getUpload()
    {
        return upload;
    }

    public Watermark getWatermark()
    {
        return watermark;
    }

    public Storage getStorage()
    {
        return storage;
    }

    public static class Audit
    {
        /** When true, portal uploads require admin approval before public listing. */
        private boolean enabled = false;

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }
    }

    public static class Preview
    {
        /** kkFileView base URL, e.g. http://127.0.0.1:8012 */
        private String kkfileviewBaseUrl = "";

        /** Public gateway URL so preview services can fetch uploaded files */
        private String filePublicBaseUrl = "http://127.0.0.1:8080";

        /** Local storage root (mirrors file.path) */
        private String localFileRoot = "D:/ruoyi/uploadPath";

        /** URL prefix for local files (mirrors file.prefix) */
        private String localFilePrefix = "/statics";

        /** LibreOffice home directory (bundled with kkFileView on Windows) */
        private String libreOfficeHome = "";

        /** kkFileView cache root (file.dir), holds extracted archive inner files */
        private String kkfileviewFileRoot = "";

        /** Maximum pages shown in portal PDF preview when not overridden in sys_config. */
        private int maxPreviewPages = 5;

        public int getMaxPreviewPages()
        {
            return maxPreviewPages;
        }

        public void setMaxPreviewPages(int maxPreviewPages)
        {
            this.maxPreviewPages = maxPreviewPages;
        }

        public String getLibreOfficeHome()
        {
            return libreOfficeHome;
        }

        public void setLibreOfficeHome(String libreOfficeHome)
        {
            this.libreOfficeHome = libreOfficeHome;
        }

        public String getKkfileviewFileRoot()
        {
            return kkfileviewFileRoot;
        }

        public void setKkfileviewFileRoot(String kkfileviewFileRoot)
        {
            this.kkfileviewFileRoot = kkfileviewFileRoot;
        }

        public String getLocalFileRoot()
        {
            return localFileRoot;
        }

        public void setLocalFileRoot(String localFileRoot)
        {
            this.localFileRoot = localFileRoot;
        }

        public String getLocalFilePrefix()
        {
            return localFilePrefix;
        }

        public void setLocalFilePrefix(String localFilePrefix)
        {
            this.localFilePrefix = localFilePrefix;
        }

        public String getKkfileviewBaseUrl()
        {
            return kkfileviewBaseUrl;
        }

        public void setKkfileviewBaseUrl(String kkfileviewBaseUrl)
        {
            this.kkfileviewBaseUrl = kkfileviewBaseUrl;
        }

        public String getFilePublicBaseUrl()
        {
            return filePublicBaseUrl;
        }

        public void setFilePublicBaseUrl(String filePublicBaseUrl)
        {
            this.filePublicBaseUrl = filePublicBaseUrl;
        }

        public boolean isKkfileviewEnabled()
        {
            return kkfileviewBaseUrl != null && !kkfileviewBaseUrl.trim().isEmpty();
        }
    }

    public static class Upload
    {
        private int maxSizeMb = 50;

        public int getMaxSizeMb()
        {
            return maxSizeMb;
        }

        public void setMaxSizeMb(int maxSizeMb)
        {
            this.maxSizeMb = maxSizeMb;
        }
    }

    public static class Watermark
    {
        private boolean enabled = true;
        private String template = "{username}";

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        public String getTemplate()
        {
            return template;
        }

        public void setTemplate(String template)
        {
            this.template = template;
        }

        public String format(String username)
        {
            String text = template == null ? "{username}" : template;
            text = text.replace("{username}", username == null ? "" : username);
            text = text.replace("{date}", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
            return text;
        }
    }

    public static class Storage
    {
        /** local | minio | cos - mirrors ruoyi-file file.storage-type */
        private String type = "local";

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }
    }
}
