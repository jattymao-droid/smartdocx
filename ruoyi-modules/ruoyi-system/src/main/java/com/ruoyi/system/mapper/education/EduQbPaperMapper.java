package com.ruoyi.system.mapper.education;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduQbPaper;
import com.ruoyi.system.domain.education.EduQbPaperItem;
import com.ruoyi.system.domain.education.EduQbExamPaperQuestionView;

public interface EduQbPaperMapper
{
    List<EduQbPaper> selectEduQbPaperList(EduQbPaper paper);

    EduQbPaper selectEduQbPaperById(@Param("paperId") Long paperId);

    List<EduQbPaperItem> selectEduQbPaperItemsByPaperId(@Param("paperId") Long paperId);

    List<EduQbExamPaperQuestionView> selectExamPaperQuestions(@Param("paperId") Long paperId);

    int insertEduQbPaper(EduQbPaper paper);

    int updateEduQbPaper(EduQbPaper paper);

    int deleteEduQbPaperById(@Param("paperId") Long paperId);

    int deleteEduQbPaperItemsByPaperId(@Param("paperId") Long paperId);

    int batchInsertEduQbPaperItems(@Param("items") List<EduQbPaperItem> items);
}
