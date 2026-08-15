package com.ruoyi.system.mapper.education;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduQbQuestionType;

public interface EduQbQuestionTypeMapper
{
    EduQbQuestionType selectEduQbQuestionTypeById(Long typeId);

    EduQbQuestionType selectEduQbQuestionTypeByCode(@Param("typeCode") String typeCode);

    List<EduQbQuestionType> selectEduQbQuestionTypeList(EduQbQuestionType query);

    List<EduQbQuestionType> selectEnabledQuestionTypeOptions();

    int insertEduQbQuestionType(EduQbQuestionType row);

    int updateEduQbQuestionType(EduQbQuestionType row);

    int deleteEduQbQuestionTypeById(Long typeId);

    int deleteEduQbQuestionTypeByIds(@Param("typeIds") Long[] typeIds);

    int countByTypeCode(@Param("typeCode") String typeCode, @Param("excludeId") Long excludeId);

    int countQuestionsByTypeCode(@Param("typeCode") String typeCode);
}
