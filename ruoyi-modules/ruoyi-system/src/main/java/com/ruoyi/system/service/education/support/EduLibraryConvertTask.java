package com.ruoyi.system.service.education.support;



import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.scheduling.annotation.Async;

import org.springframework.stereotype.Component;

import com.ruoyi.common.core.utils.StringUtils;

import com.ruoyi.system.domain.education.EduLibraryDocument;

import com.ruoyi.system.mapper.education.EduLibraryDocumentMapper;



@Component

public class EduLibraryConvertTask

{

    private static final Logger log = LoggerFactory.getLogger(EduLibraryConvertTask.class);

    private static final int MAX_ATTEMPTS = 3;

    private static final long RETRY_DELAY_MS = 1500L;



    @Autowired

    private EduLibraryDocumentMapper documentMapper;



    @Autowired

    private EduLibraryPreviewSupport previewSupport;



    @Autowired

    private EduLibraryOfficePdfConverter officePdfConverter;



    @Autowired

    private EduLibraryCoverGenerator coverGenerator;



    @Autowired

    private EduLibraryArchiveConvertSupport archiveConvertSupport;



    @Async

    public void convertAsync(Long documentId)

    {

        convertWithRetry(documentId, 0);

    }



    private void convertWithRetry(Long documentId, int attempt)

    {

        if (documentId == null)

        {

            return;

        }

        try

        {

            EduLibraryDocument document = documentMapper.selectEduLibraryDocumentById(documentId);

            if (document == null)

            {

                return;

            }

            String ext = document.getFileExt() == null ? "" : document.getFileExt().trim().toLowerCase();

            if (officePdfConverter.needsConversion(ext))

            {

                if (!officePdfConverter.isSourceReady(document.getFileUrl()) && attempt < MAX_ATTEMPTS - 1)

                {

                    Thread.sleep(RETRY_DELAY_MS);

                    convertWithRetry(documentId, attempt + 1);

                    return;

                }

                String pdfUrl = officePdfConverter.convertToPreviewPdf(document.getFileUrl());

                if (StringUtils.isEmpty(pdfUrl))

                {

                    pdfUrl = officePdfConverter.resolveExistingPreviewUrl(document.getFileUrl());

                }

                EduLibraryDocument patch = new EduLibraryDocument();

                patch.setDocumentId(documentId);

                if (StringUtils.isNotEmpty(pdfUrl))

                {

                    patch.setPreviewType("pdf");

                    patch.setPreviewUrl(pdfUrl);

                    patch.setConvertStatus("success");

                    patch.setPreviewError("");

                    applyPageCount(patch, pdfUrl);

                }

                else if (attempt < MAX_ATTEMPTS - 1)

                {

                    Thread.sleep(RETRY_DELAY_MS);

                    convertWithRetry(documentId, attempt + 1);

                    return;

                }

                else

                {

                    patch.setPreviewType("pdf");

                    patch.setPreviewUrl(null);

                    patch.setConvertStatus("failed");

                    patch.setPreviewError("\u6587\u6863\u8f6c PDF \u5931\u8d25\uff0c\u8bf7\u786e\u8ba4 LibreOffice \u914d\u7f6e\u540e\u91cd\u8bd5");

                }

                documentMapper.updateEduLibraryDocument(patch);

                applyCoverIfMissing(documentId, document);

                return;

            }

            if (EduLibraryPreviewSupport.isArchiveExt(ext))

            {

                if (!officePdfConverter.isSourceReady(document.getFileUrl()) && attempt < MAX_ATTEMPTS - 1)

                {

                    Thread.sleep(RETRY_DELAY_MS);

                    convertWithRetry(documentId, attempt + 1);

                    return;

                }

                previewSupport.applyPreviewMeta(document);

                EduLibraryDocument patch = new EduLibraryDocument();

                patch.setDocumentId(documentId);

                patch.setPreviewType(document.getPreviewType());

                patch.setPreviewUrl(document.getPreviewUrl());

                if ("failed".equals(document.getConvertStatus()))

                {

                    patch.setConvertStatus("failed");

                    patch.setPreviewError(document.getPreviewError());

                    documentMapper.updateEduLibraryDocument(patch);

                    return;

                }

                EduLibraryArchiveConvertSupport.ConvertResult result =

                    archiveConvertSupport.warmupArchiveInnerFiles(document.getFileUrl());

                if (result.totalOffice == 0 || result.converted > 0)

                {

                    patch.setConvertStatus("success");

                    if (result.hasPartialFailure())

                    {

                        patch.setPreviewError(result.failed + " \u4e2a\u5185\u5d4c\u6587\u6863\u8f6c\u6362\u5931\u8d25\uff0c\u9996\u6b21\u6253\u5f00\u65f6\u53ef\u80fd\u9700\u7a0d\u5019");

                    }

                    else

                    {

                        patch.setPreviewError("");

                    }

                }

                else if (attempt < MAX_ATTEMPTS - 1)

                {

                    Thread.sleep(RETRY_DELAY_MS);

                    convertWithRetry(documentId, attempt + 1);

                    return;

                }

                else

                {

                    patch.setConvertStatus("failed");

                    patch.setPreviewError("\u538b\u7f29\u5305\u5185\u6587\u6863\u8f6c\u6362\u5931\u8d25\uff0c\u8bf7\u786e\u8ba4 kkFileView \u5df2\u542f\u52a8\u540e\u91cd\u8bd5");

                }

                documentMapper.updateEduLibraryDocument(patch);

                return;

            }

            previewSupport.applyPreviewMeta(document);

            EduLibraryDocument patch = new EduLibraryDocument();

            patch.setDocumentId(documentId);

            patch.setConvertStatus(document.getConvertStatus());

            patch.setPreviewType(document.getPreviewType());

            patch.setPreviewUrl(document.getPreviewUrl());

            patch.setPreviewError(document.getPreviewError());

            if ("pdf".equals(ext))

            {

                applyPageCount(patch, StringUtils.isNotEmpty(document.getPreviewUrl())

                        ? document.getPreviewUrl() : document.getFileUrl());

            }

            documentMapper.updateEduLibraryDocument(patch);

            applyCoverIfMissing(documentId, document);

        }

        catch (Exception ex)

        {

            log.warn("Library convert task failed for documentId={}", documentId, ex);

        }

    }



    private void applyCoverIfMissing(Long documentId, EduLibraryDocument document)

    {

        if (document == null || StringUtils.isNotEmpty(document.getCoverUrl()))

        {

            return;

        }

        String coverUrl = coverGenerator.generateCover(document.getFileUrl(), document.getFileExt());

        if (StringUtils.isEmpty(coverUrl))

        {

            return;

        }

        EduLibraryDocument patch = new EduLibraryDocument();

        patch.setDocumentId(documentId);

        patch.setCoverUrl(coverUrl);

        documentMapper.updateEduLibraryDocument(patch);

    }



    private void applyPageCount(EduLibraryDocument patch, String fileUrl)

    {

        Integer pages = officePdfConverter.countPdfPages(fileUrl);

        if (pages != null && pages > 0)

        {

            patch.setPageCount(pages);

        }

    }

}


