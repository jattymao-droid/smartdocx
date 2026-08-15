package com.ruoyi.system.service.education.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbCatalogChapter;
import com.ruoyi.system.domain.education.EduQbChapterTreeNode;
import com.ruoyi.system.domain.education.EduQbTextbook;
import com.ruoyi.system.domain.education.EduQbTextbookVersion;
import com.ruoyi.system.mapper.education.EduQbTextbookMapper;
import com.ruoyi.system.service.education.IEduQbTextbookService;

@Service
public class EduQbTextbookServiceImpl implements IEduQbTextbookService
{
    @Autowired
    private EduQbTextbookMapper textbookMapper;

    @Override
    public List<EduQbTextbookVersion> selectVersions(Long subjectId, String schoolStage)
    {
        return textbookMapper.selectVersionsBySubjectId(subjectId, schoolStage);
    }

    @Override
    public List<EduQbTextbookVersion> selectVersionsAdmin(Long subjectId, String schoolStage)
    {
        return textbookMapper.selectVersionsAdminBySubjectId(subjectId, schoolStage);
    }

    @Override
    public EduQbTextbookVersion selectVersionById(Long versionId)
    {
        return textbookMapper.selectVersionById(versionId);
    }

    @Override
    public int insertVersion(EduQbTextbookVersion version)
    {
        validateVersion(version, true);
        if (StringUtils.isEmpty(version.getStatus()))
        {
            version.setStatus("0");
        }
        if (version.getOrderNum() == null)
        {
            version.setOrderNum(0);
        }
        if (StringUtils.isEmpty(version.getSchoolStage()))
        {
            version.setSchoolStage("\u9ad8\u4e2d");
        }
        return textbookMapper.insertVersion(version);
    }

