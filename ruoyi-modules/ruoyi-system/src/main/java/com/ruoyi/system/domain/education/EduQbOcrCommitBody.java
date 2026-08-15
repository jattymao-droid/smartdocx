package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduQbOcrCommitBody
{
    private Long draftId;
    private Long subjectId;
    private Long chapterId;
    private String chapterText;
    private String knowledgePoints;
    private BigDecimal difficulty;
    private String questionType;
    private String content;
    private String options;
    private String correctAnswer;
    private String analysis;

    /** JSON array of image paths, e.g. manually cropped figure */
    private String images;

    /** Explicit override when draft was produced by stub OCR provider. */
    private Boolean forceStub;

    public Long getDraftId()
    {
        return draftId;
    }

    public void setDraftId(Long draftId)
    {
        this.draftId = draftId;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public Long getChapterId()
    {
        return chapterId;
    }

    public void setChapterId(Long chapterId)
    {
        this.chapterId = chapterId;
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

    public String getAnalysis()
    {
        return analysis;
    }

    public void setAnalysis(String analysis)
    {
        this.analysis = analysis;
    }

    public String getImages()
    {
        return images;
    }

    public void setImages(String images)
    {
        this.images = images;
    }

    public Boolean getForceStub()
    {
        return forceStub;
    }

    public void setForceStub(Boolean forceStub)
    {
        this.forceStub = forceStub;
    }
}
