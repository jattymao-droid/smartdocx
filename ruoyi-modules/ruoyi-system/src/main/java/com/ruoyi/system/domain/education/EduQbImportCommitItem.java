package com.ruoyi.system.domain.education;

import java.util.List;

public class EduQbImportCommitItem
{
    private String content;
    private List<Integer> blockIds;
    /** JSON array of image paths from selected blocks */
    private String images;
    /** JSON array string, same as OCR import */
    private String options;
    /** Plain-text options, one per line (fallback) */
    private String optionsText;
    /** JSON-encoded correct answer */
    private String correctAnswer;
    /** Question analysis / explanation */
    private String analysis;
    /** Per-item chapter override */
    private Long chapterId;
    private String chapterText;
    /** Per-item question type; falls back to batch default when empty */
    private String questionType;

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public List<Integer> getBlockIds()
    {
        return blockIds;
    }

    public void setBlockIds(List<Integer> blockIds)
    {
        this.blockIds = blockIds;
    }

    public String getImages()
    {
        return images;
    }

    public void setImages(String images)
    {
        this.images = images;
    }

    public String getOptions()
    {
        return options;
    }

    public void setOptions(String options)
    {
        this.options = options;
    }

    public String getOptionsText()
    {
        return optionsText;
    }

    public void setOptionsText(String optionsText)
    {
        this.optionsText = optionsText;
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

    public String getQuestionType()
    {
        return questionType;
    }

    public void setQuestionType(String questionType)
    {
        this.questionType = questionType;
    }
}
