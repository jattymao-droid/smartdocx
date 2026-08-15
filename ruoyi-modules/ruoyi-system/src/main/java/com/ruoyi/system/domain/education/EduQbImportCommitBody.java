package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import java.util.List;

public class EduQbImportCommitBody
{
    private Long taskId;
    private Long subjectId;
    private Long chapterId;
    private String chapterText;
    private String knowledgePoints;
    private BigDecimal difficulty;
    private String questionType;
    private List<EduQbImportCommitItem> items;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
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

    public List<EduQbImportCommitItem> getItems()
    {
        return items;
    }

    public void setItems(List<EduQbImportCommitItem> items)
    {
        this.items = items;
    }
}
