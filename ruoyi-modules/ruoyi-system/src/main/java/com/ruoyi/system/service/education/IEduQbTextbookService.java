package com.ruoyi.system.service.education;

import java.util.List;
import com.ruoyi.system.domain.education.EduQbCatalogChapter;
import com.ruoyi.system.domain.education.EduQbChapterTreeNode;
import com.ruoyi.system.domain.education.EduQbTextbook;
import com.ruoyi.system.domain.education.EduQbTextbookVersion;

public interface IEduQbTextbookService
{
    List<EduQbTextbookVersion> selectVersions(Long subjectId, String schoolStage);

    List<EduQbTextbookVersion> selectVersionsAdmin(Long subjectId, String schoolStage);

    EduQbTextbookVersion selectVersionById(Long versionId);

    int insertVersion(EduQbTextbookVersion version);

    int updateVersion(EduQbTextbookVersion version);

    int deleteVersionByIds(Long[] versionIds);

    List<EduQbTextbook> selectTextbooks(Long versionId);

    List<EduQbTextbook> selectTextbooksAdmin(Long versionId);

    EduQbTextbook selectTextbookById(Long textbookId);

    int insertTextbook(EduQbTextbook textbook);

    int updateTextbook(EduQbTextbook textbook);

    int deleteTextbookByIds(Long[] textbookIds);

    List<EduQbChapterTreeNode> selectChapterTree(Long textbookId, Long subjectId);

    List<EduQbCatalogChapter> selectChapterList(Long textbookId);

    EduQbCatalogChapter selectChapterById(Long chapterId);

    int insertChapter(EduQbCatalogChapter chapter);

    int updateChapter(EduQbCatalogChapter chapter);

    int deleteChapterByIds(Long[] chapterIds);
}
