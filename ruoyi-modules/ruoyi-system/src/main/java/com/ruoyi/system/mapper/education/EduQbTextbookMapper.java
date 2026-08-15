package com.ruoyi.system.mapper.education;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduQbCatalogChapter;
import com.ruoyi.system.domain.education.EduQbTextbook;
import com.ruoyi.system.domain.education.EduQbTextbookVersion;

public interface EduQbTextbookMapper
{
    List<EduQbTextbookVersion> selectVersionsBySubjectId(@Param("subjectId") Long subjectId,
            @Param("schoolStage") String schoolStage);

    List<EduQbTextbookVersion> selectVersionsAdminBySubjectId(@Param("subjectId") Long subjectId,
            @Param("schoolStage") String schoolStage);

    EduQbTextbookVersion selectVersionById(@Param("versionId") Long versionId);

    int insertVersion(EduQbTextbookVersion version);

    int updateVersion(EduQbTextbookVersion version);

    int deleteVersionByIds(@Param("versionIds") Long[] versionIds);

    int countTextbookByVersionId(@Param("versionId") Long versionId);

    List<EduQbTextbook> selectTextbooksByVersionId(@Param("versionId") Long versionId);

    List<EduQbTextbook> selectTextbooksAdminByVersionId(@Param("versionId") Long versionId);

    EduQbTextbook selectTextbookById(@Param("textbookId") Long textbookId);

    int insertTextbook(EduQbTextbook textbook);

    int updateTextbook(EduQbTextbook textbook);

    int deleteTextbookByIds(@Param("textbookIds") Long[] textbookIds);

    int countChapterByTextbookId(@Param("textbookId") Long textbookId);

    List<EduQbCatalogChapter> selectChaptersByTextbookId(@Param("textbookId") Long textbookId,
            @Param("subjectId") Long subjectId);

    List<EduQbCatalogChapter> selectChapterListByTextbookId(@Param("textbookId") Long textbookId);

    EduQbCatalogChapter selectChapterById(@Param("chapterId") Long chapterId);

    List<EduQbCatalogChapter> selectChapterSubtree(@Param("chapterId") Long chapterId);

    int insertChapter(EduQbCatalogChapter chapter);

    int updateChapter(EduQbCatalogChapter chapter);

    int deleteChapterByIds(@Param("chapterIds") Long[] chapterIds);

    int countChildChapter(@Param("parentId") Long parentId);

    int countQuestionByChapterId(@Param("chapterId") Long chapterId);

    int countQuestionsDirectInChapter(@Param("chapterId") Long chapterId, @Param("subjectId") Long subjectId);

    int countQuestionsInChapterSubtree(@Param("chapterId") Long chapterId, @Param("subjectId") Long subjectId);

    int countQuestionsByTextbookId(@Param("textbookId") Long textbookId, @Param("subjectId") Long subjectId);

    List<java.util.Map<String, Object>> selectDirectQuestionCountsByTextbook(@Param("textbookId") Long textbookId,
            @Param("subjectId") Long subjectId);
}
