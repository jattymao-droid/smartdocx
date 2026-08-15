package com.ruoyi.system.domain.education;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduQbPracticeSession extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private String userName;
    private Long subjectId;
    private String paperTitle;
    private String shareId;
    private Integer totalCount;
    private Integer correctCount;
    private Integer choiceCount;
    private Integer subjectiveCount;
    private Integer durationSec;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

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

    public Integer getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount)
    {
        this.totalCount = totalCount;
    }

    public Integer getCorrectCount()
    {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount)
    {
        this.correctCount = correctCount;
    }

    public Integer getChoiceCount()
    {
        return choiceCount;
    }

    public void setChoiceCount(Integer choiceCount)
    {
        this.choiceCount = choiceCount;
    }

    public Integer getSubjectiveCount()
    {
        return subjectiveCount;
    }

    public void setSubjectiveCount(Integer subjectiveCount)
    {
        this.subjectiveCount = subjectiveCount;
    }

    public Integer getDurationSec()
    {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec)
    {
        this.durationSec = durationSec;
    }

    @Override
    public Date getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
