package com.ruoyi.system.service.education.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.auth.AuthUtil;
import com.ruoyi.system.domain.education.EduLibraryAuditBody;
import com.ruoyi.system.domain.education.EduLibraryDocument;
import com.ruoyi.system.domain.education.EduLibraryRecommendBody;
import com.ruoyi.system.domain.education.EduLibraryStatusBody;
import com.ruoyi.system.domain.education.EduPayCheckResult;
import com.ruoyi.system.mapper.education.EduLibraryDocumentMapper;
import com.ruoyi.system.domain.education.EduPayOrder;
import com.ruoyi.system.service.education.IEduLibraryAdminConfigService;
import com.ruoyi.system.service.education.IEduLibraryDocumentService;
import com.ruoyi.system.service.education.IEduLibraryVipService;
import com.ruoyi.system.service.education.IEduPayService;
import com.ruoyi.system.config.EduLibraryProperties;
import com.ruoyi.system.service.education.support.EduLibraryConvertTask;
import com.ruoyi.system.service.education.support.EduLibraryOfficePdfConverter;
import com.ruoyi.system.service.education.support.EduLibraryPreviewSupport;

@Service
public class EduLibraryDocumentServiceImpl implements IEduLibraryDocumentService
{
    @Autowired
    private EduLibraryDocumentMapper documentMapper;

    @Autowired
    private EduLibraryPreviewSupport previewSupport;

    @Autowired
    private EduLibraryConvertTask convertTask;

    @Autowired
    private EduLibraryOfficePdfConverter officePdfConverter;

    @Autowired
    private EduLibraryProperties libraryProperties;

    @Autowired
    private IEduLibraryAdminConfigService adminConfigService;

    @Autowired
    private IEduPayService payService;

    @Autowired
    private IEduLibraryVipService vipService;

    @Override
    public EduLibraryDocument selectEduLibraryDocumentById(Long documentId, String viewer)
    {
        EduLibraryDocument document = documentMapper.selectEduLibraryDocumentById(documentId);
        if (document == null)
        {
            return null;
        }
        boolean loggedIn = StringUtils.isNotEmpty(viewer);
        if (!canView(document, viewer, loggedIn))
        {
            throw new ServiceException("No permission to view this document");
        }
        if (loggedIn)
        {
            document.setFavorited(documentMapper.countFavorite(documentId, viewer) > 0);
        }
        applyPortalDownloadProtection(document, viewer);
        return document;
    }

    @Override
    public List<EduLibraryDocument> selectEduLibraryDocumentList(EduLibraryDocument query, boolean portalMode, String viewer)
    {
        if (query.getParams() == null)
        {
            query.setParams(new HashMap<>());
        }
        boolean loggedIn = StringUtils.isNotEmpty(viewer);
        if (portalMode)
        {
            query.getParams().put("portalMode", "true");
            query.getParams().put("loggedIn", loggedIn ? "true" : "false");
        }
        if (loggedIn)
        {
            query.getParams().put("currentUser", viewer);
        }
        List<EduLibraryDocument> list = documentMapper.selectEduLibraryDocumentList(query);
        if (portalMode)
        {
            sanitizePortalDocuments(list, viewer);
        }
        return list;
    }

