package com.ruoyi.system.service.education;

import java.util.List;
import com.ruoyi.system.domain.education.EduQbChapterTreeNode;
import com.ruoyi.system.domain.education.EduQbKnowledgeTag;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.domain.education.EduQbQuestionAuditBody;
import com.ruoyi.system.domain.education.EduQbDuplicateCheckBody;
import com.ruoyi.system.domain.education.EduQbDuplicateCheckResult;
import com.ruoyi.system.domain.education.EduQbQuestionFeedbackBody;

public interface IEduQbQuestionService
{
    EduQbQuestion selectEduQbQuestionById(Long questionId);

    List<EduQbQuestion> selectEduQbQuestionList(EduQbQuestion question);

    int insertEduQbQuestion(EduQbQuestion question, String operator);

    int updateEduQbQuestion(EduQbQuestion question, String operator);

    int deleteEduQbQuestionByIds(Long[] questionIds, String operator);

    List<EduQbKnowledgeTag> selectKnowledgeTags(Long subjectId, String keyword);

    List<EduQbChapterTreeNode> selectKnowledgeTree(Long textbookId, Long subjectId, String keyword);

    /**
     * @deprecated Use {@link IEduQbTextbookService#selectChapterTree(Long, Long)} instead.
     */
    @Deprecated
    List<EduQbChapterTreeNode> selectChapterTree(Long subjectId);

    boolean canManage(EduQbQuestion question, String operator);

    int auditQuestions(EduQbQuestionAuditBody body, String operator);

    int countPendingQuestions();

    EduQbDuplicateCheckResult checkDuplicates(EduQbDuplicateCheckBody body);

    int submitQuestionFeedback(EduQbQuestionFeedbackBody body, String operator);
}
