package com.ruoyi.system.mapper.education;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduLibraryCategory;

public interface EduLibraryCategoryMapper
{
    EduLibraryCategory selectEduLibraryCategoryById(Long categoryId);

    List<EduLibraryCategory> selectEduLibraryCategoryList(EduLibraryCategory query);

    int insertEduLibraryCategory(EduLibraryCategory category);

    int updateEduLibraryCategory(EduLibraryCategory category);

    int deleteEduLibraryCategoryByIds(@Param("categoryIds") Long[] categoryIds);

    int countDocumentsByCategoryId(@Param("categoryId") Long categoryId);
}
