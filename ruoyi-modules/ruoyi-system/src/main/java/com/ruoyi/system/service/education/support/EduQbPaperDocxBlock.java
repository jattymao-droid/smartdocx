package com.ruoyi.system.service.education.support;

import java.math.BigDecimal;
import java.util.List;

public class EduQbPaperDocxBlock
{
    private String sectionTitle;
    private int questionNo;
    private String content;
    private BigDecimal scoreValue;
    private List<String> options;
    private List<String> imageUrls;
    private String answerLine;

    public String getSectionTitle()
    {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle)
    {
        this.sectionTitle = sectionTitle;
    }

    public int getQuestionNo()
    {
        return questionNo;
    }

    public void setQuestionNo(int questionNo)
    {
        this.questionNo = questionNo;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public BigDecimal getScoreValue()
    {
        return scoreValue;
    }

    public void setScoreValue(BigDecimal scoreValue)
    {
        this.scoreValue = scoreValue;
    }

    public List<String> getOptions()
    {
        return options;
    }

    public void setOptions(List<String> options)
    {
        this.options = options;
    }

    public List<String> getImageUrls()
    {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls)
    {
        this.imageUrls = imageUrls;
    }

    public String getAnswerLine()
    {
        return answerLine;
    }

    public void setAnswerLine(String answerLine)
    {
        this.answerLine = answerLine;
    }
}
