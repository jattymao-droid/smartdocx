package com.ruoyi.system.domain.education;

public class EduQbChapterMatchResult
{
    private String hint;
    private Long chapterId;
    private String chapterName;
    private String chapterText;
    private Double score;
    private Boolean matched;

    public String getHint()
    {
        return hint;
    }

    public void setHint(String hint)
    {
        this.hint = hint;
    }

    public Long getChapterId()
    {
        return chapterId;
    }

    public void setChapterId(Long chapterId)
    {
        this.chapterId = chapterId;
    }

    public String getChapterName()
    {
        return chapterName;
    }

    public void setChapterName(String chapterName)
    {
        this.chapterName = chapterName;
    }

    public String getChapterText()
    {
        return chapterText;
    }

    public void setChapterText(String chapterText)
    {
        this.chapterText = chapterText;
    }

    public Double getScore()
    {
        return score;
    }

    public void setScore(Double score)
    {
        this.score = score;
    }

    public Boolean getMatched()
    {
        return matched;
    }

    public void setMatched(Boolean matched)
    {
        this.matched = matched;
    }
}
