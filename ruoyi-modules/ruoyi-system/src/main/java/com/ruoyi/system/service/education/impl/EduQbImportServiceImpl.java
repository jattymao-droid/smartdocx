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
import com.ruoyi.system.service.education.support.EduQbLocalFileSupport;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.service.education.support.EduQbSecuritySupport;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.service.education.support.EduQbFileUploadUtils;
import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbDocxParseResult;
import com.ruoyi.system.domain.education.EduQbImportBlock;
import com.ruoyi.system.domain.education.EduQbImportCommitBody;
import com.ruoyi.system.domain.education.EduQbImportParsePayload;
import com.ruoyi.system.domain.education.EduQbImportCommitItem;
import com.ruoyi.system.domain.education.EduQbImportTask;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.mapper.education.EduQbImportTaskMapper;
import com.ruoyi.system.mapper.education.EduSubjectMapper;
import com.ruoyi.system.service.education.IEduQbImportService;
import com.ruoyi.system.service.education.IEduQbQuestionService;
import com.ruoyi.system.service.education.support.EduQbDocxParseService;
import com.ruoyi.system.service.education.support.EduQbImportContentSupport;
import com.ruoyi.system.service.education.support.EduQbImportContentSupport.ParsedImportContent;
import com.ruoyi.system.service.education.support.EduQbImportTaskHelper;
import com.ruoyi.system.service.education.support.EduQbChapterMatchService;

@Service
public class EduQbImportServiceImpl implements IEduQbImportService
{
    @Autowired
    private EduQbImportTaskMapper importTaskMapper;

    @Autowired
    private EduSubjectMapper subjectMapper;

    @Autowired
    private IEduQbQuestionService questionService;

    @Autowired
    private EduQbImportTaskHelper importTaskHelper;

    @Autowired
    private EduQbChapterMatchService chapterMatchService;

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        EduQbDocxParseResult parsed = EduQbDocxParseService.parseFileWithPreview(localFile);
        List<EduQbImportBlock> blocks = parsed.getBlocks();

