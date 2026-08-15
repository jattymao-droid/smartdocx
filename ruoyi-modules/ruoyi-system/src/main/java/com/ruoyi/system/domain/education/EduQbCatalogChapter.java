package com.ruoyi.system.domain.education;

public class EduQbCatalogChapter
{
    private Long chapterId;
    private Long textbookId;
    private Long parentId;
    private String chapterName;
    private Integer orderNum;
    private Integer questionCount;

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

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getChapterName()
    {
        return chapterName;
    }

    public void setChapterName(String chapterName)
    {
        this.chapterName = chapterName;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public Integer getQuestionCount()
    {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount)
    {
        this.questionCount = questionCount;
    }
}
