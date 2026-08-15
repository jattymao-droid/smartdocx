package com.ruoyi.system.service.education.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.config.EduLibraryProperties;
import com.ruoyi.system.domain.education.EduLibraryDocument;
import com.ruoyi.system.domain.education.EduLibraryTopic;
import com.ruoyi.system.domain.education.EduPayOrder;
import com.ruoyi.system.mapper.education.EduLibraryTopicMapper;
import com.ruoyi.system.service.education.IEduLibraryDocumentService;
import com.ruoyi.system.service.education.IEduLibraryTopicService;
import com.ruoyi.system.service.education.IEduPayService;
import com.ruoyi.system.service.education.support.EduLibraryOfficePdfConverter;

@Service
public class EduLibraryTopicServiceImpl implements IEduLibraryTopicService
{
    @Autowired
    private EduLibraryTopicMapper topicMapper;

    @Autowired
    private IEduPayService payService;

    @Autowired
    private IEduLibraryDocumentService documentService;

    @Autowired
    private EduLibraryOfficePdfConverter officePdfConverter;

    @Autowired
    private EduLibraryProperties libraryProperties;

    @Override
    public EduLibraryTopic selectEduLibraryTopicById(Long topicId, boolean portal, String viewer)
    {
        EduLibraryTopic topic = topicMapper.selectEduLibraryTopicById(topicId);
        if (topic == null)
        {
            return null;
        }
        if (portal && !"0".equals(topic.getStatus()))
        {
            throw new ServiceException("\u4e13\u9898\u4e0d\u5b58\u5728\u6216\u5df2\u4e0b\u67b6");
        }
        List<EduLibraryDocument> documents = topicMapper.selectTopicDocuments(topicId, portal);
        topic.setDocuments(documents);
        if (portal)
        {
            documentService.sanitizePortalDocuments(documents, viewer);
        }
        return topic;
    }

