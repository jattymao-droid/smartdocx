package com.ruoyi.system.service.education.impl;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbDocxParseResult;
import com.ruoyi.system.domain.education.EduQbExamPaperCommitRequest;
import com.ruoyi.system.domain.education.EduQbExamPaperDetailResult;
import com.ruoyi.system.domain.education.EduQbExamPaperMarkItem;
import com.ruoyi.system.domain.education.EduQbExamPaperQuestionView;
import com.ruoyi.system.domain.education.EduQbImportBlock;
import com.ruoyi.system.domain.education.EduQbPaper;
import com.ruoyi.system.domain.education.EduQbPaperItem;
import com.ruoyi.system.domain.education.EduQbPaperItemRequest;
import com.ruoyi.system.domain.education.EduQbSchoolPaperPublishRequest;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.mapper.education.EduQbPaperMapper;
import com.ruoyi.system.mapper.education.EduQbQuestionMapper;
import com.ruoyi.system.mapper.education.EduSubjectMapper;
import com.ruoyi.system.service.education.IEduQbExamPaperService;
import com.ruoyi.system.service.education.IEduQbQuestionService;
import com.ruoyi.system.service.education.support.EduQbContentHashSupport;
import com.ruoyi.system.service.education.support.EduQbDocxParseService;
import com.ruoyi.system.service.education.support.EduQbExamPaperBlockAnalyzer;
import com.ruoyi.system.service.education.support.EduQbFileUploadUtils;
import com.ruoyi.system.service.education.support.EduQbImportContentSupport;
import com.ruoyi.system.service.education.support.EduQbImportContentSupport.ParsedImportContent;
import com.ruoyi.system.service.education.support.EduQbQuestionPredictService;
import com.ruoyi.system.service.education.support.EduQbLocalFileSupport;

@Service
public class EduQbExamPaperServiceImpl implements IEduQbExamPaperService
{
    private static final String DEFAULT_KNOWLEDGE = "[\"\u8bd5\u5377\u9009\u9898\"]";

    @Autowired
    private EduQbPaperMapper paperMapper;

    @Autowired
    private EduQbQuestionMapper questionMapper;

    @Autowired
    private EduSubjectMapper subjectMapper;

    @Autowired
    private IEduQbQuestionService questionService;

