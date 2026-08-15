package com.ruoyi.system.mapper.education;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduLibraryCategory;
import com.ruoyi.system.domain.education.EduLibraryDocument;

public interface EduLibraryDocumentMapper
{
    EduLibraryDocument selectEduLibraryDocumentById(Long documentId);

    List<EduLibraryDocument> selectEduLibraryDocumentList(EduLibraryDocument query);

    int insertEduLibraryDocument(EduLibraryDocument document);

    int updateEduLibraryDocument(EduLibraryDocument document);

    int deleteEduLibraryDocumentByIds(@Param("documentIds") Long[] documentIds);

    int incrementViewCount(@Param("documentId") Long documentId);

    int incrementDownloadCount(@Param("documentId") Long documentId);

    int updateFavoriteCount(@Param("documentId") Long documentId, @Param("delta") int delta);

    String selectMaxDocumentCodeByPrefix(@Param("prefix") String prefix);

    int insertFavorite(@Param("documentId") Long documentId, @Param("userName") String userName);

    int deleteFavorite(@Param("documentId") Long documentId, @Param("userName") String userName);

    int countFavorite(@Param("documentId") Long documentId, @Param("userName") String userName);

    List<EduLibraryDocument> selectFavoriteList(@Param("userName") String userName);

    List<EduLibraryDocument> selectRelatedDocuments(@Param("documentId") Long documentId,
            @Param("subjectId") Long subjectId, @Param("categoryId") Long categoryId,
            @Param("currentUser") String currentUser, @Param("loggedIn") boolean loggedIn,
            @Param("limit") int limit);

    List<EduLibraryDocument> selectContinueReadingList(@Param("userName") String userName, @Param("limit") int limit);

    int upsertReadLog(@Param("documentId") Long documentId, @Param("userName") String userName,
            @Param("readProgress") java.math.BigDecimal readProgress);

    Map<String, Object> selectLibraryAdminStats();
}
