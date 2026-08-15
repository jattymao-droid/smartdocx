package com.ruoyi.system.service.education.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduLibraryCategory;
import com.ruoyi.system.mapper.education.EduLibraryCategoryMapper;
import com.ruoyi.system.service.education.IEduLibraryCategoryService;

@Service
public class EduLibraryCategoryServiceImpl implements IEduLibraryCategoryService
{
    @Autowired
    private EduLibraryCategoryMapper categoryMapper;

    @Override
    public EduLibraryCategory selectEduLibraryCategoryById(Long categoryId)
    {
        return categoryMapper.selectEduLibraryCategoryById(categoryId);
    }

    @Override
    public List<EduLibraryCategory> selectEduLibraryCategoryList(EduLibraryCategory query)
    {
        return categoryMapper.selectEduLibraryCategoryList(query);
    }

    @Override
    public int insertEduLibraryCategory(EduLibraryCategory category)
    {
        validateCategory(category, true);
        if (category.getOrderNum() == null)
        {
            category.setOrderNum(0);
        }
        if (StringUtils.isEmpty(category.getStatus()))
        {
            category.setStatus("0");
        }
        return categoryMapper.insertEduLibraryCategory(category);
    }

    @Override
    public int updateEduLibraryCategory(EduLibraryCategory category)
    {
        if (category.getCategoryId() == null)
        {
            throw new ServiceException("Category id is required");
        }
        validateCategory(category, false);
        return categoryMapper.updateEduLibraryCategory(category);
    }

    @Override
    public int deleteEduLibraryCategoryByIds(Long[] categoryIds)
    {
        if (categoryIds == null || categoryIds.length == 0)
        {
            return 0;
        }
        for (Long categoryId : categoryIds)
        {
            if (categoryMapper.countDocumentsByCategoryId(categoryId) > 0)
            {
                EduLibraryCategory cat = categoryMapper.selectEduLibraryCategoryById(categoryId);
                String name = cat != null ? cat.getCategoryName() : String.valueOf(categoryId);
                throw new ServiceException("Category [" + name + "] has documents, cannot delete");
            }
        }
        return categoryMapper.deleteEduLibraryCategoryByIds(categoryIds);
    }

    private void validateCategory(EduLibraryCategory category, boolean creating)
    {
        if (category == null)
        {
            throw new ServiceException("Category is required");
        }
        if (creating && StringUtils.isEmpty(category.getCategoryName()))
        {
            throw new ServiceException("Category name is required");
        }
        if (StringUtils.isNotEmpty(category.getCategoryName()) && category.getCategoryName().length() > 100)
        {
            throw new ServiceException("Category name too long");
        }
    }
}
