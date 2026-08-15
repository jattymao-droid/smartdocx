package com.ruoyi.system.service.education.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbPracticeRecord;
import com.ruoyi.system.domain.education.EduQbPracticeSession;
import com.ruoyi.system.domain.education.EduQbPracticeStats;
import com.ruoyi.system.domain.education.EduQbPracticeSubmitBody;
import com.ruoyi.system.domain.education.EduQbPracticeSubmitItem;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.domain.education.EduQbSmartComposeQuestion;
import com.ruoyi.system.domain.education.EduQbSmartComposeResult;
import com.ruoyi.system.domain.education.EduQbWeakComposeRequest;
import com.ruoyi.system.domain.education.EduQbWeakPointStat;
import com.ruoyi.system.domain.education.EduQbWrongBook;
import com.ruoyi.system.domain.education.EduQbWrongBookBatchBody;
import com.ruoyi.system.domain.education.EduQbWrongBookStats;
import com.ruoyi.system.domain.education.EduQbWrongComposeRequest;
import com.ruoyi.system.domain.education.EduQbStudentPracticeCheckBody;
import com.ruoyi.system.domain.education.EduQbStudentPracticeCheckResult;
import com.ruoyi.system.domain.education.EduQbSmartComposeTypeRule;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.mapper.education.EduQbPracticeMapper;
import com.ruoyi.system.mapper.education.EduQbQuestionMapper;
import com.ruoyi.system.mapper.education.EduQbWrongBookMapper;
import com.ruoyi.system.service.education.IEduQbPaperService;
import com.ruoyi.system.service.education.IEduQbPracticeService;
import com.ruoyi.system.service.education.support.EduQbPracticeAnswerSupport;

@Service
public class EduQbPracticeServiceImpl implements IEduQbPracticeService
{
    private static final int DEFAULT_WEAK_LIMIT = 3;
    private static final int DEFAULT_WRONG_COMPOSE_LIMIT = 50;
    private static final BigDecimal DEFAULT_WRONG_SCORE = BigDecimal.valueOf(5);

    @Autowired
    private EduQbPracticeMapper practiceMapper;

    @Autowired
    private EduQbWrongBookMapper wrongBookMapper;

    @Autowired
    private EduQbQuestionMapper questionMapper;

    @Autowired
    private IEduQbPaperService paperService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitPractice(EduQbPracticeSubmitBody body, String userName)
    {
        if (body == null || body.getItems() == null || body.getItems().isEmpty())
        {
            throw new ServiceException("\u8bf7\u63d0\u4f9b\u7ec3\u4e60\u7ed3\u679c");
        }
        int total = body.getItems().size();
        int correct = 0;
        int choice = 0;
        int subjective = 0;
        for (EduQbPracticeSubmitItem item : body.getItems())
        {
            if (item == null)
            {
                continue;
            }
            if (Boolean.TRUE.equals(item.getSubjective()))
            {
                subjective++;
            }
            else
            {
                choice++;
                if (Boolean.TRUE.equals(item.getCorrect()))
                {
                    correct++;
                }
            }
        }

        EduQbPracticeSession session = new EduQbPracticeSession();
        session.setUserName(userName);
        session.setSubjectId(body.getSubjectId());
        session.setPaperTitle(StringUtils.isNotEmpty(body.getPaperTitle()) ? body.getPaperTitle() : "\u5728\u7ebf\u7ec3\u4e60");
        session.setShareId(body.getShareId());
        session.setTotalCount(total);
        session.setCorrectCount(correct);
        session.setChoiceCount(choice);
        session.setSubjectiveCount(subjective);
        session.setDurationSec(body.getDurationSec());
        practiceMapper.insertPracticeSession(session);

        Map<Long, EduQbQuestion> questionCache = new HashMap<>();
        for (EduQbPracticeSubmitItem item : body.getItems())
        {
            if (item == null || item.getQuestionId() == null)
            {
                continue;
            }
            EduQbQuestion question = resolveQuestion(item.getQuestionId(), questionCache);
            String correctFlag = resolveCorrectFlag(item);
            EduQbPracticeRecord record = new EduQbPracticeRecord();
            record.setSessionId(session.getSessionId());
            record.setUserName(userName);
            record.setQuestionId(item.getQuestionId());
            record.setSubjectId(question != null ? question.getSubjectId() : body.getSubjectId());
            record.setChapterId(question != null ? question.getChapterId() : null);
            record.setChapterText(question != null ? question.getChapterText() : null);
            record.setQuestionType(StringUtils.isNotEmpty(item.getQuestionType())
                    ? item.getQuestionType()
                    : (question != null ? question.getQuestionType() : null));
            record.setPickedAnswer(item.getPickedAnswer());
            record.setCorrectFlag(correctFlag);
            practiceMapper.insertPracticeRecord(record);

            if (EduQbPracticeRecord.WRONG.equals(correctFlag))
            {
                upsertWrongBook(userName, question, item, body.getSubjectId());
            }
            else if (EduQbPracticeRecord.CORRECT.equals(correctFlag))
            {
                markWrongMasteredIfExists(userName, item.getQuestionId());
            }
        }
        return session.getSessionId();
    }

