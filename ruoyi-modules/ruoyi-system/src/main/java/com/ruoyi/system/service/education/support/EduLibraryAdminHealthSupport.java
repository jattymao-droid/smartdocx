package com.ruoyi.system.service.education.support;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.config.EduLibraryProperties;
import com.ruoyi.system.mapper.education.EduLibraryDocumentMapper;
import com.ruoyi.system.service.education.IEduLibraryAdminConfigService;

@Component
public class EduLibraryAdminHealthSupport
{
    @Autowired
    private EduLibraryProperties libraryProperties;

    @Autowired
    private EduLibraryDocumentMapper documentMapper;

    @Autowired
    private IEduLibraryAdminConfigService adminConfigService;

    public Map<String, Object> buildHealthReport()
    {
        Map<String, Object> report = new HashMap<>();
        report.put("storageType", libraryProperties.getStorage().getType());
        report.put("localFileRoot", libraryProperties.getPreview().getLocalFileRoot());
        report.put("filePublicBaseUrl", libraryProperties.getPreview().getFilePublicBaseUrl());
        report.put("kkfileviewBaseUrl", libraryProperties.getPreview().getKkfileviewBaseUrl());
        report.put("libreOfficeHome", libraryProperties.getPreview().getLibreOfficeHome());
        report.put("auditEnabled", libraryProperties.getAudit().isEnabled());
        report.put("previewMaxPages", adminConfigService.resolvePreviewMaxPages());
        report.put("defaultPreviewMaxPages", libraryProperties.getPreview().getMaxPreviewPages());

        report.put("localFileRootOk", checkLocalRoot());
        report.put("libreOfficeOk", checkLibreOffice());
        report.put("kkfileviewOk", checkKkfileview());
        report.put("stats", documentMapper.selectLibraryAdminStats());
        return report;
    }

    private boolean checkLocalRoot()
    {
        String root = libraryProperties.getPreview().getLocalFileRoot();
        if (StringUtils.isEmpty(root))
        {
            return false;
        }
        return Files.isDirectory(Paths.get(root));
    }

    private boolean checkLibreOffice()
    {
        String home = libraryProperties.getPreview().getLibreOfficeHome();
        if (StringUtils.isNotEmpty(home))
        {
            Path win = Paths.get(home, "program", "soffice.exe");
            if (Files.isRegularFile(win))
            {
                return true;
            }
            Path linux = Paths.get(home, "program", "soffice");
            if (Files.isRegularFile(linux))
            {
                return true;
            }
        }
        Path bundled = Paths.get("tools", "kkfileview", "dist", "kkFileView-4.2.1", "libreoffice", "program", "soffice.exe");
        return Files.isRegularFile(bundled);
    }

    private boolean checkKkfileview()
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
}
