package com.ruoyi.system.domain.education;

import java.util.ArrayList;
import java.util.List;

public class EduQbChapterTreeNode
{
    private String id;
    private String label;
    private Integer count;
    /** chapter | knowledge | all */
    private String nodeType;
    private Long chapterId;
    private String tagName;
    private List<EduQbChapterTreeNode> children = new ArrayList<>();

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public Integer getCount()
    {
        return count;
    }

    public void setCount(Integer count)
    {
        this.count = count;
    }

    public String getNodeType()
    {
        return nodeType;
    }

    public void setNodeType(String nodeType)
    {
        this.nodeType = nodeType;
    }

    public Long getChapterId()
    {
        return chapterId;
    }

    public void setChapterId(Long chapterId)
    {
        this.chapterId = chapterId;
    }

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }

    public List<EduQbChapterTreeNode> getChildren()
    {
        return children;
    }

    public void setChildren(List<EduQbChapterTreeNode> children)
    {
        this.children = children;
    }
}