    @Override
    public EduQbStudentPracticeCheckResult checkPracticeAnswer(EduQbStudentPracticeCheckBody body)
    {
        if (body == null || body.getQuestionId() == null)
        {
            throw new ServiceException("\u8bf7\u63d0\u4f9b\u9898\u76ee");
        }
        EduQbQuestion question = questionMapper.selectEduQbQuestionById(body.getQuestionId());
        if (question == null || !"0".equals(question.getDelFlag()))
        {
            throw new ServiceException("\u8bd5\u9898\u4e0d\u5b58\u5728");
        }
        if (!EduQbConstants.STATUS_APPROVED.equals(question.getStatus()))
        {
            throw new ServiceException("\u8bd5\u9898\u672a\u901a\u8fc7\u5ba1\u6838");
        }
        String questionType = StringUtils.isNotEmpty(body.getQuestionType())
                ? body.getQuestionType()
                : question.getQuestionType();
        boolean subjective = Boolean.TRUE.equals(body.getSubjective())
                || EduQbPracticeAnswerSupport.isSubjectiveType(questionType);
        EduQbStudentPracticeCheckResult result = new EduQbStudentPracticeCheckResult();
        result.setSubjective(subjective);
        if (subjective)
        {
            result.setCorrect(Boolean.TRUE.equals(body.getSelfCorrect()));
            result.setCorrectAnswer(EduQbPracticeAnswerSupport.formatCorrectAnswerDisplay(
                    questionType, question.getCorrectAnswer()));
            result.setAnalysis(question.getAnalysis());
            return result;
        }
        boolean correct = EduQbPracticeAnswerSupport.evaluate(
                questionType, body.getPickedAnswer(), question.getCorrectAnswer());
        result.setCorrect(correct);
        result.setCorrectAnswer(EduQbPracticeAnswerSupport.formatCorrectAnswerDisplay(
                questionType, question.getCorrectAnswer()));
        result.setAnalysis(question.getAnalysis());
        return result;
    }

    @Override
    public List<EduQbPracticeSession> selectPracticeSessionList(EduQbPracticeSession query, String userName)
    {
        EduQbPracticeSession q = query != null ? query : new EduQbPracticeSession();
        q.setUserName(userName);
        return practiceMapper.selectPracticeSessionList(q);
    }

