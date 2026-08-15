package com.ruoyi.system.service.education;

import com.ruoyi.system.domain.education.EduQbPaperPreviewRequest;
import com.ruoyi.system.domain.education.EduQbPaperPreviewResult;
import com.ruoyi.system.domain.education.EduQbPaperExportResult;
import com.ruoyi.system.domain.education.EduQbSmartComposeRequest;
import com.ruoyi.system.domain.education.EduQbSmartComposeResult;

public interface IEduQbPaperService
{
    EduQbPaperPreviewResult previewPaper(EduQbPaperPreviewRequest request);

    EduQbPaperExportResult exportPdf(EduQbPaperPreviewRequest request);

    EduQbPaperExportResult exportHtml(EduQbPaperPreviewRequest request);

    EduQbPaperExportResult exportDocx(EduQbPaperPreviewRequest request);

    EduQbSmartComposeResult smartCompose(EduQbSmartComposeRequest request);
}
