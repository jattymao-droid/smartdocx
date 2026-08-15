package com.ruoyi.system.service.education.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbPaper;
import com.ruoyi.system.domain.education.EduQbPaperDetailResult;
import com.ruoyi.system.domain.education.EduQbPaperItem;
import com.ruoyi.system.domain.education.EduQbPaperItemRequest;
import com.ruoyi.system.domain.education.EduQbPaperSaveRequest;
import com.ruoyi.system.mapper.education.EduQbPaperMapper;
import com.ruoyi.system.service.education.IEduQbPaperArchiveService;

@Service
public class EduQbPaperArchiveServiceImpl implements IEduQbPaperArchiveService
{
    @Autowired
    private EduQbPaperMapper paperMapper;

    @Override
    public List<EduQbPaper> selectMyPaperList(EduQbPaper query, String username)
    {
        if (StringUtils.isEmpty(username))
        {
            return new ArrayList<>();
        }
        query.setCreateBy(username);
        query.setPaperType(EduQbConstants.PAPER_TYPE_USER);
        return paperMapper.selectEduQbPaperList(query);
    }

    @Override
    public EduQbPaperDetailResult selectMyPaperDetail(Long paperId, String username)
    {
        EduQbPaper paper = requireOwnedPaper(paperId, username);
        JSONObject config = parseConfig(paper.getExportConfig());
        EduQbPaperDetailResult result = new EduQbPaperDetailResult();
        result.setPaperId(paper.getPaperId());
        result.setPaperTitle(paper.getPaperTitle());
        result.setTemplateCode(paper.getTemplateCode());
        result.setSortMode(paper.getSortRule());
        result.setTotalScore(paper.getTotalScore());
        result.setItemCount(paper.getItemCount());
        if (paper.getCreateTime() != null)
        {
            result.setCreateTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, paper.getCreateTime()));
        }
        if (config != null)
        {
            result.setExportMode(config.getString("exportMode"));
            result.setAnswerLayout(config.getString("answerLayout"));
            result.setGroupTab(config.getString("groupTab"));
            result.setOrderRadio(config.getString("orderRadio"));
            result.setPaperTemplate(config.getString("paperTemplate"));
            result.setExportFormat(config.getString("exportFormat"));
            result.setPageLayout(config.getString("pageLayout"));
            result.setHeader(config.getObject("header", Map.class));
            result.setAnswerAreas(config.getObject("answerAreas", Map.class));
            result.setBasketItems(config.get("basketItems"));
            Object formExportConfig = config.get("formExportConfig");
            if (formExportConfig instanceof Map)
            {
                result.setExportConfig((Map<String, Object>) formExportConfig);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveMyPaper(EduQbPaperSaveRequest request, String username)
    {
        if (request == null || StringUtils.isEmpty(username))
        {
            throw new ServiceException("\u4fdd\u5b58\u5931\u8d25\uff1a\u672a\u767b\u5f55");
        }
        if (StringUtils.isEmpty(request.getPaperTitle()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u8bd5\u5377\u6807\u9898");
        }
        List<EduQbPaperItemRequest> items = request.getItems();
        if (items == null || items.isEmpty())
        {
            throw new ServiceException("\u8bd5\u5377\u9898\u76ee\u4e0d\u80fd\u4e3a\u7a7a");
        }
        BigDecimal totalScore = items.stream()
                .map(i -> i.getScoreValue() != null ? i.getScoreValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        JSONObject config = new JSONObject();
        config.put("exportMode", request.getExportMode());
        config.put("answerLayout", request.getAnswerLayout());
        config.put("groupTab", request.getGroupTab());
        config.put("orderRadio", request.getOrderRadio());
        config.put("paperTemplate", request.getPaperTemplate());
        config.put("exportFormat", request.getExportFormat());
        config.put("pageLayout", request.getPageLayout());
        config.put("header", request.getHeader());
        config.put("answerAreas", request.getAnswerAreas());
        config.put("basketItems", request.getBasketItems());
        config.put("formExportConfig", request.getExportConfig());

        EduQbPaper paper = new EduQbPaper();
        paper.setPaperTitle(request.getPaperTitle().trim());
        paper.setTemplateCode(StringUtils.isNotEmpty(request.getTemplateCode()) ? request.getTemplateCode() : "A4_1COL");
        paper.setSortRule(request.getSortMode());
        paper.setTotalScore(totalScore);
        paper.setExportConfig(config.toJSONString());
        paper.setCreateBy(username);
        paper.setPaperType(EduQbConstants.PAPER_TYPE_USER);
        paper.setPublishStatus(EduQbConstants.PUBLISH_PUBLISHED);

        Long paperId = request.getPaperId();
        if (paperId != null)
        {
            requireOwnedPaper(paperId, username);
            paper.setPaperId(paperId);
            paperMapper.updateEduQbPaper(paper);
            paperMapper.deleteEduQbPaperItemsByPaperId(paperId);
        }
        else
        {
            paperMapper.insertEduQbPaper(paper);
            paperId = paper.getPaperId();
        }

        List<EduQbPaperItem> rows = new ArrayList<>();
        for (EduQbPaperItemRequest item : items)
        {
            if (item.getQuestionId() == null)
            {
                continue;
            }
            EduQbPaperItem row = new EduQbPaperItem();
            row.setPaperId(paperId);
            row.setQuestionId(item.getQuestionId());
            row.setOrderNum(item.getOrderNum() != null ? item.getOrderNum() : rows.size() + 1);
            row.setScoreValue(item.getScoreValue() != null ? item.getScoreValue() : BigDecimal.valueOf(5));
            row.setSectionName(item.getSectionName());
            rows.add(row);
        }
        if (!rows.isEmpty())
        {
            paperMapper.batchInsertEduQbPaperItems(rows);
        }
        return paperId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteMyPaper(Long paperId, String username)
    {
        requireOwnedPaper(paperId, username);
        paperMapper.deleteEduQbPaperItemsByPaperId(paperId);
        return paperMapper.deleteEduQbPaperById(paperId);
    }

    private EduQbPaper requireOwnedPaper(Long paperId, String username)
    {
        if (paperId == null)
        {
            throw new ServiceException("\u8bd5\u5377\u4e0d\u5b58\u5728");
        }
        EduQbPaper paper = paperMapper.selectEduQbPaperById(paperId);
        if (paper == null)
        {
            throw new ServiceException("\u8bd5\u5377\u4e0d\u5b58\u5728");
        }
        if (!username.equals(paper.getCreateBy()))
        {
            throw new ServiceException("\u65e0\u6743\u8bbf\u95ee\u8be5\u8bd5\u5377");
        }
        return paper;
    }

    private JSONObject parseConfig(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return new JSONObject();
        }
        try
        {
            return JSON.parseObject(raw);
        }
        catch (Exception e)
        {
            return new JSONObject();
        }
    }
}
