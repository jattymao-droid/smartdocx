package com.ruoyi.system.domain.education;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduQbWrongBook extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long wrongId;
    private String userName;
    private Long questionId;
    private Long subjectId;
    private String subjectName;
    private Long chapterId;
    private String chapterText;
    private String questionType;
    private String content;
    private Integer wrongCount;
    private String mastered;
    private String lastWrongAnswer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastWrongTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getWrongId()
    {
        return wrongId;
    }

    public void setWrongId(Long wrongId)
    {
        this.wrongId = wrongId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getSubjectName()
    {
        return subjectName;
    }

    public void setSubjectName(String subjectName)
    {
        this.subjectName = subjectName;
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

    public String getQuestionType()
    {
        return questionType;
    }

    public void setQuestionType(String questionType)
    {
        this.questionType = questionType;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Integer getWrongCount()
    {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount)
    {
        this.wrongCount = wrongCount;
    }

    public String getMastered()
    {
        return mastered;
    }

    public void setMastered(String mastered)
    {
        this.mastered = mastered;
    }

    public String getLastWrongAnswer()
    {
        return lastWrongAnswer;
    }

    public void setLastWrongAnswer(String lastWrongAnswer)
    {
        this.lastWrongAnswer = lastWrongAnswer;
    }

    public Date getLastWrongTime()
    {
        return lastWrongTime;
    }

    public void setLastWrongTime(Date lastWrongTime)
    {
        this.lastWrongTime = lastWrongTime;
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
