package com.ruoyi.system.mapper.education;

import java.util.List;
import com.ruoyi.system.domain.education.EduQbOcrDraft;

public interface EduQbOcrDraftMapper
{
    EduQbOcrDraft selectEduQbOcrDraftById(Long draftId);

    List<EduQbOcrDraft> selectEduQbOcrDraftList(EduQbOcrDraft draft);

    int insertEduQbOcrDraft(EduQbOcrDraft draft);

    int updateEduQbOcrDraft(EduQbOcrDraft draft);
}
