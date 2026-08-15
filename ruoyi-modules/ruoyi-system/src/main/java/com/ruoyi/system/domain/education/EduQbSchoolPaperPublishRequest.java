package com.ruoyi.system.domain.education;

import java.util.List;

/** Publish composed basket as school exam paper in portal catalog. */
public class EduQbSchoolPaperPublishRequest
{
    private String paperTitle;
    private Long subjectId;
    private List<EduQbPaperItemRequest> items;

    public String getPaperTitle()
    {
        return paperTitle;
    }

    public void setPaperTitle(String paperTitle)
    {
        this.paperTitle = paperTitle;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public List<EduQbPaperItemRequest> getItems()
    {
        return items;
    }

    public void setItems(List<EduQbPaperItemRequest> items)
    {
        this.items = items;
    }
}
