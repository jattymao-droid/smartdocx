package com.ruoyi.system.domain.education;

import java.util.List;

public class EduQbPracticeSubmitBody
{
    private Long subjectId;
    private String paperTitle;
    private String shareId;
    private Integer durationSec;
    private List<EduQbPracticeSubmitItem> items;

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getPaperTitle()
    {
        return paperTitle;
    }

    public void setPaperTitle(String paperTitle)
    {
        this.paperTitle = paperTitle;
    }

    public String getShareId()
    {
        return shareId;
    }

    public void setShareId(String shareId)
    {
        this.shareId = shareId;
    }

    public Integer getDurationSec()
    {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec)
    {
        this.durationSec = durationSec;
    }

    public List<EduQbPracticeSubmitItem> getItems()
    {
        return items;
    }

    public void setItems(List<EduQbPracticeSubmitItem> items)
    {
        this.items = items;
    }
}
