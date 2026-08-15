package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduQbWeakPointStat
{
    private Long chapterId;
    private String chapterText;
    private Integer wrongCount;
    private Integer practiceCount;
    private BigDecimal wrongRate;

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

    public Integer getWrongCount()
    {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount)
    {
        this.wrongCount = wrongCount;
    }

    public Integer getPracticeCount()
    {
        return practiceCount;
    }

    public void setPracticeCount(Integer practiceCount)
    {
        this.practiceCount = practiceCount;
    }

    public BigDecimal getWrongRate()
    {
        return wrongRate;
    }

    public void setWrongRate(BigDecimal wrongRate)
    {
        this.wrongRate = wrongRate;
    }
}
