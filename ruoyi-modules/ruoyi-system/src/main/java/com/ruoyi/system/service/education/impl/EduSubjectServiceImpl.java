package com.ruoyi.system.service.education.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduSubject;
import com.ruoyi.system.mapper.education.EduSubjectMapper;
import com.ruoyi.system.service.education.IEduSubjectService;

@Service
public class EduSubjectServiceImpl implements IEduSubjectService
{
    @Autowired
    private EduSubjectMapper subjectMapper;

    @Override
    public EduSubject selectEduSubjectById(Long subjectId)
    {
        return subjectMapper.selectEduSubjectById(subjectId);
    }

    @Override
    public List<EduSubject> selectEduSubjectList(EduSubject subject)
    {
        return subjectMapper.selectEduSubjectList(subject);
    }

    @Override
    public int insertEduSubject(EduSubject subject)
    {
        if (subject != null && StringUtils.isNotEmpty(subject.getSubjectName()))
        {
            int count = subjectMapper.countBySubjectName(subject.getSubjectName(), null);
            if (count > 0)
            {
                throw new ServiceException("学科名称已存在");
            }
        }
        return subjectMapper.insertEduSubject(subject);
    }

    @Override
    public int updateEduSubject(EduSubject subject)
    {
        if (subject != null && StringUtils.isNotEmpty(subject.getSubjectName()))
        {
            int count = subjectMapper.countBySubjectName(subject.getSubjectName(), subject.getSubjectId());
            if (count > 0)
            {
                throw new ServiceException("学科名称已存在");
            }
        }
        return subjectMapper.updateEduSubject(subject);
    }

    @Override
    public int deleteEduSubjectByIds(Long[] subjectIds)
    {
        return subjectMapper.deleteEduSubjectByIds(subjectIds);
    }

    @Override
    public List<EduSubject> selectSubjectOptions()
    {
        EduSubject query = new EduSubject();
        query.setStatus("0");
        return subjectMapper.selectEduSubjectList(query);
    }
}
