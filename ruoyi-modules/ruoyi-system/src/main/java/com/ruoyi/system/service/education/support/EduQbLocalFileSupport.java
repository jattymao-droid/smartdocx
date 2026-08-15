package com.ruoyi.system.service.education.support;

import java.io.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Local file paths for question-bank uploads/exports (maps to file.path in config).
 */
@Component
public class EduQbLocalFileSupport
{
    private static String localFilePath = "D:/ruoyi/uploadPath";

    @Value("${file.path:D:/ruoyi/uploadPath}")
    public void setLocalFilePath(String path)
    {
        localFilePath = path;
    }

    public static String getUploadPath()
    {
        return localFilePath;
    }

    public static String getProfile()
    {
        return localFilePath;
    }

    public static String getDownloadPath()
    {
        return localFilePath + File.separator + "download";
    }

    public static String stripPrefix(String path)
    {
        if (path == null || path.isEmpty())
        {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    public static File resolveStoredFile(String storedPath)
    {
        return new File(localFilePath, stripPrefix(storedPath).replace('/', File.separatorChar));
    }
}
