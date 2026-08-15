package com.ruoyi.system.service.education;

import java.util.List;
import com.ruoyi.system.domain.education.EduQbPaper;
import com.ruoyi.system.domain.education.EduQbPaperDetailResult;
import com.ruoyi.system.domain.education.EduQbPaperSaveRequest;

public interface IEduQbPaperArchiveService
{
    List<EduQbPaper> selectMyPaperList(EduQbPaper query, String username);

    EduQbPaperDetailResult selectMyPaperDetail(Long paperId, String username);

    Long saveMyPaper(EduQbPaperSaveRequest request, String username);

    int deleteMyPaper(Long paperId, String username);
}
