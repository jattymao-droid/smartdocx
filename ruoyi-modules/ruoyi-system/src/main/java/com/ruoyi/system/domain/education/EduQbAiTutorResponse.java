package com.ruoyi.system.domain.education;

import java.util.ArrayList;
import java.util.List;

public class EduQbAiTutorResponse
{
    private String reply;
    private String mode;
    private Long questionId;
    private Boolean aiPowered;
    private String choiceQuestion;
    private List<EduQbAiTutorChoice> choices = new ArrayList<>();

    public String getReply()
    {
        return reply;
    }

    public void setReply(String reply)
    {
        this.reply = reply;
    }

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }

    public Boolean getAiPowered()
    {
        return aiPowered;
    }

    public void setAiPowered(Boolean aiPowered)
    {
        this.aiPowered = aiPowered;
    }

    public String getChoiceQuestion()
    {
        return choiceQuestion;
    }

    public void setChoiceQuestion(String choiceQuestion)
    {
        this.choiceQuestion = choiceQuestion;
    }

    public List<EduQbAiTutorChoice> getChoices()
    {
        return choices;
    }

    public void setChoices(List<EduQbAiTutorChoice> choices)
    {
        this.choices = choices != null ? choices : new ArrayList<>();
    }
}
