package com.ruoyi.system.domain.education;

public class EduQbStudentPracticeCheckBody
{
    private Long questionId;
    private String questionType;
    private String pickedAnswer;
    private Boolean subjective;
    private Boolean selfCorrect;

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
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

    public Boolean getSubjective()
    {
        return subjective;
    }

    public void setSubjective(Boolean subjective)
    {
        this.subjective = subjective;
    }

    public Boolean getSelfCorrect()
    {
        return selfCorrect;
    }

    public void setSelfCorrect(Boolean selfCorrect)
    {
        this.selfCorrect = selfCorrect;
    }
}
