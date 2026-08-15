package com.ruoyi.system.service.education.support;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.config.EduLibraryProperties;
import com.ruoyi.system.domain.education.EduLibraryDocument;

@Component
public class EduLibraryPreviewSupport
{
    @Autowired
    private EduLibraryProperties libraryProperties;

    @Autowired
    private EduLibraryOfficePdfConverter officePdfConverter;

    public String resolveExistingPreviewUrl(String fileUrl)
    {
        return officePdfConverter.resolveExistingPreviewUrl(fileUrl);
    }

    public void applyPreviewMeta(EduLibraryDocument document)
    {
        PreviewMeta meta = resolvePreviewMeta(document);
        document.setConvertStatus(meta.convertStatus);
        document.setPreviewType(meta.previewType);
        document.setPreviewUrl(meta.previewUrl);
        document.setPreviewError(meta.previewError);
    }

    public PreviewMeta resolvePreviewMeta(EduLibraryDocument document)
    {
        PreviewMeta meta = new PreviewMeta();
        if (document == null || document.getFileExt() == null)
        {
            return meta;
        }
        String ext = document.getFileExt().trim().toLowerCase();
        if ("pdf".equals(ext))
        {
            meta.convertStatus = "success";
            meta.previewType = "pdf";
            meta.previewUrl = document.getFileUrl();
            return meta;
        }
        if ("txt".equals(ext))
        {
            meta.convertStatus = "success";
            meta.previewType = "txt";
            meta.previewUrl = document.getFileUrl();
            return meta;
        }
        if (isArchiveExt(ext))
        {
            if (StringUtils.isNotEmpty(document.getConvertStatus())
                    && !"none".equals(document.getConvertStatus())
                    && !"pending".equals(document.getConvertStatus()))
            {
                meta.convertStatus = document.getConvertStatus();
                meta.previewType = StringUtils.defaultIfEmpty(document.getPreviewType(), "kkfileview");
                meta.previewUrl = StringUtils.isNotEmpty(document.getPreviewUrl())
                        ? document.getPreviewUrl()
                        : buildKkfileviewUrl(document.getFileUrl());
                meta.previewError = document.getPreviewError();
                return meta;
            }
            return resolveKkfileviewMeta(document.getFileUrl(), meta);
        }
        if (officePdfConverter.needsConversion(ext))
        {
            if (StringUtils.isNotEmpty(document.getConvertStatus())
                    && !"none".equals(document.getConvertStatus())
                    && !"pending".equals(document.getConvertStatus()))
            {
                meta.convertStatus = document.getConvertStatus();
                meta.previewType = StringUtils.defaultIfEmpty(document.getPreviewType(), "pdf");
                meta.previewUrl = document.getPreviewUrl();
                meta.previewError = document.getPreviewError();
                return meta;
            }
            String existingPreview = officePdfConverter.resolveExistingPreviewUrl(document.getFileUrl());
            if (StringUtils.isNotEmpty(existingPreview))
            {
                meta.convertStatus = "success";
                meta.previewType = "pdf";
                meta.previewUrl = existingPreview;
                meta.previewError = null;
                return meta;
            }
            meta.convertStatus = "pending";
            meta.previewType = "pdf";
            meta.previewUrl = null;
            meta.previewError = null;
            return meta;
        }
        if (libraryProperties.getPreview().isKkfileviewEnabled() && isKkfileviewExt(ext))
        {
            return resolveKkfileviewMeta(document.getFileUrl(), meta);
        }
        meta.convertStatus = "pending";
        meta.previewType = "unsupported";
        meta.previewUrl = null;
        meta.previewError = "Preview for ." + ext + " requires kkFileView (configure edu.library.preview.kkfileview-base-url)";
        return meta;
    }

    public String buildKkfileviewUrl(String fileUrl)
    {
        if (StringUtils.isEmpty(fileUrl) || !libraryProperties.getPreview().isKkfileviewEnabled())
        {
            return null;
        }
        String base = libraryProperties.getPreview().getFilePublicBaseUrl();
        if (StringUtils.isEmpty(base))
        {
            return null;
        }
        String fullUrl = officePdfConverter.normalizePublicFileUrl(
                fileUrl.startsWith("http") ? fileUrl : base.replaceAll("/$", "") + fileUrl);
        String encoded = URLEncoder.encode(
                Base64.getEncoder().encodeToString(fullUrl.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
        return libraryProperties.getPreview().getKkfileviewBaseUrl().replaceAll("/$", "")
                + "/onlinePreview?url=" + encoded;
    }

    private boolean isKkfileviewReachable()
    {
        String base = libraryProperties.getPreview().getKkfileviewBaseUrl();
        if (StringUtils.isEmpty(base))
        {
            return false;
        }
        HttpURLConnection conn = null;
        try
        {
            conn = (HttpURLConnection) new URL(base.replaceAll("/$", "") + "/").openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code >= 200 && code < 500;
        }
        catch (Exception ex)
        {
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

    private PreviewMeta resolveKkfileviewMeta(String fileUrl, PreviewMeta meta)
    {
        if (!libraryProperties.getPreview().isKkfileviewEnabled())
        {
            meta.convertStatus = "failed";
            meta.previewType = "unsupported";
            meta.previewError = "\u538b\u7f29\u5305\u9884\u89c8\u9700\u8981\u542f\u7528 kkFileView\uff0c\u8bf7\u5728\u540e\u53f0\u914d\u7f6e edu.library.preview.kkfileview-base-url";
            return meta;
        }
        if (!isKkfileviewReachable())
        {
            meta.convertStatus = "failed";
            meta.previewType = "unsupported";
            meta.previewError = "\u9884\u89c8\u670d\u52a1\u672a\u542f\u52a8\uff0c\u8bf7\u4e0b\u8f7d\u6587\u4ef6\u67e5\u770b\uff0c\u6216\u542f\u52a8 kkFileView\uff08\u7aef\u53e3 8012\uff09";
            return meta;
        }
        meta.previewType = "kkfileview";
        meta.previewUrl = buildKkfileviewUrl(fileUrl);
        if (StringUtils.isEmpty(meta.previewUrl))
        {
            meta.convertStatus = "failed";
            meta.previewType = "unsupported";
            meta.previewError = "Unable to build kkFileView preview URL";
            return meta;
        }
        meta.convertStatus = "pending";
        meta.previewError = null;
        return meta;
    }

    public static boolean isArchiveExt(String ext)
    {
        if (ext == null)
        {
            return false;
        }
        switch (ext.trim().toLowerCase())
        {
            case "zip":
            case "rar":
            case "7z":
                return true;
            default:
                return false;
        }
    }

    public static boolean isAllowedExt(String ext)
    {
        if (ext == null)
        {
            return false;
        }
        switch (ext.trim().toLowerCase())
        {
            case "pdf":
            case "txt":
            case "doc":
            case "docx":
            case "ppt":
            case "pptx":
            case "xls":
            case "xlsx":
            case "zip":
            case "rar":
            case "7z":
                return true;
            default:
                return false;
        }
    }

    private static boolean isKkfileviewExt(String ext)
    {
        switch (ext)
        {
            case "doc":
            case "ppt":
            case "pptx":
            case "xls":
            case "xlsx":
            case "zip":
            case "rar":
            case "7z":
                return true;
            default:
                return false;
        }
    }

    public static final class PreviewMeta
    {
        public String convertStatus = "none";
        public String previewType;
        public String previewUrl;
        public String previewError;
    }
}