    @Override
    public List<EduLibraryTopic> selectEduLibraryTopicList(EduLibraryTopic query, boolean portal, String viewer)
    {
        if (query.getParams() == null)
        {
            query.setParams(new java.util.HashMap<>());
        }
        if (portal)
        {
            query.getParams().put("portalMode", "true");
            query.setStatus("0");
        }
        return topicMapper.selectEduLibraryTopicList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertEduLibraryTopic(EduLibraryTopic topic, String operator)
    {
        validateTopic(topic, true);
        if (topic.getOrderNum() == null)
        {
            topic.setOrderNum(0);
        }
        if (topic.getBundlePrice() == null)
        {
            topic.setBundlePrice(java.math.BigDecimal.ZERO);
        }
        if (topic.getDownloadCount() == null)
        {
            topic.setDownloadCount(0);
        }
        if (StringUtils.isEmpty(topic.getStatus()))
        {
            topic.setStatus("0");
        }
        topic.setCreateBy(operator);
        int rows = topicMapper.insertEduLibraryTopic(topic);
        saveTopicDocuments(topic.getTopicId(), topic.getDocumentIds());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateEduLibraryTopic(EduLibraryTopic topic, String operator)
    {
        if (topic.getTopicId() == null)
        {
            throw new ServiceException("\u4e13\u9898 ID \u4e0d\u80fd\u4e3a\u7a7a");
        }
        validateTopic(topic, false);
        topic.setUpdateBy(operator);
        int rows = topicMapper.updateEduLibraryTopic(topic);
        if (topic.getDocumentIds() != null)
        {
            topicMapper.deleteTopicDocumentsByTopicId(topic.getTopicId());
            saveTopicDocuments(topic.getTopicId(), topic.getDocumentIds());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteEduLibraryTopicByIds(Long[] topicIds)
    {
        if (topicIds == null || topicIds.length == 0)
        {
            return 0;
        }
        return topicMapper.deleteEduLibraryTopicByIds(topicIds);
    }

    @Override
    public void streamTopicZip(Long topicId, String viewer, HttpServletResponse response)
    {
        if (StringUtils.isEmpty(viewer))
        {
            throw new ServiceException("\u8bf7\u5148\u767b\u5f55");
        }
        EduLibraryTopic topic = topicMapper.selectEduLibraryTopicById(topicId);
        if (topic == null || !"0".equals(topic.getStatus()))
        {
            throw new ServiceException("\u4e13\u9898\u4e0d\u5b58\u5728\u6216\u5df2\u4e0b\u67b6");
        }
        payService.assertAccess(EduPayOrder.BIZ_LIBRARY_TOPIC, topicId, null, viewer);

        List<EduLibraryDocument> documents = topicMapper.selectTopicDocuments(topicId, true);
        if (documents == null || documents.isEmpty())
        {
            throw new ServiceException("\u4e13\u9898\u6682\u65e0\u53ef\u4e0b\u8f7d\u6587\u6863");
        }

        String zipName = sanitizeFileName(topic.getTitle()) + ".zip";
        response.setContentType("application/zip");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(zipName, java.nio.charset.StandardCharsets.UTF_8));

        Set<String> usedNames = new HashSet<>();
        int addedFiles = 0;
        try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream()))
        {
            for (EduLibraryDocument document : documents)
            {
                if (!"1".equals(document.getAllowDownload()))
                {
                    continue;
                }
                String entryName = resolveZipEntryName(document, usedNames);
                zip.putNextEntry(new ZipEntry(entryName));
                copyDocumentToZip(document, zip);
                zip.closeEntry();
                addedFiles++;
            }
            if (addedFiles == 0)
            {
                throw new ServiceException("\u4e13\u9898\u5185\u6587\u6863\u6682\u4e0d\u652f\u6301\u4e0b\u8f7d");
            }
            zip.finish();
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("\u6253\u5305\u4e0b\u8f7d\u5931\u8d25: " + ex.getMessage());
        }
        topicMapper.incrementDownloadCount(topicId);
    }

    private void saveTopicDocuments(Long topicId, Long[] documentIds)
    {
        if (topicId == null || documentIds == null || documentIds.length == 0)
        {
            return;
        }
        topicMapper.batchInsertTopicDocuments(topicId, documentIds);
    }

    private void validateTopic(EduLibraryTopic topic, boolean creating)
    {
        if (topic == null)
        {
            throw new ServiceException("\u4e13\u9898\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (creating && StringUtils.isEmpty(topic.getTitle()))
        {
            throw new ServiceException("\u4e13\u9898\u6807\u9898\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (StringUtils.isNotEmpty(topic.getTitle()) && topic.getTitle().length() > 200)
        {
            throw new ServiceException("\u4e13\u9898\u6807\u9898\u8fc7\u957f");
        }
        if (creating && (topic.getDocumentIds() == null || topic.getDocumentIds().length == 0))
        {
            throw new ServiceException("\u8bf7\u81f3\u5c11\u9009\u62e9\u4e00\u4e2a\u6587\u6863");
        }
    }

    private void copyDocumentToZip(EduLibraryDocument document, OutputStream out) throws Exception
    {
        String fileUrl = document.getFileUrl();
        if (StringUtils.isEmpty(fileUrl))
        {
            throw new ServiceException("\u6587\u6863\u300c" + document.getTitle() + "\u300d\u6587\u4ef6\u4e0d\u5b58\u5728");
        }
        Path localPath = officePdfConverter.resolveLocalPath(fileUrl);
        if (localPath != null && Files.isRegularFile(localPath))
        {
            Files.copy(localPath, out);
            return;
        }
        String fetchUrl = fileUrl.startsWith("http") ? fileUrl
                : libraryProperties.getPreview().getFilePublicBaseUrl().replaceAll("/$", "") + fileUrl;
        try (InputStream in = new URL(fetchUrl).openStream())
        {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0)
            {
                out.write(buffer, 0, len);
            }
        }
    }

    private static String resolveZipEntryName(EduLibraryDocument document, Set<String> usedNames)
    {
        String base = StringUtils.isNotEmpty(document.getFileName())
                ? document.getFileName()
                : sanitizeFileName(document.getTitle());
        if (StringUtils.isEmpty(base))
        {
            base = "document-" + document.getDocumentId();
        }
        if (!base.contains(".") && StringUtils.isNotEmpty(document.getFileExt()))
        {
            base = base + "." + document.getFileExt();
        }
        String name = base;
        int seq = 1;
        while (usedNames.contains(name))
        {
            int dot = base.lastIndexOf('.');
            if (dot > 0)
            {
                name = base.substring(0, dot) + "(" + seq + ")" + base.substring(dot);
            }
            else
            {
                name = base + "(" + seq + ")";
            }
            seq++;
        }
        usedNames.add(name);
        return name;
    }

    private static String sanitizeFileName(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return "topic";
        }
        return text.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
