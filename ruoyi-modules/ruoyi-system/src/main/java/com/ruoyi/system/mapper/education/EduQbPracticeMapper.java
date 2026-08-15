package com.ruoyi.system.mapper.education;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduQbPracticeRecord;
import com.ruoyi.system.domain.education.EduQbPracticeSession;
import com.ruoyi.system.domain.education.EduQbPracticeStats;
import com.ruoyi.system.domain.education.EduQbWeakPointStat;

public interface EduQbPracticeMapper
{
    int insertPracticeSession(EduQbPracticeSession session);

    int insertPracticeRecord(EduQbPracticeRecord record);

    List<EduQbPracticeSession> selectPracticeSessionList(EduQbPracticeSession query);

    EduQbPracticeSession selectPracticeSessionById(@Param("sessionId") Long sessionId,
            @Param("userName") String userName);

    List<EduQbPracticeRecord> selectPracticeRecordBySessionId(@Param("sessionId") Long sessionId);

    List<EduQbWeakPointStat> selectWeakPointStats(@Param("userName") String userName,
            @Param("subjectId") Long subjectId, @Param("limit") int limit);

    EduQbPracticeStats selectPracticeStats(@Param("userName") String userName,
            @Param("subjectId") Long subjectId);
}
