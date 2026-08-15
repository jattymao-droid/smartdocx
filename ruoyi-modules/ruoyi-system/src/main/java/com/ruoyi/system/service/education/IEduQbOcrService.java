package com.ruoyi.system.service.education;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.system.domain.education.EduQbOcrCommitBody;
import com.ruoyi.system.domain.education.EduQbOcrDraft;
import com.ruoyi.system.domain.education.EduSubject;

public interface IEduQbOcrService
{
    Map<String, Object> recognize(MultipartFile file, Long subjectId, String operator);

    List<EduQbOcrDraft> selectOcrDraftList(EduQbOcrDraft query, String operator);

    EduQbOcrDraft getDraft(Long draftId, String operator);

    Map<String, Object> getDraftDetail(Long draftId, String operator);

    Long commit(EduQbOcrCommitBody body, String operator);

    void saveDraftFigure(Long draftId, String figurePath, String operator);

    List<EduSubject> listSubjects();
}
