package com.ruoyi.system.service.education.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbExamPaperMarkItem;
import com.ruoyi.system.domain.education.EduQbImportBlock;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.mapper.education.EduQbQuestionMapper;
import com.ruoyi.system.service.education.support.EduQbExamPaperAnswerSupport.ParsedAnswer;
import com.ruoyi.system.service.education.support.EduQbQuestionPredictService;
import com.ruoyi.system.service.education.support.EduQbImportContentSupport.ParsedImportContent;

public final class EduQbExamPaperBlockAnalyzer
{
    private static final Pattern SECTION_HEADER = Pattern.compile(
            "^[\\s\\u3000]*([\\u4e00-\\u9fa5]+[\u3001\uff0e\\.]|\\u7b2c[\\u4e00-\\u9fa5\\d]+[\u90e8\u5206\u8282\u9898\u5377][\uff1a:]?|\u3010[^\u3011]{1,20}\u3011)[\\s\\u3000]*$");
    private static final Pattern QUESTION_START = Pattern.compile(
            "^[\\s\\u3000]*(?:\\(\\d+\\)|\uff08\\d+\uff09|\\d+[\\.\\uff0e\\u3001\\)\\uff09]|\u7b2c\\d+\u9898)");
    private static final Pattern SCORE = Pattern.compile("(?:\\(|\\uff08|\\[|\u3010)?(\\d{1,2})(?:\\s*\u5206)(?:\\)|\\uff09|\\]|\u3011)?");
    private static final Pattern JUDGE_HINT = Pattern.compile("(\u5224\u65ad\u9898|\u6b63\u786e|\u9519\u8bef|\u5bf9\u9519|\u221a|\u00d7)");
    private static final Pattern FILL_HINT = Pattern.compile("(_{2,}|\uff3f{2,}|\\(\\s*\\)|\uff08\\s*\uff09|\u586b\u7a7a)");
    private static final Pattern OPTION_LINE = Pattern.compile(
            "^[A-Ha-d][\\.\\uFF0E\\u3001\\u3002\\)\\uFF09:\\uFF1A]\\s*|^[A-Ha-d]\\s+\\S");

    private EduQbExamPaperBlockAnalyzer()
    {
    }

    public static List<EduQbExamPaperMarkItem> analyze(List<EduQbImportBlock> blocks, Long subjectId,
            EduQbQuestionMapper questionMapper)
    {
        List<EduQbExamPaperMarkItem> items = new ArrayList<>();
        if (blocks == null || blocks.isEmpty())
        {
            return items;
        }
        String currentSection = null;
        int questionOrder = 0;
        boolean inAnswerSection = false;
        List<EduQbImportBlock> answerBlocks = new ArrayList<>();
        for (EduQbImportBlock block : blocks)
        {
            String raw = buildBlockText(block);
            if (StringUtils.isEmpty(raw) && (block.getImageUrls() == null || block.getImageUrls().isEmpty()))
            {
                continue;
            }
            if (EduQbExamPaperAnswerSupport.isAnswerSectionHeader(raw))
            {
                inAnswerSection = true;
                continue;
            }
            if (inAnswerSection)
            {
                answerBlocks.add(block);
                continue;
            }
            if (isSectionHeader(raw))
            {
                currentSection = StringUtils.trim(raw);
                EduQbExamPaperMarkItem section = baseItem(block, currentSection);
                section.setQuestion(false);
                section.setIncluded(false);
                section.setSectionName(currentSection);
                items.add(section);
                continue;
            }
            if (!looksLikeQuestion(raw, block))
            {
                if (!items.isEmpty())
                {
                    EduQbExamPaperMarkItem prev = items.get(items.size() - 1);
                    if (prev.isQuestion())
                    {
                        mergeBlockInto(prev, block, raw);
                        refreshParsedFields(prev, subjectId, questionMapper);
                        continue;
                    }
                }
                continue;
            }
            questionOrder++;
            EduQbExamPaperMarkItem item = baseItem(block, raw);
            item.setQuestion(true);
            item.setOrderNum(questionOrder);
            item.setSectionName(currentSection);
            applyImages(item, block);
            refreshParsedFields(item, subjectId, questionMapper);
            items.add(item);
        }
        applyAnswerSection(items, answerBlocks);
        return items;
    }