    @Override
    public Map<String, Object> uploadAndParse(MultipartFile file, Long subjectId, String operator)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("\u8bf7\u9009\u62e9 DOCX \u6587\u4ef6");
        }
        if (subjectId != null && subjectMapper.selectEduSubjectById(subjectId) == null)
        {
            throw new ServiceException("\u5b66\u79d1\u4e0d\u5b58\u5728");
        }
        String storedPath;
        try
        {
            storedPath = EduQbFileUploadUtils.upload(EduQbLocalFileSupport.getUploadPath(), file, new String[] { "docx" });
        }
        catch (Exception ex)
        {
            throw new ServiceException("\u6587\u4ef6\u4e0a\u4f20\u5931\u8d25\uff1a" + ex.getMessage());
        }
        File localFile = EduQbLocalFileSupport.resolveStoredFile(storedPath);
        if (!localFile.exists())
        {
            throw new ServiceException("DOCX \u6587\u4ef6\u4e0d\u5b58\u5728: " + localFile.getAbsolutePath());
        }
        EduQbDocxParseResult parsed = EduQbDocxParseService.parseFileWithPreview(localFile);
        List<EduQbImportBlock> blocks = parsed.getBlocks();
        List<EduQbExamPaperMarkItem> marked = analyzeBlocks(blocks, subjectId);

        Map<String, Object> result = new HashMap<>();
        result.put("fileName", file.getOriginalFilename());
        result.put("sourceFile", storedPath);
        result.put("blockCount", blocks.size());
        result.put("blocks", blocks);
        result.put("markedItems", marked);
        result.put("previewHtml", parsed.getPreviewHtml());
        result.put("questionCount", countIncludedQuestions(marked));
        return result;
    }

    @Override
    public List<EduQbExamPaperMarkItem> analyzeBlocks(List<EduQbImportBlock> blocks, Long subjectId)
    {
        return EduQbExamPaperBlockAnalyzer.analyze(blocks, subjectId, questionMapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long commitExamPaper(EduQbExamPaperCommitRequest request, String operator)
    {
        validateCommitRequest(request);
        if (subjectMapper.selectEduSubjectById(request.getSubjectId()) == null)
        {
            throw new ServiceException("\u5b66\u79d1\u4e0d\u5b58\u5728");
        }

        Long paperId = request.getPaperId();
        EduQbPaper paper = paperId != null ? paperMapper.selectEduQbPaperById(paperId) : null;
        if (paperId != null)
        {
            if (paper == null || !EduQbConstants.PAPER_TYPE_EXAM.equals(paper.getPaperType()))
            {
                throw new ServiceException("\u8bd5\u5377\u4e0d\u5b58\u5728");
            }
            paperMapper.deleteEduQbPaperItemsByPaperId(paperId);
        }
        else
        {
            paper = new EduQbPaper();
        }

        List<EduQbPaperItem> paperItems = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int order = 0;
        for (EduQbExamPaperMarkItem item : request.getItems())
        {
            if (item == null || !item.isQuestion() || !item.isIncluded())
            {
                continue;
            }
            if (StringUtils.isEmpty(item.getContent()))
            {
                continue;
            }
            enrichMarkItem(item);
            order++;
            Long questionId = resolveQuestionId(item, request, operator);
            BigDecimal score = item.getScoreValue() != null ? item.getScoreValue() : new BigDecimal("5");
            totalScore = totalScore.add(score);

            EduQbPaperItem row = new EduQbPaperItem();
            row.setQuestionId(questionId);
            row.setOrderNum(order);
            row.setScoreValue(score);
            row.setSectionName(item.getSectionName());
            paperItems.add(row);
        }
        if (paperItems.isEmpty())
        {
            throw new ServiceException("\u8bf7\u81f3\u5c11\u6807\u8bb0\u4e00\u9053\u8bd5\u9898");
        }

        paper.setPaperTitle(StringUtils.trim(request.getPaperTitle()));
        paper.setPaperType(EduQbConstants.PAPER_TYPE_EXAM);
        paper.setSubjectId(request.getSubjectId());
        paper.setExamCategory(StringUtils.trim(request.getExamCategory()));
        paper.setExamYear(StringUtils.trim(request.getExamYear()));
        paper.setRegion(StringUtils.trim(request.getRegion()));
        paper.setGrade(StringUtils.trim(request.getGrade()));
        paper.setSourceFile(StringUtils.trim(request.getSourceFile()));
        paper.setPublishStatus(StringUtils.isNotEmpty(request.getPublishStatus())
                ? request.getPublishStatus() : EduQbConstants.PUBLISH_DRAFT);
        paper.setTemplateCode(EduQbConstants.TEMPLATE_A4_1COL);
        paper.setTotalScore(totalScore);
        paper.setSortRule(EduQbConstants.SORT_BASKET_ORDER);
        paper.setExportConfig("{}");
        paper.setCreateBy(operator);

        if (paperId == null)
        {
            paperMapper.insertEduQbPaper(paper);
            paperId = paper.getPaperId();
        }
        else
        {
            paper.setPaperId(paperId);
            paperMapper.updateEduQbPaper(paper);
        }

        for (EduQbPaperItem row : paperItems)
        {
            row.setPaperId(paperId);
        }
        paperMapper.batchInsertEduQbPaperItems(paperItems);
        return paperId;
    }

    @Override
    public List<EduQbPaper> selectExamPaperList(EduQbPaper query)
    {
        if (query == null)
        {
            query = new EduQbPaper();
        }
        query.setPaperType(EduQbConstants.PAPER_TYPE_EXAM);
        return paperMapper.selectEduQbPaperList(query);
    }

    @Override
    public EduQbExamPaperDetailResult selectExamPaperDetail(Long paperId, boolean portalView)
    {
        EduQbPaper paper = paperMapper.selectEduQbPaperById(paperId);
        if (paper == null || !EduQbConstants.PAPER_TYPE_EXAM.equals(paper.getPaperType()))
        {
            throw new ServiceException("\u8bd5\u5377\u4e0d\u5b58\u5728");
        }
        if (portalView && !EduQbConstants.PUBLISH_PUBLISHED.equals(paper.getPublishStatus()))
        {
            throw new ServiceException("\u8bd5\u5377\u672a\u53d1\u5e03");
        }
        List<EduQbExamPaperQuestionView> questions = paperMapper.selectExamPaperQuestions(paperId);
        if (portalView)
        {
            questions.removeIf(q -> !EduQbConstants.STATUS_APPROVED.equals(q.getStatus()));
        }

        EduQbExamPaperDetailResult result = new EduQbExamPaperDetailResult();
        result.setPaperId(paper.getPaperId());
        result.setPaperTitle(paper.getPaperTitle());
        result.setSubjectId(paper.getSubjectId());
        result.setExamCategory(paper.getExamCategory());
        result.setExamYear(paper.getExamYear());
        result.setRegion(paper.getRegion());
        result.setGrade(paper.getGrade());
        result.setTotalScore(paper.getTotalScore());
        result.setItemCount(questions.size());
        result.setCreateTime(paper.getCreateTime() != null ? paper.getCreateTime().toString() : null);
        result.setQuestions(questions);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteExamPaper(Long paperId)
    {
        EduQbPaper paper = paperMapper.selectEduQbPaperById(paperId);
        if (paper == null || !EduQbConstants.PAPER_TYPE_EXAM.equals(paper.getPaperType()))
        {
            throw new ServiceException("\u8bd5\u5377\u4e0d\u5b58\u5728");
        }
        paperMapper.deleteEduQbPaperItemsByPaperId(paperId);
        return paperMapper.deleteEduQbPaperById(paperId);
    }

    @Override
    public int updatePublishStatus(Long paperId, String publishStatus)
    {
        EduQbPaper paper = paperMapper.selectEduQbPaperById(paperId);
        if (paper == null || !EduQbConstants.PAPER_TYPE_EXAM.equals(paper.getPaperType()))
        {
            throw new ServiceException("\u8bd5\u5377\u4e0d\u5b58\u5728");
        }
        EduQbPaper patch = new EduQbPaper();
        patch.setPaperId(paperId);
        patch.setPublishStatus(publishStatus);
        return paperMapper.updateEduQbPaper(patch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishSchoolExamPaper(EduQbSchoolPaperPublishRequest request, String operator)
    {
        if (request == null || request.getItems() == null || request.getItems().isEmpty())
        {
            throw new ServiceException("\u8bd5\u5377\u9898\u76ee\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String title = StringUtils.trim(request.getPaperTitle());
        if (StringUtils.isEmpty(title))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u8bd5\u5377\u6807\u9898");
        }

        Long subjectId = request.getSubjectId();
        List<EduQbPaperItem> paperItems = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int order = 0;
        for (EduQbPaperItemRequest item : request.getItems())
        {
            if (item == null || item.getQuestionId() == null)
            {
                continue;
            }
            EduQbQuestion question = questionMapper.selectEduQbQuestionById(item.getQuestionId());
            if (question == null || !"0".equals(question.getDelFlag()))
            {
                throw new ServiceException("\u9898\u76ee\u4e0d\u5b58\u5728: " + item.getQuestionId());
            }
            if (subjectId == null)
            {
                subjectId = question.getSubjectId();
            }
            order++;
            BigDecimal score = item.getScoreValue() != null ? item.getScoreValue() : new BigDecimal("5");
            totalScore = totalScore.add(score);

            EduQbPaperItem row = new EduQbPaperItem();
            row.setQuestionId(item.getQuestionId());
            row.setOrderNum(item.getOrderNum() != null ? item.getOrderNum() : order);
            row.setScoreValue(score);
            row.setSectionName(item.getSectionName());
            paperItems.add(row);
        }
        if (paperItems.isEmpty())
        {
            throw new ServiceException("\u8bf7\u81f3\u5c11\u9009\u62e9\u4e00\u9053\u8bd5\u9898");
        }
        if (subjectId == null)
        {
            throw new ServiceException("\u65e0\u6cd5\u786e\u5b9a\u5b66\u79d1");
        }
        if (subjectMapper.selectEduSubjectById(subjectId) == null)
        {
            throw new ServiceException("\u5b66\u79d1\u4e0d\u5b58\u5728");
        }

        EduQbPaper paper = new EduQbPaper();
        paper.setPaperTitle(title);
        paper.setPaperType(EduQbConstants.PAPER_TYPE_EXAM);
        paper.setSubjectId(subjectId);
        paper.setExamCategory("school");
        paper.setPublishStatus(EduQbConstants.PUBLISH_PUBLISHED);
        paper.setTemplateCode(EduQbConstants.TEMPLATE_A4_1COL);
        paper.setTotalScore(totalScore);
        paper.setSortRule(EduQbConstants.SORT_BASKET_ORDER);
        paper.setExportConfig("{\"schoolBase\":true}");
        paper.setCreateBy(operator);
        paperMapper.insertEduQbPaper(paper);

        Long paperId = paper.getPaperId();
        for (EduQbPaperItem row : paperItems)
        {
            row.setPaperId(paperId);
        }
        paperMapper.batchInsertEduQbPaperItems(paperItems);
        return paperId;
    }

    private Long resolveQuestionId(EduQbExamPaperMarkItem item, EduQbExamPaperCommitRequest request, String operator)
    {
        if (item.getMatchedQuestionId() != null && "existing".equals(item.getMatchStatus()))
        {
            EduQbQuestion existing = questionMapper.selectEduQbQuestionById(item.getMatchedQuestionId());
            if (existing != null)
            {
                return existing.getQuestionId();
            }
        }
        String hash = EduQbContentHashSupport.computeHash(item.getContent());
        List<EduQbQuestion> exact = questionMapper.selectByContentHash(request.getSubjectId(), hash, null);
        if (exact != null && !exact.isEmpty())
        {
            return exact.get(0).getQuestionId();
        }

        EduQbQuestion question = new EduQbQuestion();
        question.setSubjectId(request.getSubjectId());
        if (item.getTextbookId() != null)
        {
            question.setTextbookId(item.getTextbookId());
        }
        if (item.getChapterId() != null)
        {
            question.setChapterId(item.getChapterId());
        }
        if (StringUtils.isNotEmpty(item.getChapterText()))
        {
            question.setChapterText(item.getChapterText());
        }
        else if (StringUtils.isNotEmpty(request.getPaperTitle()))
        {
            question.setChapterText(request.getPaperTitle());
        }
        else
        {
            question.setChapterText("\u8bd5\u5377\u9009\u9898");
        }
        if (StringUtils.isNotEmpty(item.getKnowledgePoints()))
        {
            question.setKnowledgePoints(item.getKnowledgePoints());
        }
        else
        {
            question.setKnowledgePoints(DEFAULT_KNOWLEDGE);
        }
        question.setDifficulty(new BigDecimal("0.50"));
        question.setQuestionType(StringUtils.isNotEmpty(item.getQuestionType())
                ? item.getQuestionType() : EduQbConstants.TYPE_SHORT);
        question.setContent(item.getContent());
        question.setOptions(item.getOptions());
        question.setImages(limitImagesJson(item.getImages()));
        question.setSourceType(EduQbConstants.SOURCE_EXAM);
        question.setStatus(EduQbConstants.STATUS_PENDING);
        String answer = StringUtils.isNotEmpty(item.getCorrectAnswer())
                ? item.getCorrectAnswer().trim()
                : buildDefaultAnswer(question.getQuestionType(), question.getContent());
        question.setCorrectAnswer(answer);
        if (StringUtils.isNotEmpty(item.getAnalysis()))
        {
            question.setAnalysis(item.getAnalysis().trim());
        }
        questionService.insertEduQbQuestion(question, operator);
        item.setMatchStatus("new");
        item.setMatchedQuestionId(question.getQuestionId());
        return question.getQuestionId();
    }

    private void enrichMarkItem(EduQbExamPaperMarkItem item)
    {
        if (item == null || StringUtils.isEmpty(item.getContent()))
        {
            return;
        }
        String raw = item.getContent();
        if (StringUtils.isNotEmpty(item.getOptions()))
        {
            raw = raw + "\n" + unwrapOptionsText(item.getOptions());
        }
        ParsedImportContent parsed = EduQbImportContentSupport.parseContent(raw);
        if (StringUtils.isNotEmpty(parsed.getStem()))
        {
            item.setContent(parsed.getStem());
        }
        if (StringUtils.isNotEmpty(parsed.getOptionsJson()))
        {
            item.setOptions(parsed.getOptionsJson());
        }
        if (StringUtils.isEmpty(item.getCorrectAnswer()) && StringUtils.isNotEmpty(parsed.getCorrectAnswer()))
        {
            item.setCorrectAnswer(parsed.getCorrectAnswer());
        }
        if (StringUtils.isEmpty(item.getAnalysis()) && StringUtils.isNotEmpty(parsed.getAnalysis()))
        {
            item.setAnalysis(parsed.getAnalysis());
        }
        if (StringUtils.isEmpty(item.getQuestionType()))
        {
            item.setQuestionType(detectQuestionTypeForItem(item));
        }
        else if (EduQbConstants.TYPE_SINGLE.equals(item.getQuestionType()) && isMultiChoiceAnswer(item.getCorrectAnswer()))
        {
            item.setQuestionType(EduQbConstants.TYPE_MULTI);
        }
        if (StringUtils.isNotEmpty(item.getImages()))
        {
            item.setImages(limitImagesJson(item.getImages()));
        }
    }

    private String limitImagesJson(String imagesJson)
    {
        if (StringUtils.isEmpty(imagesJson))
        {
            return imagesJson;
        }
        try
        {
            List<String> urls = JSON.parseArray(imagesJson, String.class);
            if (urls == null || urls.isEmpty())
            {
                return imagesJson;
            }
            List<String> distinct = new ArrayList<>();
            for (String url : urls)
            {
                if (StringUtils.isNotEmpty(url) && !distinct.contains(url))
                {
                    distinct.add(url);
                }
            }
            if (distinct.size() <= EduQbConstants.MAX_QUESTION_IMAGES)
            {
                return JSON.toJSONString(distinct);
            }
            return JSON.toJSONString(distinct.subList(0, EduQbConstants.MAX_QUESTION_IMAGES));
        }
        catch (Exception ex)
        {
            return imagesJson;
        }
    }

    private boolean isMultiChoiceAnswer(String correctAnswer)
    {
        if (StringUtils.isEmpty(correctAnswer))
        {
            return false;
        }
        try
        {
            String value = JSON.parse(correctAnswer).toString().replaceAll("[^A-Ha-h]", "").toUpperCase();
            return value.length() > 1;
        }
        catch (Exception ex)
        {
            String value = correctAnswer.replaceAll("[^A-Ha-h]", "").toUpperCase();
            return value.length() > 1;
        }
    }

    private String unwrapOptionsText(String optionsJson)
    {
        if (StringUtils.isEmpty(optionsJson))
        {
            return "";
        }
        try
        {
            List<String> lines = JSON.parseArray(optionsJson, String.class);
            if (lines == null || lines.isEmpty())
            {
                return optionsJson;
            }
            return String.join("\n", lines);
        }
        catch (Exception ex)
        {
            return optionsJson;
        }
    }

    private String detectQuestionTypeForItem(EduQbExamPaperMarkItem item)
    {
        int optionCount = 0;
        if (StringUtils.isNotEmpty(item.getOptions()))
        {
            try
            {
                optionCount = JSON.parseArray(item.getOptions(), String.class).size();
            }
            catch (Exception ignored)
            {
            }
        }
        String raw = item.getContent();
        if (optionCount < 2 && StringUtils.isNotEmpty(raw))
        {
            optionCount = EduQbQuestionPredictService.countOptionLines(raw);
        }
        String type = EduQbQuestionPredictService.detectTypeFromText(raw, optionCount, item.getSectionName());
        if (EduQbConstants.TYPE_SINGLE.equals(type) && isMultiChoiceAnswer(item.getCorrectAnswer()))
        {
            return EduQbConstants.TYPE_MULTI;
        }
        return type;
    }

    private String buildDefaultAnswer(String questionType, String content)
    {
        if (EduQbConstants.TYPE_JUDGE.equals(questionType))
        {
            return JSON.toJSONString("true");
        }
        if (EduQbConstants.TYPE_FILL.equals(questionType) || EduQbConstants.TYPE_KNOWLEDGE_FILL.equals(questionType)
                || EduQbConstants.isSubjectiveType(questionType))
        {
            String value = content.length() > 200 ? content.substring(0, 200) : content;
            return JSON.toJSONString(value);
        }
        return JSON.toJSONString("A");
    }

    private void validateCommitRequest(EduQbExamPaperCommitRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("\u53c2\u6570\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (StringUtils.isEmpty(request.getPaperTitle()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u8bd5\u5377\u6807\u9898");
        }
        if (request.getSubjectId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u5b66\u79d1");
        }
        if (request.getItems() == null || request.getItems().isEmpty())
        {
            throw new ServiceException("\u8bf7\u81f3\u5c11\u6807\u8bb0\u4e00\u9053\u8bd5\u9898");
        }
    }

    private static int countIncludedQuestions(List<EduQbExamPaperMarkItem> marked)
    {
        int count = 0;
        if (marked == null)
        {
            return 0;
        }
        for (EduQbExamPaperMarkItem item : marked)
        {
            if (item != null && item.isQuestion() && item.isIncluded())
            {
                count++;
            }
        }
        return count;
    }
}
