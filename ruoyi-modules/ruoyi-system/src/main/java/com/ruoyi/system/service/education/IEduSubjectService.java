package com.ruoyi.system.service.education;

import java.util.List;
import com.ruoyi.system.domain.education.EduSubject;

public interface IEduSubjectService
{
    EduSubject selectEduSubjectById(Long subjectId);

    List<EduSubject> selectEduSubjectList(EduSubject subject);

    int insertEduSubject(EduSubject subject);

    int updateEduSubject(EduSubject subject);

    int deleteEduSubjectByIds(Long[] subjectIds);

    List<EduSubject> selectSubjectOptions();
}
