package com.ruoyi.system.domain.education;

import java.util.ArrayList;
import java.util.List;

public class EduQbWrongBookReviewVO
{
    private Long wrongId;
    private Long questionId;
    private String questionType;
    private String content;
    private String options;
    private String images;
    private String correctAnswer;
    private String analysis;
    private Integer wrongCount;
    private String lastWrongAnswer;
    private String chapterText;

    public Long getWrongId()
    {
        return wrongId;
    }

    public void setWrongId(Long wrongId)
    {
        this.wrongId = wrongId;
    }

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
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

    public String getImages()
    {
        return images;
    }

    public void setImages(String images)
    {
        this.images = images;
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

    public Integer getWrongCount()
    {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount)
    {
        this.wrongCount = wrongCount;
    }

    public String getLastWrongAnswer()
    {
        return lastWrongAnswer;
    }

    public void setLastWrongAnswer(String lastWrongAnswer)
    {
        this.lastWrongAnswer = lastWrongAnswer;
    }

    public String getChapterText()
    {
        return chapterText;
    }

    public void setChapterText(String chapterText)
    {
        this.chapterText = chapterText;
    }
}