        EduQbImportTask task = new EduQbImportTask();
        task.setFileName(file.getOriginalFilename());
        task.setFilePath(storedPath);
        task.setSubjectId(subjectId);
        task.setStatus(EduQbImportTask.STATUS_PARSED);
        task.setBlockCount(blocks.size());
        task.setImportedCount(0);
        task.setParseResult(buildParseResultJson(blocks, parsed.getPreviewHtml()));
        task.setCreateBy(operator);
        importTaskMapper.insertEduQbImportTask(task);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("fileName", task.getFileName());
        result.put("filePath", storedPath);
        result.put("blockCount", blocks.size());
        result.put("imageCount", countImages(blocks));
        result.put("blocks", blocks);
        result.put("previewHtml", parsed.getPreviewHtml());
        result.put("chapterHeadings", chapterMatchService.extractHeadingHints(blocks));
        return result;
    }

    private int countImages(List<EduQbImportBlock> blocks)
    {
        int count = 0;
        if (blocks == null)
        {
            return 0;
        }
        for (EduQbImportBlock block : blocks)
        {
            if (block.getImageUrls() != null)
            {
                count += block.getImageUrls().size();
            }
        }
        return count;
    }

    @Override
    public List<EduQbImportTask> selectImportTaskList(EduQbImportTask query, String operator)
    {
        if (query == null)
        {
            query = new EduQbImportTask();
        }
        if (!EduQbSecuritySupport.isQuestionBankManager())
        {
            query.setCreateBy(operator);
        }
        return importTaskMapper.selectEduQbImportTaskList(query);
    }

    @Override
    public EduQbImportTask getTask(Long taskId, String operator)
    {
        EduQbImportTask task = importTaskMapper.selectEduQbImportTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("\u5bfc\u5165\u4efb\u52a1\u4e0d\u5b58\u5728");
        }
        assertTaskOwner(task, operator);
        return task;
    }

    @Override
    public List<EduQbImportBlock> getBlocks(Long taskId, String operator)
    {
        EduQbImportTask task = getTask(taskId, operator);
        return loadParsePayload(task).getBlocks();
    }

    @Override
    public String getPreviewHtml(Long taskId, String operator)
    {
        EduQbImportTask task = getTask(taskId, operator);
        return loadParsePayload(task).getPreviewHtml();
    }

    private EduQbImportParsePayload loadParsePayload(EduQbImportTask task)
    {
        EduQbImportParsePayload payload = new EduQbImportParsePayload();
        if (task == null || StringUtils.isEmpty(task.getParseResult()))
        {
            return payload;
        }
        String raw = task.getParseResult().trim();
        if (raw.startsWith("["))
        {
            List<EduQbImportBlock> blocks = JSON.parseArray(raw, EduQbImportBlock.class);
            payload.setBlocks(blocks);
            payload.setPreviewHtml(EduQbDocxParseService.buildPreviewHtml(blocks));
            return payload;
        }
        com.alibaba.fastjson2.JSONObject obj = JSON.parseObject(raw);
        List<EduQbImportBlock> blocks = obj.getList("blocks", EduQbImportBlock.class);
        payload.setBlocks(blocks != null ? blocks : new ArrayList<>());
        String previewHtml = obj.getString("previewHtml");
        if (StringUtils.isEmpty(previewHtml))
        {
            previewHtml = EduQbDocxParseService.buildPreviewHtml(payload.getBlocks());
        }
        payload.setPreviewHtml(previewHtml);
        return payload;
    }

    private String buildParseResultJson(List<EduQbImportBlock> blocks, String previewHtml)
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("version", 2);
        payload.put("blocks", blocks);
        payload.put("previewHtml", previewHtml);
        return JSON.toJSONString(payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int commitImport(EduQbImportCommitBody body, String operator)
    {
        validateCommitBody(body);
        EduQbImportTask task = getTask(body.getTaskId(), operator);
        if (EduQbImportTask.STATUS_DONE.equals(task.getStatus()))
        {
            throw new ServiceException("\u8be5\u5bfc\u5165\u4efb\u52a1\u5df2\u5b8c\u6210\uff0c\u4e0d\u53ef\u91cd\u590d\u63d0\u4ea4");
        }
        if (subjectMapper.selectEduSubjectById(body.getSubjectId()) == null)
        {
            throw new ServiceException("\u5b66\u79d1\u4e0d\u5b58\u5728");
        }
        task.setStatus(EduQbImportTask.STATUS_IMPORTING);
        importTaskMapper.updateEduQbImportTask(task);

        try
        {
            return doCommitImport(body, operator, task);
        }
        catch (RuntimeException ex)
        {
            importTaskHelper.markFailed(task.getTaskId());
            throw ex;
        }
    }

    private int doCommitImport(EduQbImportCommitBody body, String operator, EduQbImportTask task)
    {
        int count = 0;
        for (EduQbImportCommitItem item : body.getItems())
        {
            String rawContent = item.getContent();
            if (StringUtils.isEmpty(rawContent))
            {
                continue;
            }
            String itemType = StringUtils.isNotEmpty(item.getQuestionType()) ? item.getQuestionType() : body.getQuestionType();
            ParsedImportContent parsed = EduQbImportContentSupport.parseContent(rawContent.trim());
            String content = StringUtils.isNotEmpty(parsed.getStem()) ? parsed.getStem() : rawContent.trim();
            String options = EduQbImportContentSupport.resolveOptionsJson(item, content);
            if (StringUtils.isEmpty(options))
            {
                String merged = rawContent.trim();
                if (StringUtils.isNotEmpty(item.getOptionsText()))
                {
                    merged = merged + "\n" + item.getOptionsText().trim();
                }
                ParsedImportContent mergedParsed = EduQbImportContentSupport.parseContent(merged);
                options = mergedParsed.getOptionsJson();
                if (StringUtils.isNotEmpty(mergedParsed.getStem()))
                {
                    content = mergedParsed.getStem();
                }
            }
            if (isChoiceType(itemType) && StringUtils.isEmpty(options))
            {
                throw new ServiceException("\u7b2c " + (count + 1) + " \u9898\u7f3a\u5c11\u9009\u9879\uff0c\u8bf7\u5728\u53f3\u4fa7\u586b\u5199\u9009\u9879\u540e\u91cd\u8bd5");
            }
            EduQbQuestion question = new EduQbQuestion();
            question.setSubjectId(body.getSubjectId());
            question.setChapterId(item.getChapterId() != null ? item.getChapterId() : body.getChapterId());
            question.setChapterText(StringUtils.isNotEmpty(item.getChapterText()) ? item.getChapterText() : body.getChapterText());
            question.setKnowledgePoints(body.getKnowledgePoints());
            question.setDifficulty(body.getDifficulty() != null ? body.getDifficulty() : new BigDecimal("0.50"));
            question.setQuestionType(itemType);
            question.setContent(content);
            question.setOptions(options);
            question.setSourceType(EduQbConstants.SOURCE_DOCX);
            question.setImportTaskId(task.getTaskId());
            if (StringUtils.isNotEmpty(item.getImages()))
            {
                question.setImages(item.getImages().trim());
            }
            String answer = StringUtils.isNotEmpty(item.getCorrectAnswer())
                    ? item.getCorrectAnswer().trim()
                    : buildDefaultAnswer(itemType, content);
            question.setCorrectAnswer(answer);
            if (StringUtils.isNotEmpty(item.getAnalysis()))
            {
                question.setAnalysis(item.getAnalysis().trim());
            }
            questionService.insertEduQbQuestion(question, operator);
            count++;
        }
        if (count == 0)
        {
            throw new ServiceException("\u672a\u5bfc\u5165\u4efb\u4f55\u8bd5\u9898");
        }
        task.setStatus(EduQbImportTask.STATUS_DONE);
        task.setImportedCount(count);
        task.setSubjectId(body.getSubjectId());
        importTaskMapper.updateEduQbImportTask(task);
        return count;
    }

    private void validateCommitBody(EduQbImportCommitBody body)
    {
        if (body == null || body.getTaskId() == null)
        {
            throw new ServiceException("\u5bfc\u5165\u4efb\u52a1\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (body.getSubjectId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u5b66\u79d1");
        }
        if (StringUtils.isEmpty(body.getChapterText()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u7ae0\u8282");
        }
        if (StringUtils.isEmpty(body.getKnowledgePoints()))
        {
            throw new ServiceException("\u8bf7\u81f3\u5c11\u6dfb\u52a0\u4e00\u4e2a\u77e5\u8bc6\u70b9");
        }
        if (StringUtils.isEmpty(body.getQuestionType()))
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u9898\u578b");
        }
        if (body.getItems() == null || body.getItems().isEmpty())
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u8981\u5bfc\u5165\u7684\u6bb5\u843d");
        }
    }

    private String buildDefaultAnswer(String questionType, String content)
    {
        if (EduQbConstants.TYPE_JUDGE.equals(questionType))
        {
            return JSON.toJSONString("true");
        }
        if (EduQbConstants.TYPE_FILL.equals(questionType) || EduQbConstants.TYPE_SHORT.equals(questionType))
        {
            return JSON.toJSONString(content.length() > 200 ? content.substring(0, 200) : content);
        }
        return JSON.toJSONString("A");
    }

    private boolean isChoiceType(String questionType)
    {
        return EduQbConstants.TYPE_SINGLE.equals(questionType) || EduQbConstants.TYPE_MULTI.equals(questionType);
    }

    private void assertTaskOwner(EduQbImportTask task, String operator)
    {
        if (EduQbSecuritySupport.isQuestionBankManager())
        {
            return;
        }
        if (task.getCreateBy() != null && operator != null && !task.getCreateBy().equals(operator))
        {
            throw new ServiceException("\u60a8\u6ca1\u6709\u6743\u9650\u67e5\u770b\u6b64\u5bfc\u5165\u4efb\u52a1");
        }
    }
}