    private static void applyAnswerSection(List<EduQbExamPaperMarkItem> items, List<EduQbImportBlock> answerBlocks)
    {
        if (items == null || items.isEmpty() || answerBlocks == null || answerBlocks.isEmpty())
        {
            return;
        }
        Map<Integer, ParsedAnswer> answerMap = EduQbExamPaperAnswerSupport.parseAnswerBlocks(answerBlocks);
        if (answerMap.isEmpty())
        {
            return;
        }
        for (EduQbExamPaperMarkItem item : items)
        {
            if (item == null || !item.isQuestion())
            {
                continue;
            }
            ParsedAnswer parsed = answerMap.get(item.getOrderNum());
            if (parsed == null)
            {
                continue;
            }
            if (StringUtils.isEmpty(item.getCorrectAnswer()) && StringUtils.isNotEmpty(parsed.getAnswerRaw()))
            {
                item.setCorrectAnswer(EduQbExamPaperAnswerSupport.formatAnswerJson(
                        parsed.getAnswerRaw(), item.getQuestionType()));
            }
            if (StringUtils.isEmpty(item.getAnalysis()) && StringUtils.isNotEmpty(parsed.getAnalysis()))
            {
                item.setAnalysis(parsed.getAnalysis());
            }
        }
    }

    private static EduQbExamPaperMarkItem baseItem(EduQbImportBlock block, String raw)
    {
        EduQbExamPaperMarkItem item = new EduQbExamPaperMarkItem();
        item.setBlockId(block.getBlockId());
        item.setOrderNum(block.getOrderNum());
        item.setContent(StringUtils.trim(raw));
        item.setIncluded(true);
        return item;
    }

    private static void mergeBlockInto(EduQbExamPaperMarkItem target, EduQbImportBlock block, String raw)
    {
        if (StringUtils.isNotEmpty(raw))
        {
            target.setContent(StringUtils.trim(target.getContent() + "\n" + raw));
        }
        if (block.getImageUrls() != null && !block.getImageUrls().isEmpty())
        {
            List<String> urls = new ArrayList<>();
            if (StringUtils.isNotEmpty(target.getImages()))
            {
                try
                {
                    urls.addAll(JSON.parseArray(target.getImages(), String.class));
                }
                catch (Exception ignored)
                {
                }
            }
            urls.addAll(block.getImageUrls());
            target.setImages(limitImageJson(urls));
        }
    }

    private static String limitImageJson(List<String> urls)
    {
        if (urls == null || urls.isEmpty())
        {
            return null;
        }
        List<String> distinct = new ArrayList<>();
        for (String url : urls)
        {
            if (StringUtils.isNotEmpty(url) && !distinct.contains(url))
            {
                distinct.add(url);
            }
        }
        if (distinct.size() > EduQbConstants.MAX_QUESTION_IMAGES)
        {
            distinct = distinct.subList(0, EduQbConstants.MAX_QUESTION_IMAGES);
        }
        return JSON.toJSONString(distinct);
    }

    private static void applyImages(EduQbExamPaperMarkItem item, EduQbImportBlock block)
    {
        if (block.getImageUrls() != null && !block.getImageUrls().isEmpty())
        {
            item.setImages(limitImageJson(block.getImageUrls()));
        }
    }

