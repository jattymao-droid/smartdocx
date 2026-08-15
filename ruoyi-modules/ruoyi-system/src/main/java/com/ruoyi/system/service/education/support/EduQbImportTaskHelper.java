package com.ruoyi.system.service.education.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.education.EduQbImportTask;
import com.ruoyi.system.mapper.education.EduQbImportTaskMapper;

@Component
public class EduQbImportTaskHelper
{
    @Autowired
    private EduQbImportTaskMapper importTaskMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(Long taskId)
    {
        if (taskId == null)
        {
            return;
        }
        EduQbImportTask task = importTaskMapper.selectEduQbImportTaskById(taskId);
        if (task == null || EduQbImportTask.STATUS_DONE.equals(task.getStatus()))
        {
            return;
        }
        task.setStatus(EduQbImportTask.STATUS_FAILED);
        importTaskMapper.updateEduQbImportTask(task);
    }
}
