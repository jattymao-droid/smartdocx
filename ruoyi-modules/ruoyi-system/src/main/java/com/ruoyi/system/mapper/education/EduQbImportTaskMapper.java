package com.ruoyi.system.mapper.education;

import java.util.List;
import com.ruoyi.system.domain.education.EduQbImportTask;

public interface EduQbImportTaskMapper
{
    EduQbImportTask selectEduQbImportTaskById(Long taskId);

    List<EduQbImportTask> selectEduQbImportTaskList(EduQbImportTask task);

    int insertEduQbImportTask(EduQbImportTask task);

    int updateEduQbImportTask(EduQbImportTask task);
}