    @Override
    public int updateVersion(EduQbTextbookVersion version)
    {
        validateVersion(version, false);
        return textbookMapper.updateVersion(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteVersionByIds(Long[] versionIds)
    {
        if (versionIds == null || versionIds.length == 0)
        {
            return 0;
        }
        for (Long versionId : versionIds)
        {
            if (textbookMapper.countTextbookByVersionId(versionId) > 0)
            {
                throw new ServiceException("\u7248\u672c\u4e0b\u5b58\u5728\u6559\u6750\uff0c\u8bf7\u5148\u5220\u9664\u6559\u6750");
            }
        }
        return textbookMapper.deleteVersionByIds(versionIds);
    }

    @Override
    public List<EduQbTextbook> selectTextbooks(Long versionId)
    {
        return textbookMapper.selectTextbooksByVersionId(versionId);
    }

    @Override
    public List<EduQbTextbook> selectTextbooksAdmin(Long versionId)
    {
        return textbookMapper.selectTextbooksAdminByVersionId(versionId);
    }

    @Override
    public EduQbTextbook selectTextbookById(Long textbookId)
    {
        return textbookMapper.selectTextbookById(textbookId);
    }

    @Override
    public int insertTextbook(EduQbTextbook textbook)
    {
        validateTextbook(textbook, true);
        if (StringUtils.isEmpty(textbook.getStatus()))
        {
            textbook.setStatus("0");
        }
        if (textbook.getOrderNum() == null)
        {
            textbook.setOrderNum(0);
        }
        return textbookMapper.insertTextbook(textbook);
    }

    @Override
    public int updateTextbook(EduQbTextbook textbook)
    {
        validateTextbook(textbook, false);
        return textbookMapper.updateTextbook(textbook);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTextbookByIds(Long[] textbookIds)
    {
        if (textbookIds == null || textbookIds.length == 0)
        {
            return 0;
        }
        for (Long textbookId : textbookIds)
        {
            if (textbookMapper.countChapterByTextbookId(textbookId) > 0)
            {
                throw new ServiceException("\u6559\u6750\u4e0b\u5b58\u5728\u7ae0\u8282\uff0c\u8bf7\u5148\u5220\u9664\u7ae0\u8282");
            }
        }
        return textbookMapper.deleteTextbookByIds(textbookIds);
    }

    @Override
    public List<EduQbChapterTreeNode> selectChapterTree(Long textbookId, Long subjectId)
    {
        if (textbookId == null)
        {
            return new ArrayList<>();
        }
        List<EduQbCatalogChapter> rows = textbookMapper.selectChaptersByTextbookId(textbookId, subjectId);
        return buildChapterTree(rows, true, textbookId, subjectId);
    }

    @Override
    public List<EduQbCatalogChapter> selectChapterList(Long textbookId)
    {
        if (textbookId == null)
        {
            return new ArrayList<>();
        }
        return textbookMapper.selectChapterListByTextbookId(textbookId);
    }

    @Override
    public EduQbCatalogChapter selectChapterById(Long chapterId)
    {
        return textbookMapper.selectChapterById(chapterId);
    }

    @Override
    public int insertChapter(EduQbCatalogChapter chapter)
    {
        validateChapter(chapter, true);
        if (chapter.getOrderNum() == null)
        {
            chapter.setOrderNum(0);
        }
        return textbookMapper.insertChapter(chapter);
    }

    @Override
    public int updateChapter(EduQbCatalogChapter chapter)
    {
        validateChapter(chapter, false);
        if (chapter.getChapterId() != null && chapter.getParentId() != null
                && chapter.getChapterId().equals(chapter.getParentId()))
        {
            throw new ServiceException("\u4e0a\u7ea7\u7ae0\u8282\u4e0d\u80fd\u662f\u81ea\u8eab");
        }
        return textbookMapper.updateChapter(chapter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteChapterByIds(Long[] chapterIds)
    {
        if (chapterIds == null || chapterIds.length == 0)
        {
            return 0;
        }
        for (Long chapterId : chapterIds)
        {
            if (textbookMapper.countChildChapter(chapterId) > 0)
            {
                throw new ServiceException("\u8bf7\u5148\u5220\u9664\u5b50\u7ae0\u8282");
            }
            if (textbookMapper.countQuestionsInChapterSubtree(chapterId, null) > 0)
            {
                throw new ServiceException("\u7ae0\u8282\u5df2\u5173\u8054\u8bd5\u9898\uff0c\u65e0\u6cd5\u5220\u9664");
            }
        }
        return textbookMapper.deleteChapterByIds(chapterIds);
    }

    private void validateVersion(EduQbTextbookVersion version, boolean creating)
    {
        if (version == null)
        {
            throw new ServiceException("\u7248\u672c\u4fe1\u606f\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (version.getSubjectId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u5b66\u79d1");
        }
        if (StringUtils.isEmpty(version.getVersionName()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u7248\u672c\u540d\u79f0");
        }
        if (StringUtils.isEmpty(version.getSchoolStage()))
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u5b66\u6bb5");
        }
        if (!creating && version.getVersionId() == null)
        {
            throw new ServiceException("\u7248\u672cID\u4e0d\u80fd\u4e3a\u7a7a");
        }
    }

    private void validateTextbook(EduQbTextbook textbook, boolean creating)
    {
        if (textbook == null)
        {
            throw new ServiceException("\u6559\u6750\u4fe1\u606f\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (textbook.getVersionId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u7248\u672c");
        }
        if (StringUtils.isEmpty(textbook.getTextbookName()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u6559\u6750\u540d\u79f0");
        }
        if (!creating && textbook.getTextbookId() == null)
        {
            throw new ServiceException("\u6559\u6750ID\u4e0d\u80fd\u4e3a\u7a7a");
        }
    }

    private void validateChapter(EduQbCatalogChapter chapter, boolean creating)
    {
        if (chapter == null)
        {
            throw new ServiceException("\u7ae0\u8282\u4fe1\u606f\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (chapter.getTextbookId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u6559\u6750");
        }
        if (StringUtils.isEmpty(chapter.getChapterName()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u7ae0\u8282\u540d\u79f0");
        }
        if (!creating && chapter.getChapterId() == null)
        {
            throw new ServiceException("\u7ae0\u8282ID\u4e0d\u80fd\u4e3a\u7a7a");
        }
    }

    private List<EduQbChapterTreeNode> buildChapterTree(List<EduQbCatalogChapter> rows, boolean includeAllNode,
            Long textbookId, Long subjectId)
    {
        Map<Long, Integer> directCounts = loadDirectQuestionCounts(textbookId, subjectId);
        Map<Long, EduQbChapterTreeNode> nodeMap = new LinkedHashMap<>();
        List<EduQbChapterTreeNode> roots = new ArrayList<>();
        for (EduQbCatalogChapter row : rows)
        {
            EduQbChapterTreeNode node = new EduQbChapterTreeNode();
            node.setId(String.valueOf(row.getChapterId()));
            node.setLabel(row.getChapterName());
            node.setCount(directCounts.getOrDefault(row.getChapterId(), 0));
            nodeMap.put(row.getChapterId(), node);
            if (row.getParentId() == null)
            {
                roots.add(node);
            }
        }
        for (EduQbCatalogChapter row : rows)
        {
            if (row.getParentId() != null)
            {
                EduQbChapterTreeNode parent = nodeMap.get(row.getParentId());
                EduQbChapterTreeNode child = nodeMap.get(row.getChapterId());
                if (parent != null && child != null)
                {
                    parent.getChildren().add(child);
                }
            }
        }
        List<EduQbChapterTreeNode> tree = new ArrayList<>();
        if (includeAllNode)
        {
            EduQbChapterTreeNode all = new EduQbChapterTreeNode();
            all.setId("all");
            all.setLabel("\u5168\u90e8\u7ae0\u8282");
            int total = textbookId != null
                    ? textbookMapper.countQuestionsByTextbookId(textbookId, subjectId)
                    : 0;
            all.setCount(total);
            tree.add(all);
        }
        tree.addAll(roots);
        aggregateChapterTreeCounts(roots);
        return tree;
    }

    private Map<Long, Integer> loadDirectQuestionCounts(Long textbookId, Long subjectId)
    {
        Map<Long, Integer> counts = new HashMap<>();
        if (textbookId == null)
        {
            return counts;
        }
        List<Map<String, Object>> rows = textbookMapper.selectDirectQuestionCountsByTextbook(textbookId, subjectId);
        if (rows == null)
        {
            return counts;
        }
        for (Map<String, Object> row : rows)
        {
            if (row == null || row.get("chapterId") == null)
            {
                continue;
            }
            Long chapterId = Long.valueOf(String.valueOf(row.get("chapterId")));
            Object countObj = row.get("questionCount");
            int count = countObj == null ? 0 : Integer.parseInt(String.valueOf(countObj));
            counts.put(chapterId, count);
        }
        return counts;
    }

    /** Parent count = direct questions on node + sum of children's aggregated counts. */
    private void aggregateChapterTreeCounts(List<EduQbChapterTreeNode> nodes)
    {
        if (nodes == null)
        {
            return;
        }
        for (EduQbChapterTreeNode node : nodes)
        {
            List<EduQbChapterTreeNode> children = node.getChildren();
            if (children != null && !children.isEmpty())
            {
                aggregateChapterTreeCounts(children);
                int childSum = 0;
                for (EduQbChapterTreeNode child : children)
                {
                    childSum += child.getCount() != null ? child.getCount() : 0;
                }
                int direct = node.getCount() != null ? node.getCount() : 0;
                node.setCount(direct + childSum);
            }
        }
    }
}
