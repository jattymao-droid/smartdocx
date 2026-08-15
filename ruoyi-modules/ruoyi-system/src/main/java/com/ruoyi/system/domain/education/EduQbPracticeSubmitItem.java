package com.ruoyi.system.domain.education;

public class EduQbPracticeSubmitItem
{
    private Long questionId;
    private String questionType;
    private String pickedAnswer;
    /** true | false | null (subjective) */
    private Boolean correct;
    private Boolean subjective;

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

    public Boolean getCorrect()
    {
        return correct;
    }

    public void setCorrect(Boolean correct)
    {
        this.correct = correct;
    }

    public Boolean getSubjective()
    {
        return subjective;
    }

    public void setSubjective(Boolean subjective)
    {
        this.subjective = subjective;
    }
}
