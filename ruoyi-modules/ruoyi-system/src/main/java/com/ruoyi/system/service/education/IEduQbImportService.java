package com.ruoyi.system.service.education;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.system.domain.education.EduQbImportBlock;
import com.ruoyi.system.domain.education.EduQbImportCommitBody;
import com.ruoyi.system.domain.education.EduQbImportTask;

public interface IEduQbImportService
{
    Map<String, Object> uploadAndParse(MultipartFile file, Long subjectId, String operator);

    List<EduQbImportTask> selectImportTaskList(EduQbImportTask query, String operator);

    EduQbImportTask getTask(Long taskId, String operator);

    List<EduQbImportBlock> getBlocks(Long taskId, String operator);

    String getPreviewHtml(Long taskId, String operator);

    int commitImport(EduQbImportCommitBody body, String operator);
}
