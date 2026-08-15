package com.ruoyi.system.mapper.education;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduQbKnowledgeTag;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.domain.education.EduQbQuestionFeedbackBody;

public interface EduQbQuestionMapper
{
    EduQbQuestion selectEduQbQuestionById(Long questionId);

    List<EduQbQuestion> selectEduQbQuestionByIds(Long[] questionIds);

    List<EduQbQuestion> selectEduQbQuestionList(EduQbQuestion question);

    int insertEduQbQuestion(EduQbQuestion question);

    int updateEduQbQuestion(EduQbQuestion question);

    int deleteEduQbQuestionByIds(Long[] questionIds);

    String selectMaxQuestionCodeByPrefix(@Param("prefix") String prefix);

    List<EduQbKnowledgeTag> selectKnowledgeTags(@Param("subjectId") Long subjectId, @Param("keyword") String keyword);

    List<java.util.Map<String, Object>> selectChapterKnowledgeTagStats(@Param("subjectId") Long subjectId,
            @Param("textbookId") Long textbookId, @Param("keyword") String keyword);

    List<java.util.Map<String, Object>> selectChapterTextStats(@Param("subjectId") Long subjectId);

    int upsertKnowledgeTag(@Param("subjectId") Long subjectId, @Param("tagName") String tagName);

    int decrementKnowledgeTag(@Param("subjectId") Long subjectId, @Param("tagName") String tagName);

    int updateQuestionAuditStatus(@Param("questionIds") Long[] questionIds, @Param("status") String status,
            @Param("remark") String remark, @Param("updateBy") String updateBy);

    int countQuestionsByStatus(@Param("status") String status);

    List<EduQbQuestion> selectByContentHash(@Param("subjectId") Long subjectId,
            @Param("contentHash") String contentHash, @Param("excludeQuestionId") Long excludeQuestionId);

    List<EduQbQuestion> selectDedupCandidates(@Param("subjectId") Long subjectId,
            @Param("excludeQuestionId") Long excludeQuestionId, @Param("limit") int limit);

    int insertEduQbQuestionFeedback(@Param("body") EduQbQuestionFeedbackBody body, @Param("createBy") String createBy);
}
