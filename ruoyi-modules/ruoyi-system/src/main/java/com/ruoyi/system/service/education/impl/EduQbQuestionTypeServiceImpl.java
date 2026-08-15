package com.ruoyi.system.service.education.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbQuestionType;
import com.ruoyi.system.mapper.education.EduQbQuestionTypeMapper;
import com.ruoyi.system.service.education.IEduQbQuestionTypeService;

@Service
public class EduQbQuestionTypeServiceImpl implements IEduQbQuestionTypeService
{
    private static final Set<String> ANSWER_MODES = new HashSet<>(
            Arrays.asList("choice", "multi", "judge", "fill", "subjective"));

    @Autowired
    private EduQbQuestionTypeMapper questionTypeMapper;

    @Override
    public EduQbQuestionType selectEduQbQuestionTypeById(Long typeId)
    {
        return questionTypeMapper.selectEduQbQuestionTypeById(typeId);
    }

    @Override
    public EduQbQuestionType selectEnabledByCode(String typeCode)
    {
        if (StringUtils.isEmpty(typeCode))
        {
            return null;
        }
        EduQbQuestionType row = questionTypeMapper.selectEduQbQuestionTypeByCode(typeCode);
        if (row == null || !"0".equals(row.getStatus()))
        {
            return null;
        }
        return row;
    }

    @Override
    public List<EduQbQuestionType> selectEduQbQuestionTypeList(EduQbQuestionType query)
    {
        return questionTypeMapper.selectEduQbQuestionTypeList(query);
    }

    @Override
    public List<EduQbQuestionType> selectEnabledQuestionTypeOptions()
    {
        return questionTypeMapper.selectEnabledQuestionTypeOptions();
    }

    @Override
    public int insertEduQbQuestionType(EduQbQuestionType row)
    {
        validateRow(row, true);
        if (questionTypeMapper.countByTypeCode(row.getTypeCode(), null) > 0)
        {
            throw new ServiceException("\u9898\u578b\u7f16\u7801\u5df2\u5b58\u5728");
        }
        if (row.getOrderNum() == null)
        {
            row.setOrderNum(99);
        }
        if (StringUtils.isEmpty(row.getStatus()))
        {
            row.setStatus("0");
        }
        row.setBuiltin("0");
        return questionTypeMapper.insertEduQbQuestionType(row);
    }

    @Override
    public int updateEduQbQuestionType(EduQbQuestionType row)
    {
        if (row == null || row.getTypeId() == null)
        {
            throw new ServiceException("\u53c2\u6570\u4e0d\u80fd\u4e3a\u7a7a");
        }
        EduQbQuestionType existing = questionTypeMapper.selectEduQbQuestionTypeById(row.getTypeId());
        if (existing == null)
        {
            throw new ServiceException("\u9898\u578b\u4e0d\u5b58\u5728");
        }
        if ("1".equals(existing.getBuiltin()))
        {
            row.setTypeCode(existing.getTypeCode());
            row.setAnswerMode(existing.getAnswerMode());
        }
        validateRow(row, false);
        if (StringUtils.isNotEmpty(row.getTypeCode())
                && questionTypeMapper.countByTypeCode(row.getTypeCode(), row.getTypeId()) > 0)
        {
            throw new ServiceException("\u9898\u578b\u7f16\u7801\u5df2\u5b58\u5728");
        }
        return questionTypeMapper.updateEduQbQuestionType(row);
    }

    @Override
    public int deleteEduQbQuestionTypeByIds(Long[] typeIds)
    {
        if (typeIds == null || typeIds.length == 0)
        {
            return 0;
        }
        for (Long typeId : typeIds)
        {
            EduQbQuestionType row = questionTypeMapper.selectEduQbQuestionTypeById(typeId);
            if (row == null)
            {
                continue;
            }
            if ("1".equals(row.getBuiltin()))
            {
                throw new ServiceException("\u5185\u7f6e\u9898\u578b\u4e0d\u53ef\u5220\u9664\uff1a" + row.getTypeName());
            }
            if (questionTypeMapper.countQuestionsByTypeCode(row.getTypeCode()) > 0)
            {
                throw new ServiceException("\u9898\u578b\u300c" + row.getTypeName() + "\u300d\u5df2\u88ab\u9898\u76ee\u4f7f\u7528\uff0c\u65e0\u6cd5\u5220\u9664");
            }
        }
        return questionTypeMapper.deleteEduQbQuestionTypeByIds(typeIds);
    }

