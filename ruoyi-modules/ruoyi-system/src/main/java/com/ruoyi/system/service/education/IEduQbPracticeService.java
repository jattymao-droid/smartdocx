package com.ruoyi.system.service.education;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.education.EduQbPracticeSession;
import com.ruoyi.system.domain.education.EduQbPracticeStats;
import com.ruoyi.system.domain.education.EduQbPracticeSubmitBody;
import com.ruoyi.system.domain.education.EduQbSmartComposeResult;
import com.ruoyi.system.domain.education.EduQbWeakComposeRequest;
import com.ruoyi.system.domain.education.EduQbWeakPointStat;
import com.ruoyi.system.domain.education.EduQbWrongBook;
import com.ruoyi.system.domain.education.EduQbWrongBookBatchBody;
import com.ruoyi.system.domain.education.EduQbWrongBookStats;
import com.ruoyi.system.domain.education.EduQbWrongComposeRequest;
import com.ruoyi.system.domain.education.EduQbStudentPracticeCheckBody;
import com.ruoyi.system.domain.education.EduQbStudentPracticeCheckResult;

public interface IEduQbPracticeService
{
    Long submitPractice(EduQbPracticeSubmitBody body, String userName);

    List<EduQbPracticeSession> selectPracticeSessionList(EduQbPracticeSession query, String userName);

    EduQbPracticeStats selectPracticeStats(Long subjectId, String userName);

    Map<String, Object> getPracticeSessionDetail(Long sessionId, String userName);

    List<EduQbWrongBook> selectWrongBookList(EduQbWrongBook query, String userName);

    EduQbWrongBookStats selectWrongBookStats(Long subjectId, String userName);

    int markWrongMastered(Long wrongId, String userName);

    int restoreWrong(Long wrongId, String userName);

    int deleteWrongBook(Long wrongId, String userName);

    int batchMarkWrongMastered(EduQbWrongBookBatchBody body, String userName);

    int batchDeleteWrongBook(EduQbWrongBookBatchBody body, String userName);

    List<EduQbWeakPointStat> selectWeakPointStats(Long subjectId, int limit, String userName);

    EduQbSmartComposeResult weakCompose(EduQbWeakComposeRequest request, String userName);

    EduQbSmartComposeResult wrongCompose(EduQbWrongComposeRequest request, String userName);

    EduQbStudentPracticeCheckResult checkPracticeAnswer(EduQbStudentPracticeCheckBody body);
}
