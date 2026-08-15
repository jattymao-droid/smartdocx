package com.ruoyi.system.service.education;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.system.domain.education.EduQbExamPaperCommitRequest;
import com.ruoyi.system.domain.education.EduQbExamPaperDetailResult;
import com.ruoyi.system.domain.education.EduQbExamPaperMarkItem;
import com.ruoyi.system.domain.education.EduQbImportBlock;
import com.ruoyi.system.domain.education.EduQbPaper;
import com.ruoyi.system.domain.education.EduQbSchoolPaperPublishRequest;

public interface IEduQbExamPaperService
{
    Map<String, Object> uploadAndParse(MultipartFile file, Long subjectId, String operator);

    List<EduQbExamPaperMarkItem> analyzeBlocks(List<EduQbImportBlock> blocks, Long subjectId);

    Long commitExamPaper(EduQbExamPaperCommitRequest request, String operator);

    List<EduQbPaper> selectExamPaperList(EduQbPaper query);

    EduQbExamPaperDetailResult selectExamPaperDetail(Long paperId, boolean portalView);

    int deleteExamPaper(Long paperId);

    int updatePublishStatus(Long paperId, String publishStatus);

    Long publishSchoolExamPaper(EduQbSchoolPaperPublishRequest request, String operator);
}
