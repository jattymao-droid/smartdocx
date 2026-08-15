package com.ruoyi.system.service.education;

import java.util.List;
import com.ruoyi.system.domain.education.EduQbQuestionType;

public interface IEduQbQuestionTypeService
{
    EduQbQuestionType selectEduQbQuestionTypeById(Long typeId);

    EduQbQuestionType selectEnabledByCode(String typeCode);

    List<EduQbQuestionType> selectEduQbQuestionTypeList(EduQbQuestionType query);

    List<EduQbQuestionType> selectEnabledQuestionTypeOptions();

    int insertEduQbQuestionType(EduQbQuestionType row);

    int updateEduQbQuestionType(EduQbQuestionType row);

    int deleteEduQbQuestionTypeByIds(Long[] typeIds);

    int resolveMaxContentLength(String typeCode);

    void assertEnabledType(String typeCode);

    String resolveTypeLabel(String typeCode);

    int resolveTypeSortIndex(String typeCode);

    List<String> selectEnabledTypeCodesInOrder();
}