    @Override
    public int resolveMaxContentLength(String typeCode)
    {
        EduQbQuestionType row = selectEnabledByCode(typeCode);
        if (row != null && row.getContentMaxLen() != null && row.getContentMaxLen() > 0)
        {
            return row.getContentMaxLen();
        }
        return EduQbConstants.resolveMaxContentLength(typeCode);
    }

    @Override
    public void assertEnabledType(String typeCode)
    {
        if (StringUtils.isEmpty(typeCode))
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u9898\u578b");
        }
        EduQbQuestionType row = selectEnabledByCode(typeCode);
        if (row == null)
        {
            EduQbQuestionType disabled = questionTypeMapper.selectEduQbQuestionTypeByCode(typeCode);
            if (disabled != null)
            {
                throw new ServiceException("\u9898\u578b\u300c" + disabled.getTypeName() + "\u300d\u5df2\u505c\u7528");
            }
            throw new ServiceException("\u4e0d\u652f\u6301\u7684\u9898\u578b");
        }
    }

    @Override
    public String resolveTypeLabel(String typeCode)
    {
        if (StringUtils.isEmpty(typeCode))
        {
            return "";
        }
        EduQbQuestionType row = questionTypeMapper.selectEduQbQuestionTypeByCode(typeCode);
        return row != null ? row.getTypeName() : typeCode;
    }

    @Override
    public int resolveTypeSortIndex(String typeCode)
    {
        if (StringUtils.isEmpty(typeCode))
        {
            return 9999;
        }
        List<EduQbQuestionType> options = selectEnabledQuestionTypeOptions();
        for (int i = 0; i < options.size(); i++)
        {
            if (typeCode.equals(options.get(i).getTypeCode()))
            {
                return i;
            }
        }
        EduQbQuestionType row = questionTypeMapper.selectEduQbQuestionTypeByCode(typeCode);
        if (row != null && row.getOrderNum() != null)
        {
            return 1000 + row.getOrderNum();
        }
        return 9999;
    }

    @Override
    public List<String> selectEnabledTypeCodesInOrder()
    {
        List<EduQbQuestionType> options = selectEnabledQuestionTypeOptions();
        List<String> codes = new ArrayList<>();
        for (EduQbQuestionType row : options)
        {
            codes.add(row.getTypeCode());
        }
        return codes;
    }

    private void validateRow(EduQbQuestionType row, boolean creating)
    {
        if (row == null)
        {
            throw new ServiceException("\u53c2\u6570\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (creating && StringUtils.isEmpty(row.getTypeCode()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u9898\u578b\u7f16\u7801");
        }
        if (StringUtils.isNotEmpty(row.getTypeCode()) && !row.getTypeCode().matches("^[a-z][a-z0-9_]{0,31}$"))
        {
            throw new ServiceException("\u9898\u578b\u7f16\u7801\u4ec5\u652f\u6301\u5c0f\u5199\u5b57\u6bcd\u3001\u6570\u5b57\u4e0e\u4e0b\u5212\u7ebf\uff0c\u4e14\u4ee5\u5b57\u6bcd\u5f00\u5934");
        }
        if (StringUtils.isEmpty(row.getTypeName()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u9898\u578b\u540d\u79f0");
        }
        if (creating && StringUtils.isEmpty(row.getAnswerMode()))
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u7b54\u9898\u6a21\u5f0f");
        }
        if (StringUtils.isNotEmpty(row.getAnswerMode()) && !ANSWER_MODES.contains(row.getAnswerMode()))
        {
            throw new ServiceException("\u7b54\u9898\u6a21\u5f0f\u4e0d\u5408\u6cd5");
        }
        if (row.getContentMaxLen() != null && (row.getContentMaxLen() < 100 || row.getContentMaxLen() > 50000))
        {
            throw new ServiceException("\u9898\u5e72\u957f\u5ea6\u4e0a\u9650\u9700\u5728 100~50000 \u4e4b\u95f4");
        }
    }
}
