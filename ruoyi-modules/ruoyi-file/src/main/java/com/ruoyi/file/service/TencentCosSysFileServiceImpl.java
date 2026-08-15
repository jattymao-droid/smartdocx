package com.ruoyi.file.service;

import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.nacos.common.utils.IoUtils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.file.config.TencentCosProperties;
import com.ruoyi.file.utils.FileUploadUtils;

@Service
@ConditionalOnProperty(name = "file.storage-type", havingValue = "cos")
public class TencentCosSysFileServiceImpl implements ISysFileService
{
    @Autowired
    private TencentCosProperties cosProperties;

    @Autowired
    private COSClient cosClient;

    @Override
    public String uploadFile(MultipartFile file) throws Exception
    {
        InputStream inputStream = null;
        try
        {
            String objectKey = buildObjectKey(FileUploadUtils.extractFilename(file));
            inputStream = file.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (StringUtils.isNotEmpty(file.getContentType()))
            {
                metadata.setContentType(file.getContentType());
            }
            PutObjectRequest request = new PutObjectRequest(
                    cosProperties.getBucketName(), objectKey, inputStream, metadata);
            cosClient.putObject(request);
            return cosProperties.buildObjectUrl(objectKey);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Tencent COS failed to upload file", e);
        }
        finally
        {
            IoUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public void deleteFile(String fileUrl) throws Exception
    {
        try
        {
            String objectKey = resolveObjectKey(fileUrl);
            if (StringUtils.isEmpty(objectKey))
            {
                throw new IllegalArgumentException("Invalid COS file url");
            }
            cosClient.deleteObject(cosProperties.getBucketName(), objectKey);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Tencent COS failed to delete file", e);
        }
    }

    private String buildObjectKey(String fileName)
    {
        String name = fileName.startsWith("/") ? fileName.substring(1) : fileName;
        String prefix = cosProperties.getPrefix();
        if (StringUtils.isEmpty(prefix))
        {
            return name;
        }
        return prefix.replaceAll("/$", "") + "/" + name;
    }

    private String resolveObjectKey(String fileUrl)
    {
        if (StringUtils.isEmpty(fileUrl))
        {
            return null;
        }
        String url = fileUrl.trim();
        String domain = cosProperties.getDomain();
        if (StringUtils.isNotEmpty(domain))
        {
            String base = domain.replaceAll("/$", "");
            if (url.startsWith(base))
            {
                return url.substring(base.length()).replaceFirst("^/+", "");
            }
        }
        String marker = ".myqcloud.com/";
        int idx = url.indexOf(marker);
        if (idx >= 0)
        {
            return url.substring(idx + marker.length());
        }
        return null;
    }
}