    @Override
    public EduQbPracticeStats selectPracticeStats(Long subjectId, String userName)
    {
        EduQbPracticeStats stats = practiceMapper.selectPracticeStats(userName, subjectId);
        if (stats == null)
        {
            stats = new EduQbPracticeStats();
            stats.setSessionCount(0);
            stats.setTotalQuestions(0);
            stats.setTotalCorrect(0);
            stats.setTotalChoice(0);
        }
        int choice = stats.getTotalChoice() != null ? stats.getTotalChoice() : 0;
        int correct = stats.getTotalCorrect() != null ? stats.getTotalCorrect() : 0;
        if (choice > 0)
        {
            stats.setAvgChoiceRate(BigDecimal.valueOf(correct)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(choice), 1, java.math.RoundingMode.HALF_UP));
        }
        else
        {
            stats.setAvgChoiceRate(BigDecimal.ZERO);
        }
        return stats;
    }

    @Override
    public Map<String, Object> getPracticeSessionDetail(Long sessionId, String userName)
    {
        EduQbPracticeSession session = practiceMapper.selectPracticeSessionById(sessionId, userName);
        if (session == null)
        {
            throw new ServiceException("\u7ec3\u4e60\u8bb0\u5f55\u4e0d\u5b58\u5728");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("session", session);
        data.put("records", practiceMapper.selectPracticeRecordBySessionId(sessionId));
        return data;
    }

    @Override
    public List<EduQbWrongBook> selectWrongBookList(EduQbWrongBook query, String userName)
    {
        EduQbWrongBook q = query != null ? query : new EduQbWrongBook();
        q.setUserName(userName);
        if (StringUtils.isEmpty(q.getMastered()))
        {
            q.setMastered("0");
        }
        return wrongBookMapper.selectWrongBookList(q);
    }

    @Override
    public EduQbWrongBookStats selectWrongBookStats(Long subjectId, String userName)
    {
        EduQbWrongBookStats stats = wrongBookMapper.selectWrongBookStats(userName, subjectId);
        if (stats == null)
        {
            stats = new EduQbWrongBookStats();
            stats.setActiveCount(0);
            stats.setMasteredCount(0);
            stats.setTotalWrongAttempts(0);
        }
        return stats;
    }

    @Override
    public int markWrongMastered(Long wrongId, String userName)
    {
        return wrongBookMapper.markMastered(wrongId, userName);
    }

    @Override
    public int restoreWrong(Long wrongId, String userName)
    {
        return wrongBookMapper.restoreWrong(wrongId, userName);
    }

    @Override
    public int deleteWrongBook(Long wrongId, String userName)
    {
        return wrongBookMapper.deleteWrongBook(wrongId, userName);
    }

    @Override
    public int batchMarkWrongMastered(EduQbWrongBookBatchBody body, String userName)
    {
        if (body == null || body.getWrongIds() == null || body.getWrongIds().isEmpty())
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u9519\u9898\u8bb0\u5f55");
        }
        return wrongBookMapper.batchMarkMastered(body.getWrongIds(), userName);
    }

    @Override
    public int batchDeleteWrongBook(EduQbWrongBookBatchBody body, String userName)
    {
        if (body == null || body.getWrongIds() == null || body.getWrongIds().isEmpty())
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u9519\u9898\u8bb0\u5f55");
        }
        return wrongBookMapper.batchDeleteWrongBook(body.getWrongIds(), userName);
    }

    @Override
    public List<EduQbWeakPointStat> selectWeakPointStats(Long subjectId, int limit, String userName)
    {
        if (subjectId == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u5b66\u79d1");
        }
        int safeLimit = limit > 0 ? Math.min(limit, 20) : DEFAULT_WEAK_LIMIT;
        return practiceMapper.selectWeakPointStats(userName, subjectId, safeLimit);
    }

    @Override
    public EduQbSmartComposeResult weakCompose(EduQbWeakComposeRequest request, String userName)
    {
        if (request == null || request.getSubjectId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u5b66\u79d1");
        }
        int chapterLimit = request.getWeakChapterLimit() != null && request.getWeakChapterLimit() > 0
                ? Math.min(request.getWeakChapterLimit(), 10)
                : DEFAULT_WEAK_LIMIT;
        List<EduQbWeakPointStat> weakPoints = practiceMapper.selectWeakPointStats(
                userName, request.getSubjectId(), chapterLimit);
        if (weakPoints.isEmpty())
        {
            throw new ServiceException("\u6682\u65e0\u7ec3\u4e60\u9519\u9898\u6570\u636e\uff0c\u8bf7\u5148\u5b8c\u6210\u5728\u7ebf\u7ec3\u4e60");
        }
        List<Long> chapterIds = weakPoints.stream()
                .map(EduQbWeakPointStat::getChapterId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        request.setChapterIds(chapterIds.isEmpty() ? null : chapterIds);
        if (StringUtils.isEmpty(request.getPaperTitle()))
        {
            request.setPaperTitle("\u8584\u5f31\u70b9\u9488\u5bf9\u7ec3\u4e60\u5377");
        }
        if (request.getTypeRules() == null || request.getTypeRules().isEmpty())
        {
            request.setTypeRules(defaultWeakComposeRules());
        }
        List<Long> excludeIds = wrongBookMapper.selectActiveWrongQuestionIds(userName, request.getSubjectId());
        if (excludeIds != null && !excludeIds.isEmpty())
        {
            if (request.getExcludeQuestionIds() == null)
            {
                request.setExcludeQuestionIds(new ArrayList<>());
            }
            request.getExcludeQuestionIds().addAll(excludeIds);
        }
        EduQbSmartComposeResult result = paperService.smartCompose(request);
        result.setWarnings(appendWeakPointWarnings(result.getWarnings(), weakPoints));
        return result;
    }

    @Override
    public EduQbSmartComposeResult wrongCompose(EduQbWrongComposeRequest request, String userName)
    {
        if (request == null || request.getSubjectId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u5b66\u79d1");
        }
        int limit = request.getLimit() != null && request.getLimit() > 0
                ? Math.min(request.getLimit(), 100)
                : DEFAULT_WRONG_COMPOSE_LIMIT;
        List<Long> questionIds = request.getQuestionIds();
        List<EduQbWrongBook> wrongRows = wrongBookMapper.selectActiveWrongBooks(
                userName, request.getSubjectId(),
                questionIds != null && !questionIds.isEmpty() ? questionIds : null,
                limit);
        if (wrongRows.isEmpty())
        {
            throw new ServiceException("\u6682\u65e0\u5f85\u590d\u4e60\u9519\u9898");
        }

        EduQbSmartComposeResult result = new EduQbSmartComposeResult();
        result.setPaperTitle(StringUtils.isNotEmpty(request.getPaperTitle())
                ? request.getPaperTitle()
                : "\u9519\u9898\u91cd\u7ec3\u5377");
        List<EduQbSmartComposeQuestion> composed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int order = 1;
        int skipped = 0;
        for (EduQbWrongBook row : wrongRows)
        {
            EduQbQuestion question = questionMapper.selectEduQbQuestionById(row.getQuestionId());
            if (question == null || !"0".equals(question.getDelFlag()) || !"0".equals(question.getStatus()))
            {
                skipped++;
                continue;
            }
            composed.add(toWrongComposeQuestion(question, order++));
        }
        if (composed.isEmpty())
        {
            throw new ServiceException("\u9519\u9898\u5df2\u4e0b\u67b6\u6216\u4e0d\u53ef\u7528\uff0c\u65e0\u6cd5\u7ec4\u5377");
        }
        if (skipped > 0)
        {
            warnings.add("\u5df2\u8df3\u8fc7 " + skipped + " \u9053\u4e0d\u53ef\u7528\u9898\u76ee");
        }
        if (wrongRows.size() > composed.size() + skipped)
        {
            warnings.add("\u5df2\u6309\u6700\u8fd1\u9519\u8bef\u65f6\u95f4\u53d6\u524d " + composed.size() + " \u9898");
        }
        result.setQuestions(composed);
        result.setTotalScore(composed.stream()
                .map(EduQbSmartComposeQuestion::getScoreValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setWarnings(warnings);
        return result;
    }

    private EduQbSmartComposeQuestion toWrongComposeQuestion(EduQbQuestion question, int orderNum)
    {
        EduQbSmartComposeQuestion item = new EduQbSmartComposeQuestion();
        item.setQuestionId(question.getQuestionId());
        item.setQuestionCode(question.getQuestionCode());
        item.setContent(question.getContent());
        item.setQuestionType(question.getQuestionType());
        item.setDifficulty(question.getDifficulty());
        item.setOptions(question.getOptions());
        item.setImages(question.getImages());
        item.setScoreValue(DEFAULT_WRONG_SCORE);
        item.setOrderNum(orderNum);
        return item;
    }

    private EduQbQuestion resolveQuestion(Long questionId, Map<Long, EduQbQuestion> cache)
    {
        if (questionId == null)
        {
            return null;
        }
        if (cache.containsKey(questionId))
        {
            return cache.get(questionId);
        }
        EduQbQuestion question = questionMapper.selectEduQbQuestionById(questionId);
        cache.put(questionId, question);
        return question;
    }

    private String resolveCorrectFlag(EduQbPracticeSubmitItem item)
    {
        if (Boolean.TRUE.equals(item.getSubjective()))
        {
            return EduQbPracticeRecord.SUBJECTIVE;
        }
        if (Boolean.TRUE.equals(item.getCorrect()))
        {
            return EduQbPracticeRecord.CORRECT;
        }
        return EduQbPracticeRecord.WRONG;
    }

    private void upsertWrongBook(String userName, EduQbQuestion question, EduQbPracticeSubmitItem item, Long fallbackSubjectId)
    {
        EduQbWrongBook row = new EduQbWrongBook();
        row.setUserName(userName);
        row.setQuestionId(item.getQuestionId());
        row.setSubjectId(question != null ? question.getSubjectId() : fallbackSubjectId);
        row.setChapterId(question != null ? question.getChapterId() : null);
        row.setChapterText(question != null ? question.getChapterText() : null);
        row.setQuestionType(StringUtils.isNotEmpty(item.getQuestionType())
                ? item.getQuestionType()
                : (question != null ? question.getQuestionType() : null));
        wrongBookMapper.upsertWrongBook(row);
    }

    private void markWrongMasteredIfExists(String userName, Long questionId)
    {
        EduQbWrongBook existing = wrongBookMapper.selectWrongBookByUserQuestion(userName, questionId);
        if (existing != null && "0".equals(existing.getMastered()))
        {
            wrongBookMapper.markMastered(existing.getWrongId(), userName);
        }
    }

    private List<String> appendWeakPointWarnings(List<String> warnings, List<EduQbWeakPointStat> weakPoints)
    {
        List<String> merged = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        StringBuilder hint = new StringBuilder("\u9488\u5bf9\u8584\u5f31\u7ae0\u8282\uff1a");
        for (int i = 0; i < weakPoints.size() && i < 3; i++)
        {
            EduQbWeakPointStat stat = weakPoints.get(i);
            if (i > 0)
            {
                hint.append("\u3001");
            }
            hint.append(stat.getChapterText()).append("(").append(stat.getWrongCount()).append("\u9898)");
        }
        merged.add(0, hint.toString());
        return merged;
    }

    private List<EduQbSmartComposeTypeRule> defaultWeakComposeRules()
    {
        List<EduQbSmartComposeTypeRule> rules = new ArrayList<>();
        rules.add(buildRule("single", 5, 3));
        rules.add(buildRule("multi", 2, 4));
        rules.add(buildRule("fill", 2, 4));
        return rules;
    }

    private EduQbSmartComposeTypeRule buildRule(String type, int count, int score)
    {
        EduQbSmartComposeTypeRule rule = new EduQbSmartComposeTypeRule();
        rule.setQuestionType(type);
        rule.setCount(count);
        rule.setScorePerQuestion(BigDecimal.valueOf(score));
        return rule;
    }
}
