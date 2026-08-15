package com.ruoyi.system.mapper.education;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduSubject;

public interface EduSubjectMapper
{
    EduSubject selectEduSubjectById(Long subjectId);

    List<EduSubject> selectEduSubjectList(EduSubject subject);

    int insertEduSubject(EduSubject subject);

    int updateEduSubject(EduSubject subject);

    int deleteEduSubjectById(Long subjectId);

    int deleteEduSubjectByIds(@Param("subjectIds") Long[] subjectIds);

    String selectSubjectNameById(Long subjectId);

    List<String> selectSubjectNamesByIds(@Param("subjectIds") Long[] subjectIds);

    int countBySubjectName(@Param("subjectName") String subjectName, @Param("excludeId") Long excludeId);

    List<String> selectSubjectOptions();
}
