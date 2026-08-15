package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduQbQuestion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long questionId;
    private String questionCode;
    private String content;
    private String options;
    private String correctAnswer;
    private Long subjectId;
    private String subjectName;
    private Long chapterId;
    private Long textbookId;
    private String chapterText;
    private String knowledgePoints;
    private BigDecimal difficulty;
    private String questionType;
    private String sourceType;
    private String status;
    private String images;
    private String analysis;
    private String contentHash;
    private Long importTaskId;
    private String delFlag;

    /** query-only */
    private BigDecimal difficultyMin;
    private BigDecimal difficultyMax;
    private String knowledgePoint;
    private String keyword;

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }

    public String getQuestionCode()
    {
        return questionCode;
    }

    public void setQuestionCode(String questionCode)
    {
        this.questionCode = questionCode;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getOptions()
    {
        return options;
    }

    public void setOptions(String options)
    {
        this.options = options;
    }

    public String getCorrectAnswer()
    {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer)
    {
        this.correctAnswer = correctAnswer;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getSubjectName()
    {
        return subjectName;
    }

    public void setSubjectName(String subjectName)
    {
        this.subjectName = subjectName;
    }

    public Long getChapterId()
    {
        return chapterId;
    }

    public void setChapterId(Long chapterId)
    {
        this.chapterId = chapterId;
    }

    public Long getTextbookId()
    {
        return textbookId;
    }

    public void setTextbookId(Long textbookId)
    {
        this.textbookId = textbookId;
    }

    public String getChapterText()
    {
        return chapterText;
    }

    public void setChapterText(String chapterText)
    {
        this.chapterText = chapterText;
    }

    public String getKnowledgePoints()
    {
        return knowledgePoints;
    }

    public void setKnowledgePoints(String knowledgePoints)
    {
        this.knowledgePoints = knowledgePoints;
    }

    public BigDecimal getDifficulty()
    {
        return difficulty;
    }

    public void setDifficulty(BigDecimal difficulty)
    {
        this.difficulty = difficulty;
    }

    public String getQuestionType()
    {
        return questionType;
    }

    public void setQuestionType(String questionType)
    {
        this.questionType = questionType;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getImages()
    {
        return images;
    }

    public void setImages(String images)
    {
        this.images = images;
    }

    public String getAnalysis()
    {
        return analysis;
    }

    public void setAnalysis(String analysis)
    {
        this.analysis = analysis;
    }

    public String getContentHash()
    {
        return contentHash;
    }

    public void setContentHash(String contentHash)
    {
        this.contentHash = contentHash;
    }

    public Long getImportTaskId()
    {
        return importTaskId;
    }

    public void setImportTaskId(Long importTaskId)
    {
        this.importTaskId = importTaskId;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public BigDecimal getDifficultyMin()
    {
        return difficultyMin;
    }

    public void setDifficultyMin(BigDecimal difficultyMin)
    {
        this.difficultyMin = difficultyMin;
    }

    public BigDecimal getDifficultyMax()
    {
        return difficultyMax;
    }

    public void setDifficultyMax(BigDecimal difficultyMax)
    {
        this.difficultyMax = difficultyMax;
    }

    public String getKnowledgePoint()
    {
        return knowledgePoint;
    }

    public void setKnowledgePoint(String knowledgePoint)
    {
        this.knowledgePoint = knowledgePoint;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }
}
