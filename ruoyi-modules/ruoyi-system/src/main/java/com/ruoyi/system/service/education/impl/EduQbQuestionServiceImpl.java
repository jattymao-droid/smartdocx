package com.ruoyi.system.service.education.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.system.config.EduQbAuditProperties;
import com.ruoyi.system.config.EduQbDedupProperties;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.service.education.support.EduQbSecuritySupport;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbChapterTreeNode;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbKnowledgeTag;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.domain.education.EduQbQuestionType;
import com.ruoyi.system.domain.education.EduQbQuestionAuditBody;
import com.ruoyi.system.domain.education.EduQbDuplicateCheckBody;
import com.ruoyi.system.domain.education.EduQbDuplicateCheckResult;
import com.ruoyi.system.domain.education.EduQbQuestionFeedbackBody;
import com.ruoyi.system.domain.education.EduQbSimilarQuestion;
import com.ruoyi.system.mapper.education.EduQbQuestionMapper;
import com.ruoyi.system.mapper.education.EduQbTextbookMapper;
import com.ruoyi.system.mapper.education.EduSubjectMapper;
import com.ruoyi.system.domain.education.EduQbCatalogChapter;
import com.ruoyi.system.domain.education.EduQbTextbook;
import com.ruoyi.system.domain.education.EduQbTextbookVersion;
import com.ruoyi.system.service.education.IEduQbQuestionService;
import com.ruoyi.system.service.education.IEduQbQuestionTypeService;
import com.ruoyi.system.service.education.IEduQbTextbookService;
import com.ruoyi.system.service.education.support.EduQbContentHashSupport;
import com.ruoyi.system.service.education.support.EduQbQuestionContentSupport;

@Service
public class EduQbQuestionServiceImpl implements IEduQbQuestionService
{
    private static final int MAX_KNOWLEDGE_TAGS = 10;
    private static final int MAX_IMAGES = EduQbConstants.MAX_QUESTION_IMAGES;

    @Autowired
    private EduQbQuestionMapper questionMapper;

    @Autowired
    private EduQbTextbookMapper textbookMapper;

    @Autowired
    private EduSubjectMapper subjectMapper;

    @Autowired
    private EduQbAuditProperties auditProperties;

    @Autowired
    private EduQbDedupProperties dedupProperties;

    @Autowired
    private IEduQbQuestionTypeService questionTypeService;

    @Autowired
    private IEduQbTextbookService textbookService;

    @Override
    public EduQbQuestion selectEduQbQuestionById(Long questionId)
    {
        EduQbQuestion question = questionMapper.selectEduQbQuestionById(questionId);
        enrichCatalogContext(question);
        return question;
    }

    @Override
    public List<EduQbQuestion> selectEduQbQuestionList(EduQbQuestion question)
    {
        applyChapterScope(question);
        return questionMapper.selectEduQbQuestionList(question);
    }

    /** Expand chapter filter to include descendant chapters (matches sidebar counts). */
    private void applyChapterScope(EduQbQuestion question)
    {
        if (question == null || question.getChapterId() == null)
        {
            return;
        }
        Long rootChapterId = question.getChapterId();
        question.setChapterId(null);
        question.setChapterText(null);
        List<EduQbCatalogChapter> subtree = textbookMapper.selectChapterSubtree(rootChapterId);
        if (subtree == null || subtree.isEmpty())
        {
            question.getParams().put("chapterIds", java.util.Collections.singletonList(rootChapterId));
            return;
        }
        List<Long> chapterIds = new ArrayList<>();
        List<String> chapterNames = new ArrayList<>();
        Set<String> nameSet = new HashSet<>();
        for (EduQbCatalogChapter chapter : subtree)
        {
            if (chapter.getChapterId() != null)
            {
                chapterIds.add(chapter.getChapterId());
            }
            if (StringUtils.isNotEmpty(chapter.getChapterName()) && nameSet.add(chapter.getChapterName()))
            {
                chapterNames.add(chapter.getChapterName());
            }
        }
        question.getParams().put("chapterIds", chapterIds);
        question.getParams().put("chapterNames", chapterNames);
    }

