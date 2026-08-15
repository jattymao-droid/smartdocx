package com.ruoyi.system.service.education;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.domain.education.EduLibraryAuditBody;
import com.ruoyi.system.domain.education.EduLibraryDocument;
import com.ruoyi.system.domain.education.EduLibraryRecommendBody;
import com.ruoyi.system.domain.education.EduLibraryStatusBody;
import java.math.BigDecimal;

public interface IEduLibraryDocumentService
{
    EduLibraryDocument selectEduLibraryDocumentById(Long documentId, String viewer);

    List<EduLibraryDocument> selectEduLibraryDocumentList(EduLibraryDocument query, boolean portalMode, String viewer);

    List<EduLibraryDocument> selectMineList(EduLibraryDocument query, String username);

    List<EduLibraryDocument> selectFavoriteList(String username);

    int insertEduLibraryDocument(EduLibraryDocument document, String operator);

    int insertEduLibraryDocument(EduLibraryDocument document, String operator, boolean portalUpload);

    int updateEduLibraryDocument(EduLibraryDocument document, String operator, boolean adminBypass);

    int deleteEduLibraryDocumentByIds(Long[] documentIds, String operator, boolean adminBypass);

    int auditDocuments(EduLibraryAuditBody body, String operator);

    int recommendDocuments(EduLibraryRecommendBody body, String operator);

    int changeDocumentStatus(EduLibraryStatusBody body, String operator);

    int reconvertDocument(Long documentId, String operator);

    Map<String, Object> buildPreviewPayload(Long documentId, String viewer);

    void streamPreviewContent(Long documentId, String viewer, HttpServletResponse response);

    void applyPortalDownloadProtection(EduLibraryDocument document, String viewer);

    void sanitizePortalDocuments(List<EduLibraryDocument> documents, String viewer);

    String resolveDownloadUrl(Long documentId, String viewer);

    void recordView(Long documentId, String viewer);

    int addFavorite(Long documentId, String username);

    int removeFavorite(Long documentId, String username);

    List<EduLibraryDocument> selectRelatedDocuments(Long documentId, String viewer);

    List<EduLibraryDocument> selectContinueReadingList(String username, int limit);

    void saveReadProgress(Long documentId, String username, BigDecimal readProgress);

    boolean canView(EduLibraryDocument document, String viewer, boolean loggedIn);
}