    private static void refreshParsedFields(EduQbExamPaperMarkItem item, Long subjectId,
            EduQbQuestionMapper questionMapper)
    {
        String raw = item.getContent();
        ParsedImportContent parsed = EduQbImportContentSupport.parseContent(raw);
        String stem = StringUtils.isNotEmpty(parsed.getStem()) ? parsed.getStem() : raw;
        item.setContent(stem);
        item.setOptions(parsed.getOptionsJson());
        if (StringUtils.isNotEmpty(parsed.getCorrectAnswer()))
        {
            item.setCorrectAnswer(parsed.getCorrectAnswer());
        }
        if (StringUtils.isNotEmpty(parsed.getAnalysis()))
        {
            item.setAnalysis(parsed.getAnalysis());
        }
        item.setScoreValue(extractScore(raw));
        item.setQuestionType(detectQuestionType(raw, parsed.getOptionsJson(), item.getSectionName(), item.getCorrectAnswer()));
        if (subjectId != null && questionMapper != null && StringUtils.isNotEmpty(stem))
        {
            String hash = EduQbContentHashSupport.computeHash(stem);
            List<EduQbQuestion> exact = questionMapper.selectByContentHash(subjectId, hash, null);
            if (exact != null && !exact.isEmpty())
            {
                EduQbQuestion matched = exact.get(0);
                item.setMatchStatus("existing");
                item.setMatchedQuestionId(matched.getQuestionId());
                item.setMatchedQuestionCode(matched.getQuestionCode());
                item.setQuestionType(matched.getQuestionType());
                if (StringUtils.isEmpty(item.getCorrectAnswer()) && StringUtils.isNotEmpty(matched.getCorrectAnswer()))
                {
                    item.setCorrectAnswer(matched.getCorrectAnswer());
                }
                if (StringUtils.isEmpty(item.getAnalysis()) && StringUtils.isNotEmpty(matched.getAnalysis()))
                {
                    item.setAnalysis(matched.getAnalysis());
                }
            }
            else
            {
                item.setMatchStatus("new");
            }
        }
        else
        {
            item.setMatchStatus("new");
        }
    }

    private static String buildBlockText(EduQbImportBlock block)
    {
        return block == null ? "" : StringUtils.trim(block.getText());
    }

    private static boolean isSectionHeader(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return false;
        }
        String oneLine = text.replace('\n', ' ').trim();
        if (oneLine.length() > 40)
        {
            return false;
        }
        return SECTION_HEADER.matcher(oneLine).find()
                || oneLine.matches("[\\u4e00-\\u9fa5]+[\u3001].*\u9898.*")
                || oneLine.matches("\u7b2c[\\u4e00-\\u9fa5\\d]+\u90e8\u5206.*");
    }

    private static boolean looksLikeQuestion(String text, EduQbImportBlock block)
    {
        if (StringUtils.isEmpty(text) && block != null && block.getImageUrls() != null
                && !block.getImageUrls().isEmpty())
        {
            return true;
        }
        if (StringUtils.isEmpty(text))
        {
            return false;
        }
        String firstLine = text.split("\\r?\\n")[0].trim();
        if (QUESTION_START.matcher(firstLine).find())
        {
            return true;
        }
        if (hasOptions(text))
        {
            return true;
        }
        if (FILL_HINT.matcher(text).find())
        {
            return true;
        }
        if (JUDGE_HINT.matcher(text).find() && text.length() < 120)
        {
            return true;
        }
        return text.length() >= 12 && !isSectionHeader(text);
    }

    private static boolean hasOptions(String text)
    {
        int count = 0;
        for (String line : text.split("\\r?\\n"))
        {
            if (OPTION_LINE.matcher(line.trim()).find())
            {
                count++;
            }
        }
        return count >= 2;
    }

    private static BigDecimal extractScore(String text)
    {
        Matcher matcher = SCORE.matcher(text);
        if (matcher.find())
        {
            try
            {
                return new BigDecimal(matcher.group(1));
            }
            catch (Exception ignored)
            {
            }
        }
        return new BigDecimal("5");
    }

    private static String detectQuestionType(String text, String optionsJson, String sectionName, String correctAnswer)
    {
        int optionCount = 0;
        if (StringUtils.isNotEmpty(optionsJson))
        {
            try
            {
                optionCount = JSON.parseArray(optionsJson, String.class).size();
            }
            catch (Exception ignored)
            {
            }
        }
        if (optionCount < 2)
        {
            optionCount = EduQbQuestionPredictService.countOptionLines(text);
        }
        String type = EduQbQuestionPredictService.detectTypeFromText(text, optionCount, sectionName);
        if (EduQbConstants.TYPE_SINGLE.equals(type) && isMultiChoiceAnswer(correctAnswer))
        {
            return EduQbConstants.TYPE_MULTI;
        }
        return type;
    }

    private static boolean isMultiChoiceAnswer(String correctAnswer)
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
}