    @Override
    public List<EduLibraryDocument> selectMineList(EduLibraryDocument query, String username)
    {
        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("Login required");
        }
        if (query.getParams() == null)
        {
            query.setParams(new HashMap<>());
        }
        query.getParams().put("mineOnly", "true");
        query.getParams().put("currentUser", username);
        return documentMapper.selectEduLibraryDocumentList(query);
    }

    @Override
    public List<EduLibraryDocument> selectFavoriteList(String username)
    {
        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("Login required");
        }
        List<EduLibraryDocument> list = documentMapper.selectFavoriteList(username);
        sanitizePortalDocuments(list, username);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertEduLibraryDocument(EduLibraryDocument document, String operator)
    {
        return insertEduLibraryDocument(document, operator, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertEduLibraryDocument(EduLibraryDocument document, String operator, boolean portalUpload)
    {
        validateDocument(document, true);
        document.setDocumentCode(nextDocumentCode());
        document.setCreateBy(operator);
        document.setStatus("0");
        document.setDelFlag("0");
        if (portalUpload && libraryProperties.getAudit().isEnabled())
        {
            document.setAuditStatus("0");
        }
        else
        {
            document.setAuditStatus("1");
        }
        document.setRecommendFlag(StringUtils.defaultIfEmpty(document.getRecommendFlag(), "0"));
        document.setRecommendOrder(document.getRecommendOrder() == null ? 0 : document.getRecommendOrder());
        document.setFileStorage(StringUtils.defaultIfEmpty(document.getFileStorage(), libraryProperties.getStorage().getType()));
        document.setVisibility(StringUtils.defaultIfEmpty(document.getVisibility(), "school"));
        document.setAllowDownload(StringUtils.defaultIfEmpty(document.getAllowDownload(), "1"));
        if (document.getDownloadPrice() == null)
        {
            document.setDownloadPrice(BigDecimal.ZERO);
        }
        previewSupport.applyPreviewMeta(document);
        int rows = documentMapper.insertEduLibraryDocument(document);
        if (rows > 0 && document.getDocumentId() != null)
        {
            scheduleConvertAfterCommit(document.getDocumentId());
        }
        return rows;
    }

    private void scheduleConvertAfterCommit(Long documentId)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCommit()
                {
                    convertTask.convertAsync(documentId);
                }
            });
        }
        else
        {
            convertTask.convertAsync(documentId);
        }
    }

    @Override
    public int updateEduLibraryDocument(EduLibraryDocument document, String operator, boolean adminBypass)
    {
        if (document.getDocumentId() == null)
        {
            throw new ServiceException("Document id is required");
        }
        EduLibraryDocument existing = documentMapper.selectEduLibraryDocumentById(document.getDocumentId());
        if (existing == null)
        {
            throw new ServiceException("Document not found");
        }
        if (!adminBypass)
        {
            assertOwner(existing, operator);
        }
        document.setUpdateBy(operator);
        return documentMapper.updateEduLibraryDocument(document);
    }

    @Override
    public int deleteEduLibraryDocumentByIds(Long[] documentIds, String operator, boolean adminBypass)
    {
        if (documentIds == null || documentIds.length == 0)
        {
            return 0;
        }
        if (!adminBypass)
        {
            for (Long documentId : documentIds)
            {
                EduLibraryDocument existing = documentMapper.selectEduLibraryDocumentById(documentId);
                if (existing != null)
                {
                    assertOwner(existing, operator);
                }
            }
        }
        return documentMapper.deleteEduLibraryDocumentByIds(documentIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditDocuments(EduLibraryAuditBody body, String operator)
    {
        if (body == null || body.getDocumentIds() == null || body.getDocumentIds().length == 0)
        {
            return 0;
        }
        if (StringUtils.isEmpty(body.getAuditStatus()))
        {
            throw new ServiceException("Audit status is required");
        }
        int rows = 0;
        for (Long documentId : body.getDocumentIds())
        {
            EduLibraryDocument patch = new EduLibraryDocument();
            patch.setDocumentId(documentId);
            patch.setAuditStatus(body.getAuditStatus());
            patch.setAuditRemark(body.getAuditRemark());
            patch.setAuditBy(operator);
            patch.setAuditTime(new Date());
            patch.setUpdateBy(operator);
            rows += documentMapper.updateEduLibraryDocument(patch);
        }
        return rows;
    }

    @Override
    public Map<String, Object> buildPreviewPayload(Long documentId, String viewer)
    {
        EduLibraryDocument document = documentMapper.selectEduLibraryDocumentById(documentId);
        if (document == null)
        {
            throw new ServiceException("Document not found");
        }
        boolean loggedIn = StringUtils.isNotEmpty(viewer);
        if (!canView(document, viewer, loggedIn))
        {
            throw new ServiceException("No permission to view this document");
        }
        EduLibraryPreviewSupport.PreviewMeta meta = previewSupport.resolvePreviewMeta(document);
        if ("pending".equals(meta.convertStatus)
                && (officePdfConverter.needsConversion(document.getFileExt())
                    || EduLibraryPreviewSupport.isArchiveExt(document.getFileExt())))
        {
            convertTask.convertAsync(documentId);
        }
        else if ("success".equals(meta.convertStatus)
                && StringUtils.isNotEmpty(meta.previewUrl)
                && !"success".equals(document.getConvertStatus()))
        {
            syncPreviewMeta(documentId, meta, viewer);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("documentId", document.getDocumentId());
        payload.put("previewType", meta.previewType);
        payload.put("previewUrl", meta.previewUrl);
        payload.put("fileUrl", document.getFileUrl());
        payload.put("fileExt", document.getFileExt());
        payload.put("convertStatus", meta.convertStatus);
        payload.put("previewError", meta.previewError);
        payload.put("allowDownload", document.getAllowDownload());
        payload.put("title", document.getTitle());
        EduPayCheckResult payCheck = payService.checkAccess(
                EduPayOrder.BIZ_LIBRARY_DOCUMENT, documentId, null, viewer);
        payload.put("payEnabled", payCheck.isEnabled());
        payload.put("needPay", payCheck.isNeedPay());
        payload.put("purchased", payCheck.isPurchased());
        payload.put("downloadPrice", document.getDownloadPrice());
        if (shouldProtectDownload(document, viewer, payCheck))
        {
            payload.put("fileUrl", null);
            if (StringUtils.isNotEmpty(meta.previewType) && !"unsupported".equals(meta.previewType)
                    && !"kkfileview".equals(meta.previewType))
            {
                payload.put("previewUrl", buildPreviewProxyPath(documentId));
            }
        }
        if (libraryProperties.getWatermark().isEnabled() && StringUtils.isNotEmpty(viewer))
        {
            payload.put("watermark", libraryProperties.getWatermark().format(viewer));
        }
        else if (StringUtils.isNotEmpty(viewer))
        {
            payload.put("watermark", viewer);
        }
        int previewPageLimit = vipService.resolvePreviewMaxPages(viewer, adminConfigService.resolvePreviewMaxPages());
        payload.put("previewPageLimit", previewPageLimit);
        Integer totalPageCount = document.getPageCount();
        if (totalPageCount != null && totalPageCount > 0)
        {
            int previewPageCount = Math.min(totalPageCount, previewPageLimit);
            payload.put("totalPageCount", totalPageCount);
            payload.put("previewPageCount", previewPageCount);
            payload.put("previewTruncated", totalPageCount > previewPageLimit);
            payload.put("remainingPages", Math.max(0, totalPageCount - previewPageLimit));
        }
        return payload;
    }

    @Override
    public void streamPreviewContent(Long documentId, String viewer, HttpServletResponse response)
    {
        EduLibraryDocument document = documentMapper.selectEduLibraryDocumentById(documentId);
        if (document == null)
        {
            throw new ServiceException("Document not found");
        }
        boolean loggedIn = StringUtils.isNotEmpty(viewer);
        if (!canView(document, viewer, loggedIn))
        {
            throw new ServiceException("No permission to view this document");
        }
        EduLibraryPreviewSupport.PreviewMeta meta = previewSupport.resolvePreviewMeta(document);
        String sourceUrl = StringUtils.isNotEmpty(meta.previewUrl) ? meta.previewUrl : document.getFileUrl();
        if (StringUtils.isEmpty(sourceUrl))
        {
            throw new ServiceException("Preview source not available");
        }
        String contentType = resolvePreviewContentType(meta.previewType, document.getFileExt());
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "private, max-age=120");
        try (InputStream in = openSourceStream(sourceUrl);
                OutputStream out = response.getOutputStream())
        {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) >= 0)
            {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("Unable to stream preview content");
        }
    }

    @Override
    public void applyPortalDownloadProtection(EduLibraryDocument document, String viewer)
    {
        if (document == null || isLibraryAdmin())
        {
            return;
        }
        EduPayCheckResult payCheck = payService.checkAccess(
                EduPayOrder.BIZ_LIBRARY_DOCUMENT, document.getDocumentId(), null, viewer);
        if (shouldProtectDownload(document, viewer, payCheck))
        {
            document.setFileUrl(null);
        }
    }

    @Override
    public void sanitizePortalDocuments(List<EduLibraryDocument> documents, String viewer)
    {
        if (documents == null || documents.isEmpty() || isLibraryAdmin())
        {
            return;
        }
        for (EduLibraryDocument document : documents)
        {
            applyPortalDownloadProtection(document, viewer);
        }
    }

    @Override
    public String resolveDownloadUrl(Long documentId, String viewer)
    {
        EduLibraryDocument document = documentMapper.selectEduLibraryDocumentById(documentId);
        if (document == null)
        {
            throw new ServiceException("Document not found");
        }
        boolean loggedIn = StringUtils.isNotEmpty(viewer);
        if (!canView(document, viewer, loggedIn))
        {
            throw new ServiceException("No permission to view this document");
        }
        if (!"1".equals(document.getAllowDownload()))
        {
            throw new ServiceException("Download is not allowed for this document");
        }
        payService.assertAccess(EduPayOrder.BIZ_LIBRARY_DOCUMENT, documentId, null, viewer);
        documentMapper.incrementDownloadCount(documentId);
        return document.getFileUrl();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recommendDocuments(EduLibraryRecommendBody body, String operator)
    {
        if (body == null || body.getDocumentIds() == null || body.getDocumentIds().length == 0)
        {
            return 0;
        }
        int rows = 0;
        for (Long documentId : body.getDocumentIds())
        {
            EduLibraryDocument patch = new EduLibraryDocument();
            patch.setDocumentId(documentId);
            patch.setRecommendFlag(StringUtils.defaultIfEmpty(body.getRecommendFlag(), "0"));
            if (body.getRecommendOrder() != null)
            {
                patch.setRecommendOrder(body.getRecommendOrder());
            }
            patch.setUpdateBy(operator);
            rows += documentMapper.updateEduLibraryDocument(patch);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeDocumentStatus(EduLibraryStatusBody body, String operator)
    {
        if (body == null || body.getDocumentIds() == null || body.getDocumentIds().length == 0)
        {
            return 0;
        }
        if (!"0".equals(body.getStatus()) && !"1".equals(body.getStatus()))
        {
            throw new ServiceException("Invalid document status");
        }
        int rows = 0;
        for (Long documentId : body.getDocumentIds())
        {
            EduLibraryDocument patch = new EduLibraryDocument();
            patch.setDocumentId(documentId);
            patch.setStatus(body.getStatus());
            patch.setUpdateBy(operator);
            rows += documentMapper.updateEduLibraryDocument(patch);
        }
        return rows;
    }

    @Override
    public int reconvertDocument(Long documentId, String operator)
    {
        EduLibraryDocument existing = documentMapper.selectEduLibraryDocumentById(documentId);
        if (existing == null)
        {
            throw new ServiceException("Document not found");
        }
        EduLibraryDocument patch = new EduLibraryDocument();
        patch.setDocumentId(documentId);
        patch.setConvertStatus("pending");
        patch.setPreviewError("");
        patch.setUpdateBy(operator);
        documentMapper.updateEduLibraryDocument(patch);
        convertTask.convertAsync(documentId);
        return 1;
    }

    @Override
    public void recordView(Long documentId, String viewer)
    {
        EduLibraryDocument document = documentMapper.selectEduLibraryDocumentById(documentId);
        if (document == null)
        {
            return;
        }
        boolean loggedIn = StringUtils.isNotEmpty(viewer);
        if (!canView(document, viewer, loggedIn))
        {
            throw new ServiceException("No permission to view this document");
        }
        documentMapper.incrementViewCount(documentId);
        if (loggedIn)
        {
            documentMapper.upsertReadLog(documentId, viewer, BigDecimal.ZERO);
        }
    }

    @Override
    public List<EduLibraryDocument> selectRelatedDocuments(Long documentId, String viewer)
    {
        EduLibraryDocument document = selectEduLibraryDocumentById(documentId, viewer);
        boolean loggedIn = StringUtils.isNotEmpty(viewer);
        List<EduLibraryDocument> related = documentMapper.selectRelatedDocuments(documentId, document.getSubjectId(),
                document.getCategoryId(), viewer, loggedIn, 6);
        sanitizePortalDocuments(related, viewer);
        return related;
    }

    @Override
    public List<EduLibraryDocument> selectContinueReadingList(String username, int limit)
    {
        if (StringUtils.isEmpty(username))
        {
            return Collections.emptyList();
        }
        int size = limit <= 0 ? 5 : Math.min(limit, 20);
        List<EduLibraryDocument> list = documentMapper.selectContinueReadingList(username, size);
        sanitizePortalDocuments(list, username);
        return list;
    }

    @Override
    public void saveReadProgress(Long documentId, String username, BigDecimal readProgress)
    {
        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("Login required");
        }
        selectEduLibraryDocumentById(documentId, username);
        BigDecimal progress = readProgress == null ? BigDecimal.ZERO : readProgress;
        if (progress.compareTo(BigDecimal.ZERO) < 0)
        {
            progress = BigDecimal.ZERO;
        }
        if (progress.compareTo(new BigDecimal("100")) > 0)
        {
            progress = new BigDecimal("100");
        }
        documentMapper.upsertReadLog(documentId, username, progress);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addFavorite(Long documentId, String username)
    {
        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("Login required");
        }
        selectEduLibraryDocumentById(documentId, username);
        int rows = documentMapper.insertFavorite(documentId, username);
        if (rows > 0)
        {
            documentMapper.updateFavoriteCount(documentId, 1);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeFavorite(Long documentId, String username)
    {
        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("Login required");
        }
        int rows = documentMapper.deleteFavorite(documentId, username);
        if (rows > 0)
        {
            documentMapper.updateFavoriteCount(documentId, -1);
        }
        return rows;
    }

    @Override
    public boolean canView(EduLibraryDocument document, String viewer, boolean loggedIn)
    {
        if (document == null || !"0".equals(document.getDelFlag()) || !"0".equals(document.getStatus()))
        {
            return false;
        }
        if (!"1".equals(document.getAuditStatus()))
        {
            return loggedIn && (isOwner(document, viewer));
        }
        if ("public".equals(document.getVisibility()))
        {
            return true;
        }
        if (!loggedIn)
        {
            return false;
        }
        if ("school".equals(document.getVisibility()))
        {
            return true;
        }
        if ("private".equals(document.getVisibility()))
        {
            return isOwner(document, viewer);
        }
        return isOwner(document, viewer);
    }

    private void assertOwner(EduLibraryDocument document, String operator)
    {
        if (!isOwner(document, operator))
        {
            throw new ServiceException("No permission to modify this document");
        }
    }

    private void validateDocument(EduLibraryDocument document, boolean creating)
    {
        if (document == null)
        {
            throw new ServiceException("Document is required");
        }
        if (StringUtils.isEmpty(document.getTitle()))
        {
            throw new ServiceException("Title is required");
        }
        if (creating)
        {
            if (StringUtils.isEmpty(document.getFileUrl()))
            {
                throw new ServiceException("File url is required");
            }
            if (StringUtils.isEmpty(document.getFileName()))
            {
                throw new ServiceException("File name is required");
            }
            if (StringUtils.isEmpty(document.getFileExt()))
            {
                throw new ServiceException("File extension is required");
            }
            if (!EduLibraryPreviewSupport.isAllowedExt(document.getFileExt()))
            {
                throw new ServiceException("Unsupported file type");
            }
        }
    }

    private String nextDocumentCode()
    {
        String prefix = "WK" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        String max = documentMapper.selectMaxDocumentCodeByPrefix(prefix);
        int seq = 1;
        if (StringUtils.isNotEmpty(max) && max.length() > prefix.length())
        {
            try
            {
                seq = Integer.parseInt(max.substring(prefix.length())) + 1;
            }
            catch (NumberFormatException ignored)
            {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private boolean isOwner(EduLibraryDocument document, String viewer)
    {
        return document != null && StringUtils.isNotEmpty(viewer) && viewer.equals(document.getCreateBy());
    }

    private boolean isLibraryAdmin()
    {
        try
        {
            return AuthUtil.hasPermi("education:library:edit");
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private boolean shouldProtectDownload(EduLibraryDocument document, String viewer, EduPayCheckResult payCheck)
    {
        if (document == null || payCheck == null || isLibraryAdmin())
        {
            return false;
        }
        if (isOwner(document, viewer))
        {
            return false;
        }
        return payCheck.isNeedPay() && !payCheck.isPurchased();
    }

    private static String buildPreviewProxyPath(Long documentId)
    {
        return "/system/education/library/document/" + documentId + "/preview-content";
    }

    private static String resolvePreviewContentType(String previewType, String fileExt)
    {
        if ("txt".equalsIgnoreCase(previewType))
        {
            return MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8";
        }
        return MediaType.APPLICATION_PDF_VALUE;
    }

    private InputStream openSourceStream(String sourceUrl) throws Exception
    {
        Path localPath = officePdfConverter.resolveLocalPath(sourceUrl);
        if (localPath != null && Files.isRegularFile(localPath))
        {
            return Files.newInputStream(localPath);
        }
        String fetchUrl = sourceUrl;
        if (!fetchUrl.startsWith("http"))
        {
            String base = libraryProperties.getPreview().getFilePublicBaseUrl();
            if (StringUtils.isEmpty(base))
            {
                throw new ServiceException("Preview source not available");
            }
            fetchUrl = base.replaceAll("/$", "") + (fetchUrl.startsWith("/") ? fetchUrl : "/" + fetchUrl);
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(fetchUrl).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();
        if (code >= 400)
        {
            throw new ServiceException("Preview source not available");
        }
        return connection.getInputStream();
    }

    private void syncPreviewMeta(Long documentId, EduLibraryPreviewSupport.PreviewMeta meta, String operator)
    {
        EduLibraryDocument patch = new EduLibraryDocument();
        patch.setDocumentId(documentId);
        patch.setConvertStatus(meta.convertStatus);
        patch.setPreviewType(meta.previewType);
        patch.setPreviewUrl(meta.previewUrl);
        patch.setPreviewError(meta.previewError == null ? "" : meta.previewError);
        patch.setUpdateBy(StringUtils.isNotEmpty(operator) ? operator : "system");
        documentMapper.updateEduLibraryDocument(patch);
    }
}
