package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Auto-marked question candidate from DOCX blocks. */
public class EduQbExamPaperMarkItem
{
    private int blockId;
    private int orderNum;
    private boolean question;
    private boolean included = true;
    private String sectionName;
    private String questionType;
    private BigDecimal scoreValue;
    private String content;
    private String options;
    private String images;
    private String correctAnswer;
    private String analysis;
    private String matchStatus;
    private Long matchedQuestionId;
    private String matchedQuestionCode;
    private Long textbookId;
    private Long chapterId;
    private String chapterText;
    private String knowledgePoints;

    public int getBlockId()
    {
        return blockId;
    }

    public void setBlockId(int blockId)
    {
        this.blockId = blockId;
    }

    public int getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(int orderNum)
    {
        this.orderNum = orderNum;
    }

    public boolean isQuestion()
    {
        return question;
    }

    public void setQuestion(boolean question)
    {
        this.question = question;
    }

    public boolean isIncluded()
    {
        return included;
    }

    public void setIncluded(boolean included)
    {
        this.included = included;
    }

    public String getSectionName()
    {
        return sectionName;
    }

    public void setSectionName(String sectionName)
    {
        this.sectionName = sectionName;
    }

    public String getQuestionType()
    {
        return questionType;
    }

    public void setQuestionType(String questionType)
    {
        this.questionType = questionType;
    }

    public BigDecimal getScoreValue()
    {
        return scoreValue;
    }

    public void setScoreValue(BigDecimal scoreValue)
    {
        this.scoreValue = scoreValue;
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

    public String getMatchStatus()
    {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus)
    {
        this.matchStatus = matchStatus;
    }

    public Long getMatchedQuestionId()
    {
        return matchedQuestionId;
    }

    public void setMatchedQuestionId(Long matchedQuestionId)
    {
        this.matchedQuestionId = matchedQuestionId;
    }

    public String getMatchedQuestionCode()
    {
        return matchedQuestionCode;
    }

    public void setMatchedQuestionCode(String matchedQuestionCode)
    {
        this.matchedQuestionCode = matchedQuestionCode;
    }

    public Long getTextbookId()
    {
        return textbookId;
    }

    public void setTextbookId(Long textbookId)
    {
        this.textbookId = textbookId;
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

    public List<String> imageUrlList()
    {
        return new ArrayList<>();
    }
}
