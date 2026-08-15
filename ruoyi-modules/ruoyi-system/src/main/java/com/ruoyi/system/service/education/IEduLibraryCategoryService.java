package com.ruoyi.system.service.education;

import java.util.List;
import com.ruoyi.system.domain.education.EduLibraryCategory;

public interface IEduLibraryCategoryService
{
    EduLibraryCategory selectEduLibraryCategoryById(Long categoryId);

    List<EduLibraryCategory> selectEduLibraryCategoryList(EduLibraryCategory query);

    int insertEduLibraryCategory(EduLibraryCategory category);

    int updateEduLibraryCategory(EduLibraryCategory category);

    int deleteEduLibraryCategoryByIds(Long[] categoryIds);
}
