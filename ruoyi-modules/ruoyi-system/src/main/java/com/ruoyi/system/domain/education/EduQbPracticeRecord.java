package com.ruoyi.system.domain.education;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduQbPracticeRecord extends BaseEntity
{
    public static final String CORRECT = "1";
    public static final String WRONG = "0";
    public static final String SUBJECTIVE = "2";

    private static final long serialVersionUID = 1L;

    private Long recordId;
    private Long sessionId;
    private String userName;
    private Long questionId;
    private Long subjectId;
    private Long chapterId;
    private String chapterText;
    private String questionType;
    private String pickedAnswer;
    private String correctFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

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

    public String getPickedAnswer()
    {
        return pickedAnswer;
    }

    public void setPickedAnswer(String pickedAnswer)
    {
        this.pickedAnswer = pickedAnswer;
    }

    public String getCorrectFlag()
    {
        return correctFlag;
    }

    public void setCorrectFlag(String correctFlag)
    {
        this.correctFlag = correctFlag;
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