    private void enrichCatalogContext(EduQbQuestion question)
    {
        if (question == null || question.getChapterId() == null)
        {
            return;
        }
        EduQbCatalogChapter chapter = textbookMapper.selectChapterById(question.getChapterId());
        if (chapter == null || chapter.getTextbookId() == null)
        {
            return;
        }
        EduQbTextbook textbook = textbookMapper.selectTextbookById(chapter.getTextbookId());
        if (textbook == null)
        {
            return;
        }
        question.getParams().put("textbookId", textbook.getTextbookId());
        question.getParams().put("versionId", textbook.getVersionId());
        if (textbook.getVersionId() != null)
        {
            EduQbTextbookVersion version = textbookMapper.selectVersionById(textbook.getVersionId());
            if (version != null && StringUtils.isNotEmpty(version.getSchoolStage()))
            {
                question.getParams().put("schoolStage", version.getSchoolStage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertEduQbQuestion(EduQbQuestion question, String operator)
    {
        validateQuestion(question);
        if (subjectMapper.selectEduSubjectById(question.getSubjectId()) == null)
        {
            throw new ServiceException("\u5b66\u79d1\u4e0d\u5b58\u5728");
        }
        question.setQuestionCode(generateQuestionCode());
        if (StringUtils.isEmpty(question.getSourceType()))
        {
            question.setSourceType(EduQbConstants.SOURCE_MANUAL);
        }
        applyInsertStatus(question, operator);
        applyContentHashAndValidate(question, null);
        applyTextbookFromChapter(question);
        question.setCreateBy(operator);
        int rows = questionMapper.insertEduQbQuestion(question);
        syncKnowledgeTags(question, null);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateEduQbQuestion(EduQbQuestion question, String operator)
    {
        if (question == null || question.getQuestionId() == null)
        {
            throw new ServiceException("\u8bd5\u9898ID\u4e0d\u80fd\u4e3a\u7a7a");
        }
        EduQbQuestion existing = questionMapper.selectEduQbQuestionById(question.getQuestionId());
        if (existing == null)
        {
            throw new ServiceException("\u8bd5\u9898\u4e0d\u5b58\u5728");
        }
        assertOwnerOrAdmin(existing, operator);
        validateQuestion(question);
        if (subjectMapper.selectEduSubjectById(question.getSubjectId()) == null)
        {
            throw new ServiceException("\u5b66\u79d1\u4e0d\u5b58\u5728");
        }
        applyUpdateStatus(question, existing, operator);
        applyContentHashAndValidate(question, question.getQuestionId());
        applyTextbookFromChapter(question);
        question.setUpdateBy(operator);
        int rows = questionMapper.updateEduQbQuestion(question);
        syncKnowledgeTags(question, existing);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteEduQbQuestionByIds(Long[] questionIds, String operator)
    {
        if (questionIds == null || questionIds.length == 0)
        {
            return 0;
        }
        if (!EduQbSecuritySupport.isQuestionBankManager())
        {
            for (Long questionId : questionIds)
            {
                EduQbQuestion existing = questionMapper.selectEduQbQuestionById(questionId);
                if (existing != null)
                {
                    assertOwnerOrAdmin(existing, operator);
                }
            }
        }
        return questionMapper.deleteEduQbQuestionByIds(questionIds);
    }

    @Override
    public List<EduQbKnowledgeTag> selectKnowledgeTags(Long subjectId, String keyword)
    {
        return questionMapper.selectKnowledgeTags(subjectId, keyword);
    }

    @Override
    public List<EduQbChapterTreeNode> selectKnowledgeTree(Long textbookId, Long subjectId, String keyword)
    {
        if (textbookId == null)
        {
            return new ArrayList<>();
        }
        List<EduQbChapterTreeNode> tree = textbookService.selectChapterTree(textbookId, subjectId);
        List<Map<String, Object>> stats = questionMapper.selectChapterKnowledgeTagStats(subjectId, textbookId, keyword);
        Map<Long, List<EduQbChapterTreeNode>> tagsByChapter = new LinkedHashMap<>();
        for (Map<String, Object> row : stats)
        {
            Long chapterId = toLong(row.get("chapterId"));
            String tagName = row.get("tagName") == null ? "" : String.valueOf(row.get("tagName")).trim();
            if (chapterId == null || StringUtils.isEmpty(tagName))
            {
                continue;
            }
            EduQbChapterTreeNode tagNode = new EduQbChapterTreeNode();
            tagNode.setId("k_" + chapterId + "_" + Integer.toHexString(tagName.hashCode()));
            tagNode.setLabel(tagName);
            tagNode.setNodeType("knowledge");
            tagNode.setChapterId(chapterId);
            tagNode.setTagName(tagName);
            tagNode.setCount(toInt(row.get("useCount")));
            tagsByChapter.computeIfAbsent(chapterId, key -> new ArrayList<>()).add(tagNode);
        }
        decorateKnowledgeTree(tree, tagsByChapter);
        if (StringUtils.isNotEmpty(keyword))
        {
            tree = pruneKnowledgeTree(tree);
        }
        return tree;
    }

    private void decorateKnowledgeTree(List<EduQbChapterTreeNode> nodes,
            Map<Long, List<EduQbChapterTreeNode>> tagsByChapter)
    {
        if (nodes == null)
        {
            return;
        }
        for (EduQbChapterTreeNode node : nodes)
        {
            if ("all".equals(node.getId()))
            {
                node.setNodeType("all");
                continue;
            }
            node.setNodeType("chapter");
            Long chapterId = toLong(node.getId());
            node.setChapterId(chapterId);
            List<EduQbChapterTreeNode> chapterChildren = new ArrayList<>();
            List<EduQbChapterTreeNode> knowledgeChildren = new ArrayList<>();
            for (EduQbChapterTreeNode child : node.getChildren())
            {
                if ("knowledge".equals(child.getNodeType()))
                {
                    knowledgeChildren.add(child);
                }
                else
                {
                    chapterChildren.add(child);
                }
            }
            node.setChildren(chapterChildren);
            decorateKnowledgeTree(node.getChildren(), tagsByChapter);
            if (chapterId != null && tagsByChapter.containsKey(chapterId))
            {
                node.getChildren().addAll(tagsByChapter.get(chapterId));
            }
        }
    }

    private List<EduQbChapterTreeNode> pruneKnowledgeTree(List<EduQbChapterTreeNode> nodes)
    {
        List<EduQbChapterTreeNode> kept = new ArrayList<>();
        if (nodes == null)
        {
            return kept;
        }
        for (EduQbChapterTreeNode node : nodes)
        {
            if ("all".equals(node.getId()))
            {
                continue;
            }
            List<EduQbChapterTreeNode> children = pruneKnowledgeTree(node.getChildren());
            boolean hasKnowledge = children.stream().anyMatch(child -> "knowledge".equals(child.getNodeType()));
            boolean hasChapter = children.stream().anyMatch(child -> "chapter".equals(child.getNodeType()));
            node.setChildren(children);
            if (hasKnowledge || hasChapter)
            {
                kept.add(node);
            }
        }
        return kept;
    }

    private Long toLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        try
        {
            return Long.parseLong(String.valueOf(value));
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    @Override
    @Deprecated
    public List<EduQbChapterTreeNode> selectChapterTree(Long subjectId)
    {
        List<Map<String, Object>> rows = questionMapper.selectChapterTextStats(subjectId);
        Map<String, EduQbChapterTreeNode> chapterMap = new LinkedHashMap<>();
        int total = 0;
        for (Map<String, Object> row : rows)
        {
            String chapterText = row.get("chapterText") == null ? "" : String.valueOf(row.get("chapterText")).trim();
            if (StringUtils.isEmpty(chapterText))
            {
                continue;
            }
            int count = toInt(row.get("questionCount"));
            total += count;
            String[] parts = chapterText.split("\\s*>\\s*|\\s*/\\s*", 2);
            String parentLabel = parts[0].trim();
            EduQbChapterTreeNode parent = chapterMap.computeIfAbsent(parentLabel, key -> {
                EduQbChapterTreeNode node = new EduQbChapterTreeNode();
                node.setId("ch-" + key);
                node.setLabel(key);
                node.setCount(0);
                return node;
            });
            parent.setCount(parent.getCount() + count);
            if (parts.length > 1 && StringUtils.isNotEmpty(parts[1]))
            {
                String childLabel = parts[1].trim();
                String childId = parent.getId() + "::" + childLabel;
                EduQbChapterTreeNode child = parent.getChildren().stream()
                        .filter(item -> childId.equals(item.getId()))
                        .findFirst()
                        .orElse(null);
                if (child == null)
                {
                    child = new EduQbChapterTreeNode();
                    child.setId(childId);
                    child.setLabel(childLabel);
                    child.setCount(count);
                    parent.getChildren().add(child);
                }
                else
                {
                    child.setCount(child.getCount() + count);
                }
            }
        }
        List<EduQbChapterTreeNode> tree = new ArrayList<>();
        EduQbChapterTreeNode all = new EduQbChapterTreeNode();
        all.setId("all");
        all.setLabel("\u5168\u90e8\u7ae0\u8282");
        all.setCount(total);
        tree.add(all);
        tree.addAll(chapterMap.values());
        return tree;
    }

    private int toInt(Object value)
    {
        if (value == null)
        {
            return 0;
        }
        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }
        try
        {
            return Integer.parseInt(String.valueOf(value));
        }
        catch (NumberFormatException ex)
        {
            return 0;
        }
    }

    @Override
    public boolean canManage(EduQbQuestion question, String operator)
    {
        if (question == null)
        {
            return false;
        }
        if (EduQbSecuritySupport.isQuestionBankManager())
        {
            return true;
        }
        return StringUtils.isNotEmpty(operator) && operator.equals(question.getCreateBy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditQuestions(EduQbQuestionAuditBody body, String operator)
    {
        assertAuditor(operator);
        if (body == null || body.getQuestionIds() == null || body.getQuestionIds().isEmpty())
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u8981\u5ba1\u6838\u7684\u8bd5\u9898");
        }
        String action = body.getAction();
        if (StringUtils.isEmpty(action))
        {
            throw new ServiceException("\u8bf7\u6307\u5b9a\u5ba1\u6838\u64cd\u4f5c");
        }
        Long[] ids = body.getQuestionIds().toArray(new Long[0]);
        String status;
        String remark = null;
        if (EduQbQuestionAuditBody.ACTION_APPROVE.equals(action))
        {
            status = EduQbConstants.STATUS_APPROVED;
        }
        else if (EduQbQuestionAuditBody.ACTION_REJECT.equals(action))
        {
            status = EduQbConstants.STATUS_REJECTED;
            if (StringUtils.isEmpty(body.getRemark()))
            {
                throw new ServiceException("\u8bf7\u586b\u5199\u9000\u56de\u539f\u56e0");
            }
            remark = body.getRemark().trim();
            if (remark.length() > 500)
            {
                throw new ServiceException("\u9000\u56de\u539f\u56e0\u4e0d\u80fd\u8d85\u8fc7500\u5b57");
            }
        }
        else
        {
            throw new ServiceException("\u4e0d\u652f\u6301\u7684\u5ba1\u6838\u64cd\u4f5c");
        }
        for (Long questionId : ids)
        {
            EduQbQuestion existing = questionMapper.selectEduQbQuestionById(questionId);
            if (existing == null)
            {
                throw new ServiceException("\u8bd5\u9898\u4e0d\u5b58\u5728\uff1a" + questionId);
            }
            if (!EduQbConstants.STATUS_PENDING.equals(existing.getStatus()))
            {
                throw new ServiceException("\u4ec5\u5f85\u5ba1\u6838\u72b6\u6001\u7684\u8bd5\u9898\u53ef\u5ba1\u6838\uff1a" + existing.getQuestionCode());
            }
        }
        int updated = questionMapper.updateQuestionAuditStatus(ids, status, remark, operator);
        if (updated < ids.length)
        {
            throw new ServiceException("\u90e8\u5206\u8bd5\u9898\u72b6\u6001\u5df2\u53d8\u66f4\uff0c\u8bf7\u5237\u65b0\u540e\u91cd\u8bd5");
        }
        return updated;
    }

    @Override
    public int countPendingQuestions()
    {
        return questionMapper.countQuestionsByStatus(EduQbConstants.STATUS_PENDING);
    }

    @Override
    public EduQbDuplicateCheckResult checkDuplicates(EduQbDuplicateCheckBody body)
    {
        EduQbDuplicateCheckResult result = new EduQbDuplicateCheckResult();
        if (body == null || body.getSubjectId() == null || StringUtils.isEmpty(body.getContent()))
        {
            return result;
        }
        String hash = EduQbContentHashSupport.computeHash(body.getContent());
        result.setContentHash(hash);
        if (!dedupProperties.isEnabled())
        {
            return result;
        }
        if (hash != null)
        {
            result.setExactMatches(questionMapper.selectByContentHash(body.getSubjectId(), hash, body.getQuestionId()));
        }
        List<EduQbQuestion> candidates = questionMapper.selectDedupCandidates(body.getSubjectId(),
                body.getQuestionId(), dedupProperties.getCandidateLimit());
        Set<Long> exactIds = new HashSet<>();
        for (EduQbQuestion exact : result.getExactMatches())
        {
            exactIds.add(exact.getQuestionId());
        }
        List<EduQbSimilarQuestion> similarMatches = new ArrayList<>();
        for (EduQbQuestion candidate : candidates)
        {
            if (exactIds.contains(candidate.getQuestionId()))
            {
                continue;
            }
            double score = EduQbContentHashSupport.similarity(body.getContent(), candidate.getContent());
            if (score < dedupProperties.getSimilarityThreshold())
            {
                continue;
            }
            EduQbSimilarQuestion item = new EduQbSimilarQuestion();
            BeanUtils.copyBeanProp(item, candidate);
            item.setSimilarity(Math.round(score * 1000D) / 1000D);
            similarMatches.add(item);
        }
        similarMatches.sort(Comparator.comparing(EduQbSimilarQuestion::getSimilarity).reversed());
        int limit = Math.max(1, dedupProperties.getResultLimit());
        if (similarMatches.size() > limit)
        {
            similarMatches = new ArrayList<>(similarMatches.subList(0, limit));
        }
        result.setSimilarMatches(similarMatches);
        return result;
    }

    @Override
    public int submitQuestionFeedback(EduQbQuestionFeedbackBody body, String operator)
    {
        if (body == null || body.getQuestionId() == null)
        {
            throw new ServiceException("\u8bf7\u6307\u5b9a\u8981\u7ea0\u9519\u7684\u8bd5\u9898");
        }
        if (StringUtils.isEmpty(body.getFeedbackType()))
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u7ea0\u9519\u7c7b\u578b");
        }
        EduQbQuestion question = questionMapper.selectEduQbQuestionById(body.getQuestionId());
        if (question == null)
        {
            throw new ServiceException("\u8bd5\u9898\u4e0d\u5b58\u5728");
        }
        return questionMapper.insertEduQbQuestionFeedback(body, operator);
    }

    private void applyContentHashAndValidate(EduQbQuestion question, Long excludeQuestionId)
    {
        String hash = EduQbContentHashSupport.computeHash(question.getContent());
        question.setContentHash(hash);
        if (!dedupProperties.isEnabled() || hash == null || !dedupProperties.isBlockExactDuplicate())
        {
            return;
        }
        List<EduQbQuestion> exact = questionMapper.selectByContentHash(question.getSubjectId(), hash, excludeQuestionId);
        if (!exact.isEmpty())
        {
            throw new ServiceException("\u540c\u4e00\u5b66\u79d1\u4e0b\u5df2\u5b58\u5728\u76f8\u540c\u9898\u5e72\uff1a" + exact.get(0).getQuestionCode());
        }
    }

    private void applyInsertStatus(EduQbQuestion question, String operator)
    {
        if (isAuditor(operator))
        {
            if (StringUtils.isEmpty(question.getStatus()))
            {
                question.setStatus(EduQbConstants.STATUS_APPROVED);
            }
            return;
        }
        question.setStatus(resolveInitialStatus());
    }

    private void applyUpdateStatus(EduQbQuestion question, EduQbQuestion existing, String operator)
    {
        if (isAuditor(operator))
        {
            if (StringUtils.isEmpty(question.getStatus()))
            {
                question.setStatus(existing.getStatus());
            }
            return;
        }
        question.setStatus(existing.getStatus());
        if (EduQbConstants.STATUS_REJECTED.equals(existing.getStatus()))
        {
            question.setStatus(EduQbConstants.STATUS_PENDING);
            question.setRemark(null);
        }
        else if (auditProperties.isEnabled()
                && EduQbConstants.STATUS_APPROVED.equals(existing.getStatus())
                && hasContentChanged(question, existing))
        {
            question.setStatus(EduQbConstants.STATUS_PENDING);
            question.setRemark(null);
        }
    }

    private boolean hasContentChanged(EduQbQuestion question, EduQbQuestion existing)
    {
        if (!StringUtils.equals(StringUtils.trimToEmpty(question.getContent()),
                StringUtils.trimToEmpty(existing.getContent())))
        {
            return true;
        }
        if (!StringUtils.equals(StringUtils.trimToEmpty(question.getOptions()),
                StringUtils.trimToEmpty(existing.getOptions())))
        {
            return true;
        }
        if (!StringUtils.equals(StringUtils.trimToEmpty(question.getCorrectAnswer()),
                StringUtils.trimToEmpty(existing.getCorrectAnswer())))
        {
            return true;
        }
        if (!StringUtils.equals(StringUtils.trimToEmpty(question.getAnalysis()),
                StringUtils.trimToEmpty(existing.getAnalysis())))
        {
            return true;
        }
        return false;
    }

    private String resolveInitialStatus()
    {
        if (auditProperties.isEnabled())
        {
            return EduQbConstants.STATUS_PENDING;
        }
        return EduQbConstants.STATUS_APPROVED;
    }

    private boolean isAuditor(String operator)
    {
        return EduQbSecuritySupport.isQuestionBankManager();
    }

    private void assertAuditor(String operator)
    {
        if (!isAuditor(operator))
        {
            throw new ServiceException("\u60a8\u6ca1\u6709\u5ba1\u6838\u6743\u9650");
        }
    }

    private void validateQuestion(EduQbQuestion question)
    {
        if (question == null)
        {
            throw new ServiceException("\u53c2\u6570\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (question.getSubjectId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u5b66\u79d1");
        }
        if (StringUtils.isEmpty(question.getContent()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u9898\u5e72");
        }
        question.setContent(EduQbQuestionContentSupport.stripLeadingQuestionNo(question.getContent().trim()));
        if (StringUtils.isEmpty(question.getContent()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u9898\u5e72");
        }
        if (StringUtils.isEmpty(question.getChapterText()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u7ae0\u8282");
        }
        questionTypeService.assertEnabledType(question.getQuestionType());
        int maxContentLen = EduQbQuestionContentSupport.isHtmlContent(question.getContent())
                ? EduQbConstants.MAX_CONTENT_LEN_HTML
                : questionTypeService.resolveMaxContentLength(question.getQuestionType());
        if (question.getContent().length() > maxContentLen)
        {
            throw new ServiceException("\u9898\u5e72\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7" + maxContentLen);
        }
        validateDifficulty(question.getDifficulty());
        validateKnowledgePoints(question.getKnowledgePoints());
        validateAnswerByType(question);
        validateImages(question.getImages());
        normalizeJsonFields(question);
    }

    private void validateDifficulty(BigDecimal difficulty)
    {
        if (difficulty == null)
        {
            throw new ServiceException("\u8bf7\u8bbe\u5b9a\u96be\u5ea6");
        }
        if (difficulty.compareTo(new BigDecimal("0.1")) < 0 || difficulty.compareTo(BigDecimal.ONE) > 0)
        {
            throw new ServiceException("\u96be\u5ea6\u9700\u5728 0.1~1.0 \u4e4b\u95f4");
        }
    }

    private void validateKnowledgePoints(String knowledgePoints)
    {
        if (StringUtils.isEmpty(knowledgePoints))
        {
            throw new ServiceException("\u8bf7\u81f3\u5c11\u6dfb\u52a0\u4e00\u4e2a\u77e5\u8bc6\u70b9");
        }
        JSONArray array;
        try
        {
            array = JSON.parseArray(knowledgePoints);
        }
        catch (Exception ex)
        {
            throw new ServiceException("\u77e5\u8bc6\u70b9\u683c\u5f0f\u4e0d\u6b63\u786e");
        }
        if (array == null || array.isEmpty())
        {
            throw new ServiceException("\u8bf7\u81f3\u5c11\u6dfb\u52a0\u4e00\u4e2a\u77e5\u8bc6\u70b9");
        }
        if (array.size() > MAX_KNOWLEDGE_TAGS)
        {
            throw new ServiceException("\u77e5\u8bc6\u70b9\u4e0d\u80fd\u8d85\u8fc7" + MAX_KNOWLEDGE_TAGS + "\u4e2a");
        }
    }

    private void validateAnswerByType(EduQbQuestion question)
    {
        String type = question.getQuestionType();
        String answer = question.getCorrectAnswer();
        if (StringUtils.isEmpty(answer))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u6807\u51c6\u7b54\u6848");
        }
        EduQbQuestionType typeRow = questionTypeService.selectEnabledByCode(type);
        String answerMode = typeRow != null ? typeRow.getAnswerMode() : resolveLegacyAnswerMode(type);
        if (answerMode == null)
        {
            throw new ServiceException("\u4e0d\u652f\u6301\u7684\u9898\u578b");
        }
        if ("choice".equals(answerMode))
        {
            if (StringUtils.isEmpty(question.getOptions()))
            {
                throw new ServiceException("\u8bf7\u8f93\u5165\u9009\u9879");
            }
            return;
        }
        if ("multi".equals(answerMode))
        {
            if (StringUtils.isEmpty(question.getOptions()))
            {
                throw new ServiceException("\u8bf7\u8f93\u5165\u9009\u9879");
            }
            try
            {
                JSON.parseArray(answer);
            }
            catch (Exception ex)
            {
                throw new ServiceException("\u591a\u9009\u9898\u7b54\u6848\u683c\u5f0f\u4e0d\u6b63\u786e");
            }
            return;
        }
        if ("judge".equals(answerMode) || "fill".equals(answerMode) || "subjective".equals(answerMode))
        {
            return;
        }
        throw new ServiceException("\u4e0d\u652f\u6301\u7684\u9898\u578b");
    }

    private String resolveLegacyAnswerMode(String type)
    {
        if (EduQbConstants.TYPE_SINGLE.equals(type))
        {
            return "choice";
        }
        if (EduQbConstants.TYPE_MULTI.equals(type))
        {
            return "multi";
        }
        if (EduQbConstants.TYPE_JUDGE.equals(type))
        {
            return "judge";
        }
        if (EduQbConstants.TYPE_FILL.equals(type) || EduQbConstants.TYPE_KNOWLEDGE_FILL.equals(type))
        {
            return "fill";
        }
        if (EduQbConstants.isSubjectiveType(type))
        {
            return "subjective";
        }
        return null;
    }

    private void validateImages(String images)
    {
        if (StringUtils.isEmpty(images))
        {
            return;
        }
        try
        {
            JSONArray array = JSON.parseArray(images);
            if (array != null && array.size() > MAX_IMAGES)
            {
                throw new ServiceException("\u56fe\u7247\u4e0d\u80fd\u8d85\u8fc7" + MAX_IMAGES + "\u5f20");
            }
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("\u56fe\u7247\u683c\u5f0f\u4e0d\u6b63\u786e");
        }
    }

    private void normalizeJsonFields(EduQbQuestion question)
    {
        question.setCorrectAnswer(normalizeJsonValue(question.getCorrectAnswer()));
        if (StringUtils.isNotEmpty(question.getOptions()))
        {
            question.setOptions(normalizeJsonValue(question.getOptions()));
        }
        if (StringUtils.isNotEmpty(question.getImages()))
        {
            question.setImages(normalizeJsonValue(question.getImages()));
        }
        if (StringUtils.isEmpty(question.getKnowledgePoints()))
        {
            question.setKnowledgePoints("[]");
        }
        else
        {
            question.setKnowledgePoints(normalizeJsonValue(question.getKnowledgePoints()));
        }
    }

    private String normalizeJsonValue(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return raw;
        }
        String value = raw.trim();
        try
        {
            JSON.parse(value);
            return value;
        }
        catch (Exception ex)
        {
            return JSON.toJSONString(value);
        }
    }

    private void applyTextbookFromChapter(EduQbQuestion question)
    {
        if (question == null || question.getTextbookId() != null || question.getChapterId() == null)
        {
            return;
        }
        EduQbCatalogChapter chapter = textbookMapper.selectChapterById(question.getChapterId());
        if (chapter != null && chapter.getTextbookId() != null)
        {
            question.setTextbookId(chapter.getTextbookId());
        }
    }

    private void syncKnowledgeTags(EduQbQuestion question, EduQbQuestion existing)
    {
        Set<String> newTags = parseKnowledgeTagSet(question.getKnowledgePoints());
        Set<String> oldTags = existing != null ? parseKnowledgeTagSet(existing.getKnowledgePoints()) : new HashSet<>();
        for (String tag : newTags)
        {
            if (existing == null || !oldTags.contains(tag))
            {
                questionMapper.upsertKnowledgeTag(question.getSubjectId(), tag);
            }
        }
        if (existing != null)
        {
            for (String tag : oldTags)
            {
                if (!newTags.contains(tag))
                {
                    questionMapper.decrementKnowledgeTag(question.getSubjectId(), tag);
                }
            }
        }
    }

    private Set<String> parseKnowledgeTagSet(String knowledgePoints)
    {
        Set<String> tags = new HashSet<>();
        JSONArray array = JSON.parseArray(knowledgePoints);
        if (array == null)
        {
            return tags;
        }
        for (int i = 0; i < array.size(); i++)
        {
            String tag = array.getString(i);
            if (StringUtils.isNotEmpty(tag))
            {
                tags.add(tag.trim());
            }
        }
        return tags;
    }

    private synchronized String generateQuestionCode()
    {
        String date = DateUtils.dateTimeNow("yyyyMMdd");
        String prefix = "Q-" + date + "-";
        String maxCode = questionMapper.selectMaxQuestionCodeByPrefix(prefix);
        int seq = 1;
        if (StringUtils.isNotEmpty(maxCode) && maxCode.length() > prefix.length())
        {
            try
            {
                seq = Integer.parseInt(maxCode.substring(prefix.length())) + 1;
            }
            catch (NumberFormatException ignored)
            {
                seq = 1;
            }
        }
        return prefix + String.format("%03d", seq);
    }

    private void assertOwnerOrAdmin(EduQbQuestion existing, String operator)
    {
        if (EduQbSecuritySupport.isQuestionBankManager())
        {
            return;
        }
        if (existing.getCreateBy() == null || !existing.getCreateBy().equals(operator))
        {
            throw new ServiceException("\u60a8\u6ca1\u6709\u6743\u9650\u64cd\u4f5c\u4ed6\u4eba\u5f55\u5165\u7684\u8bd5\u9898");
        }
    }
}
